package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.config.DatosEmisor;
import co.tiendabarrio.config.ResolucionFacturacion;
import co.tiendabarrio.dto.request.FacturaElectronicaRequest;
import co.tiendabarrio.dto.response.FacturaElectronicaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Adquiriente;
import co.tiendabarrio.model.Cliente;
import co.tiendabarrio.model.DesgloseIva;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.TipoDocumento;
import co.tiendabarrio.model.Venta;
import co.tiendabarrio.repository.VentaRepository;
import co.tiendabarrio.util.CodigoQr;
import co.tiendabarrio.util.Cufe;
import co.tiendabarrio.util.Dinero;
import co.tiendabarrio.util.Enlaces;

/**
 * Factura electrónica de venta (SWR-32): la expide con la numeración de la resolución y arma el documento con
 * los datos que exige la norma. Es una simulación: calcula el CUFE con la fórmula de la DIAN y genera el QR,
 * pero no firma la factura ni la envía a la DIAN para su validación.
 */
@Service
public class FacturaElectronicaService {

    /** Va en la factura y en su QR para que no se tome por un documento válido ante la DIAN. */
    static final String AVISO_PROTOTIPO = "Documento de prueba sin validez fiscal: no fue validado por la DIAN.";

    /** Todos los productos de la tienda se venden por unidades. */
    private static final String UNIDAD = "Unidad";

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private static final FacturaElectronicaResponse.Adquiriente CONSUMIDOR_FINAL =
            new FacturaElectronicaResponse.Adquiriente(TipoDocumento.CONSUMIDOR_FINAL.getNombre(),
                    TipoDocumento.NIT.getNombre(), TipoDocumento.NIT_CONSUMIDOR_FINAL, null, null, null);

    private final VentaRepository ventas;
    private final AdquirienteService adquirientes;
    private final ClienteService clientes;
    private final DatosEmisor emisor;
    private final ResolucionFacturacion resolucion;

    public FacturaElectronicaService(VentaRepository ventas, AdquirienteService adquirientes, ClienteService clientes,
                                     DatosEmisor emisor, ResolucionFacturacion resolucion) {
        this.ventas = ventas;
        this.adquirientes = adquirientes;
        this.clientes = clientes;
        this.emisor = emisor;
        this.resolucion = resolucion;
    }

    /**
     * Expide la factura de una venta que ya tiene sus productos y aún no se ha guardado: le da el siguiente
     * número de la resolución (sin saltos), guarda los datos del cliente y calcula el CUFE. Se rechaza si la
     * resolución venció o su numeración se agotó.
     */
    @Transactional
    public void expedir(Venta venta, FacturaElectronicaRequest datos) {
        long consecutivo = resolucion.siguiente(ventas.ultimoConsecutivoFactura());
        resolucion.validarQueAutoriza(consecutivo, venta.getFechaHora().toLocalDate());
        Adquiriente adquiriente = datos.esConsumidorFinal() ? null : adquirientes.guardar(datos);
        String numero = resolucion.numero(consecutivo);
        long iva = DesgloseIva.de(venta.getLineas()).totalIva();
        String cufe = Cufe.calcular(sinSeparador(numero), venta.getFechaHora(), venta.getTotal() - iva, iva,
                emisor.nitSinDigito(), documentoDe(adquiriente), resolucion.claveTecnica(), Cufe.AMBIENTE_PRUEBAS);
        venta.expedirFacturaElectronica(adquiriente, consecutivo, numero, cufe);
    }

    /** Factura electrónica de una venta, para mostrarla, imprimirla o enviarla al cliente. */
    @Transactional(readOnly = true)
    public FacturaElectronicaResponse generar(Long ventaId) {
        Venta venta = ventas.findById(ventaId).orElseThrow(() -> new NoEncontradoException("Venta no encontrada"));
        String numero = venta.getNumeroFacturaElectronica();
        if (numero == null) {
            throw new NegocioException("La venta #" + ventaId + " no tiene factura electrónica");
        }

        FacturaElectronicaResponse.Adquiriente adquiriente = adquiriente(venta.getAdquiriente());
        List<FacturaElectronicaResponse.Linea> lineas = venta.getLineas().stream()
                .map(l -> new FacturaElectronicaResponse.Linea(l.getProducto().getCodigo(), l.getProducto().getNombre(),
                        l.getCantidad(), UNIDAD, l.getPrecioUnitario(), l.getSubtotal(),
                        l.getTratamientoIva() == null ? null : l.getTratamientoIva().getEtiqueta()))
                .toList();
        // La factura discrimina IVA si la tienda era responsable al vender: sus líneas guardan el tratamiento
        DesgloseIva desglose = DesgloseIva.de(venta.getLineas());
        long iva = desglose.totalIva();
        long subtotal = venta.getTotal() - iva;
        List<FacturaElectronicaResponse.Impuesto> impuestos = desglose.grupos().stream()
                .map(g -> new FacturaElectronicaResponse.Impuesto(g.tratamiento().getNombre(), g.base(), g.iva()))
                .toList();
        boolean credito = venta.getFormaPago() == FormaPago.CREDITO;
        Cliente cliente = venta.getCliente();
        // Las facturas anteriores a la numeración por resolución no tienen CUFE ni se expidieron con ella
        boolean conResolucion = venta.getCufe() != null;

        FacturaElectronicaResponse factura = new FacturaElectronicaResponse(
                ventaId, numero, venta.getFechaHora(),
                new FacturaElectronicaResponse.Emisor(emisor.nombre(), emisor.razonSocial(), emisor.nit(),
                        desglose.vacio() ? "No responsable de IVA" : "Responsable de IVA",
                        emisor.direccion(), emisor.telefono()),
                conResolucion ? resolucion.descripcion() : null,
                adquiriente, lineas, subtotal, 0, iva, venta.getTotal(), impuestos,
                credito ? "Crédito" : "Contado",
                credito || venta.getFormaPago() == null ? null : venta.getFormaPago().getNombre(),
                venta.getMontoRecibido(), venta.getCambio(),
                cliente == null ? null : cliente.getNombre(),
                cliente == null ? null : clientes.saldo(cliente.getId()),
                venta.getCufe(),
                conResolucion ? CodigoQr.imagen(contenidoQr(venta, adquiriente.numeroDocumento(), iva)) : null,
                AVISO_PROTOTIPO, null, null, null);

        String texto = texto(factura);
        // WhatsApp va al celular que dio para la factura o, si es fiada, al del cliente; sin ninguno, deja elegir
        String telefono = adquiriente.telefono();
        if (telefono == null && cliente != null) {
            telefono = cliente.getTelefono();
        }
        return factura.conEnvio(texto, Enlaces.whatsapp(telefono, texto),
                Enlaces.correo(adquiriente.correo(), "Factura electrónica " + numero + " - " + emisor.nombre(), texto));
    }

    private static FacturaElectronicaResponse.Adquiriente adquiriente(Adquiriente a) {
        if (a == null) {
            return CONSUMIDOR_FINAL;
        }
        String direccion = Stream.of(a.getDireccion(), a.getCiudad()).filter(Objects::nonNull)
                .reduce((calle, ciudad) -> calle + ", " + ciudad).orElse(null);
        return new FacturaElectronicaResponse.Adquiriente(a.getNombre(), a.getTipoDocumento().getNombre(),
                a.getNumeroDocumento(), a.getCorreo(), a.getTelefono(), direccion);
    }

    private static String documentoDe(Adquiriente adquiriente) {
        return adquiriente == null ? TipoDocumento.NIT_CONSUMIDOR_FINAL : adquiriente.getNumeroDocumento();
    }

    /** La DIAN usa el número sin separadores: FE12. */
    private static String sinSeparador(String numeroFactura) {
        return numeroFactura.replace("-", "");
    }

    /** Datos que lleva el QR de una factura electrónica; en vez del enlace de consulta de la DIAN, el aviso. */
    private String contenidoQr(Venta venta, String documentoAdquiriente, long iva) {
        return String.join("\n",
                "NumFac: " + sinSeparador(venta.getNumeroFacturaElectronica()),
                "FecFac: " + Cufe.fecha(venta.getFechaHora()),
                "HorFac: " + Cufe.hora(venta.getFechaHora()),
                "NitFac: " + emisor.nitSinDigito(),
                "DocAdq: " + documentoAdquiriente,
                "ValFac: " + Cufe.valor(venta.getTotal() - iva),
                "ValIva: " + Cufe.valor(iva),
                "ValOtroIm: " + Cufe.valor(0),
                "ValTolFac: " + Cufe.valor(venta.getTotal()),
                "CUFE: " + venta.getCufe(),
                AVISO_PROTOTIPO);
    }

    private static String texto(FacturaElectronicaResponse f) {
        StringBuilder t = new StringBuilder();
        FacturaElectronicaResponse.Emisor e = f.emisor();
        t.append(e.nombre()).append('\n');
        if (!e.razonSocial().equals(e.nombre())) {
            t.append(e.razonSocial()).append('\n');
        }
        t.append("NIT ").append(e.nit()).append(" · ").append(e.regimen()).append('\n');
        if (e.direccion() != null) {
            t.append(e.direccion()).append('\n');
        }
        if (e.telefono() != null) {
            t.append("Tel. ").append(e.telefono()).append('\n');
        }

        t.append("\nFactura electrónica de venta ").append(f.numero()).append('\n');
        if (f.resolucion() != null) {
            t.append(f.resolucion()).append('\n');
        }
        t.append("Fecha de generación y expedición: ").append(f.fechaHora().format(FECHA_HORA)).append('\n');

        FacturaElectronicaResponse.Adquiriente a = f.adquiriente();
        t.append("\nCliente: ").append(a.nombre()).append('\n')
                .append(a.tipoDocumento()).append(": ").append(a.numeroDocumento()).append('\n');
        if (a.correo() != null) {
            t.append("Correo: ").append(a.correo()).append('\n');
        }
        if (a.telefono() != null) {
            t.append("Teléfono: ").append(a.telefono()).append('\n');
        }
        if (a.direccion() != null) {
            t.append("Dirección: ").append(a.direccion()).append('\n');
        }

        t.append('\n');
        for (FacturaElectronicaResponse.Linea l : f.lineas()) {
            t.append(l.codigo()).append(" · ").append(l.cantidad()).append(" x ").append(l.descripcion());
            if (l.precioUnitario() != null) {
                t.append(" (").append(Dinero.formatear(l.precioUnitario())).append(" c/u): ")
                        .append(Dinero.formatear(l.subtotal()));
            }
            if (l.tarifaIva() != null) {
                t.append(" · ").append(l.tarifaIva());
            }
            t.append('\n');
        }
        t.append("\nSubtotal: ").append(Dinero.formatear(f.subtotal())).append('\n')
                .append("Descuentos: ").append(Dinero.formatear(f.descuentos())).append('\n');
        if (!f.impuestos().isEmpty()) {
            t.append("IVA: ").append(Dinero.formatear(f.iva())).append('\n');
        }
        t.append("TOTAL: ").append(Dinero.formatear(f.total())).append('\n');
        for (FacturaElectronicaResponse.Impuesto i : f.impuestos()) {
            t.append("  IVA ").append(i.tarifa()).append(": base ").append(Dinero.formatear(i.base()))
                    .append(", impuesto ").append(Dinero.formatear(i.iva())).append('\n');
        }
        t.append("Forma de pago: ").append(f.formaPago()).append('\n');
        if (f.medioPago() != null) {
            t.append("Medio de pago: ").append(f.medioPago()).append('\n');
        }
        if (f.montoRecibido() != null) {
            t.append("Recibido: ").append(Dinero.formatear(f.montoRecibido())).append('\n')
                    .append("Cambio: ").append(Dinero.formatear(f.cambio())).append('\n');
        }
        if (f.clienteFiado() != null) {
            t.append("Fiado a: ").append(f.clienteFiado()).append('\n')
                    .append("Saldo pendiente de su cuenta: ").append(Dinero.formatear(f.saldoCliente())).append('\n');
        }

        t.append('\n');
        if (f.cufe() != null) {
            t.append("CUFE: ").append(f.cufe()).append('\n');
        }
        return t.append(f.aviso()).append("\n\n¡Gracias por su compra!").toString();
    }
}
