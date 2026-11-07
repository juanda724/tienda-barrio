package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.stream.Stream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import co.tiendabarrio.dto.response.ComprobanteResponse;
import co.tiendabarrio.model.Adquiriente;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.Venta;
import co.tiendabarrio.util.Dinero;
import co.tiendabarrio.util.Enlaces;

/** Arma el comprobante digital de una venta para compartirlo con el cliente (SWR-14). */
@Service
public class ComprobanteService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    static final String AVISO_PROTOTIPO = "";

    private final String nombreTienda;

    public ComprobanteService(@Value("${tienda.nombre:Tienda de Barrio}") String nombreTienda) {
        this.nombreTienda = nombreTienda;
    }

    /**
     * telefono es opcional: sin él, WhatsApp deja elegir a quién enviarlo. saldoCliente es la deuda
     * actual del cliente en una venta fiada (null en las demás).
     */
    public ComprobanteResponse de(Venta venta, String telefono, Long saldoCliente) {
        String texto = texto(venta, saldoCliente);
        Adquiriente adquiriente = venta.getAdquiriente();
        String correo = adquiriente == null ? null : Enlaces.correo(adquiriente.getCorreo(),
                "Factura electrónica " + venta.getNumeroFacturaElectronica() + " - " + nombreTienda, texto);
        return new ComprobanteResponse(venta.getId(), texto, Enlaces.whatsapp(telefono, texto), correo);
    }

    String texto(Venta venta, Long saldoCliente) {
        Adquiriente adquiriente = venta.getAdquiriente();
        StringBuilder t = new StringBuilder().append(nombreTienda).append('\n');
        if (adquiriente == null) {
            t.append("Comprobante de venta #").append(venta.getId()).append('\n');
        } else {
            t.append("Factura electrónica de venta ").append(venta.getNumeroFacturaElectronica())
                    .append(" (venta #").append(venta.getId()).append(")\n");
        }
        t.append(venta.getFechaHora().format(FECHA_HORA)).append('\n');
        if (adquiriente != null) {
            t.append("\nCliente: ").append(adquiriente.getNombre()).append('\n')
                    .append(adquiriente.getTipoDocumento().getNombre()).append(": ")
                    .append(adquiriente.getNumeroDocumento()).append('\n')
                    .append("Correo: ").append(adquiriente.getCorreo()).append('\n');
            if (adquiriente.getTelefono() != null) {
                t.append("Teléfono: ").append(adquiriente.getTelefono()).append('\n');
            }
            if (adquiriente.getDireccion() != null || adquiriente.getCiudad() != null) {
                t.append("Dirección: ").append(String.join(", ", Stream.of(adquiriente.getDireccion(),
                        adquiriente.getCiudad()).filter(Objects::nonNull).toList())).append('\n');
            }
        }
        if (venta.getCliente() != null) {
            t.append(adquiriente == null ? "Cliente: " : "Fiado a: ").append(venta.getCliente().getNombre()).append('\n');
        }
        t.append('\n');
        for (LineaProducto linea : venta.getLineas()) {
            t.append(linea.getCantidad()).append(" x ").append(linea.getProducto().getNombre());
            if (linea.getPrecioUnitario() != null) {
                t.append(" (").append(Dinero.formatear(linea.getPrecioUnitario())).append(" c/u): ")
                        .append(Dinero.formatear(linea.getSubtotal()));
            }
            t.append('\n');
        }
        if (venta.getTotal() != null) {
            t.append("\nTOTAL: ").append(Dinero.formatear(venta.getTotal())).append('\n');
        }
        if (venta.getFormaPago() != null) {
            t.append("Forma de pago: ").append(venta.getFormaPago().getNombre()).append('\n');
        }
        if (venta.getMontoRecibido() != null) {
            t.append("Recibido: ").append(Dinero.formatear(venta.getMontoRecibido())).append('\n')
                    .append("Cambio: ").append(Dinero.formatear(venta.getCambio())).append('\n');
        }
        if (saldoCliente != null) {
            t.append("Saldo pendiente de su cuenta: ").append(Dinero.formatear(saldoCliente)).append('\n');
        }
        if (adquiriente != null) {
            t.append("\n").append(AVISO_PROTOTIPO).append('\n');
        }
        return t.append("\n¡Gracias por su compra!").toString();
    }
}
