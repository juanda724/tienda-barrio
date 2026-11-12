package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.config.DatosEmisor;
import co.tiendabarrio.config.ResolucionFacturacion;
import co.tiendabarrio.dto.request.ClienteRequest;
import co.tiendabarrio.dto.request.FacturaElectronicaRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ClienteResponse;
import co.tiendabarrio.dto.response.FacturaElectronicaResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.TipoDocumento;
import co.tiendabarrio.repository.AdquirienteRepository;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.VentaRepository;
import co.tiendabarrio.service.AdquirienteService;
import co.tiendabarrio.service.ClienteService;
import co.tiendabarrio.service.ComprobanteService;
import co.tiendabarrio.service.FacturaElectronicaService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.VentaService;
import jakarta.persistence.EntityManager;

/**
 * Verificación de SWR-32: registrar una venta con factura electrónica y validar que la factura tenga su
 * número de la resolución, los datos del emisor y del cliente, el pago, el CUFE y el QR.
 */
@SpringBootTest
@Transactional
class FacturaElectronicaTest {

    private static final FacturaElectronicaRequest ANA = new FacturaElectronicaRequest(TipoDocumento.CC,
            "1.023.456.789", "Ana Gómez", "ana@example.com", "3001112233", "Calle 10 # 5-20", "Bogotá");

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    VentaService ventas;
    @Autowired
    VentaRepository ventaRepository;
    @Autowired
    FacturaElectronicaService facturas;
    @Autowired
    AdquirienteService adquirientes;
    @Autowired
    AdquirienteRepository adquirienteRepository;
    @Autowired
    ClienteService clientes;
    @Autowired
    InventarioService inventario;
    @Autowired
    ComprobanteService comprobantes;
    @Autowired
    DatosEmisor emisor;
    @Autowired
    EntityManager entityManager;

    @Test
    void laFacturaTieneLosDatosDelEmisorDelClienteYDelPago() {
        ProductoResponse arroz = arroz();

        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(arroz.id(), 2)),
                FormaPago.TARJETA, null, null, ANA));

        assertThat(venta.numeroFacturaElectronica()).matches("FE-\\d+");
        assertThat(venta.facturaElectronica().numeroDocumento()).as("sin puntos").isEqualTo("1023456789");

        FacturaElectronicaResponse factura = facturas.generar(venta.id());
        assertThat(factura.numero()).isEqualTo(venta.numeroFacturaElectronica());
        assertThat(factura.emisor().nit()).isEqualTo("123456789-6");
        assertThat(factura.emisor().regimen()).isEqualTo("No responsable de IVA");
        assertThat(factura.resolucion()).startsWith("Resolución DIAN 18760000001").contains("FE-1 a FE-5000");
        assertThat(factura.adquiriente().tipoDocumento()).isEqualTo("Cédula de ciudadanía");
        assertThat(factura.adquiriente().direccion()).isEqualTo("Calle 10 # 5-20, Bogotá");
        assertThat(factura.formaPago()).isEqualTo("Contado");
        assertThat(factura.medioPago()).isEqualTo("Tarjeta / datáfono");
        assertThat(factura.subtotal()).isEqualTo(5600);
        assertThat(factura.descuentos()).isZero();
        assertThat(factura.total()).isEqualTo(5600);

        FacturaElectronicaResponse.Linea linea = factura.lineas().get(0);
        assertThat(linea.codigo()).isEqualTo(String.format("P-%04d", arroz.id()));
        assertThat(linea.unidad()).isEqualTo("Unidad");
        assertThat(linea.cantidad()).isEqualTo(2);

        assertThat(factura.aviso()).contains("sin validez fiscal");
        assertThat(factura.texto())
                .contains("NIT 123456789-6 · No responsable de IVA")
                .contains("Factura electrónica de venta " + factura.numero())
                .contains("Cliente: Ana Gómez")
                .contains("Cédula de ciudadanía: 1023456789")
                .contains("Correo: ana@example.com")
                .contains(linea.codigo() + " · 2 x Arroz ($ 2.800 c/u): $ 5.600")
                .contains("TOTAL: $ 5.600")
                .contains("Medio de pago: Tarjeta / datáfono")
                .contains("CUFE: " + factura.cufe())
                .contains(factura.aviso());
        assertThat(factura.correoUrl())
                .startsWith("https://mail.google.com/mail/?view=cm&fs=1&to=ana%40example.com&su=Factura%20electr");
        assertThat(factura.whatsappUrl()).startsWith("https://wa.me/573001112233?text=");
    }

    @Test
    void laFacturaLlevaCufeYCodigoQr() {
        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(arroz().id(), 1)),
                FormaPago.EFECTIVO, null, null, ANA));

        FacturaElectronicaResponse factura = facturas.generar(venta.id());

        assertThat(factura.cufe()).as("SHA-384 en hexadecimal").matches("[0-9a-f]{96}");
        assertThat(factura.qr()).startsWith("data:image/svg+xml;base64,");
        assertThat(facturas.generar(venta.id()).cufe()).as("no cambia al volver a consultarla").isEqualTo(factura.cufe());
    }

    @Test
    void laNumeracionEsConsecutivaYNoCuentaLasVentasSinFactura() {
        ProductoResponse arroz = arroz();
        List<LineaProductoRequest> unArroz = List.of(new LineaProductoRequest(arroz.id(), 1));

        VentaResponse primera = ventas.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null, ANA));
        VentaResponse sinFactura = ventas.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null));
        VentaResponse segunda = ventas.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null, ANA));

        assertThat(sinFactura.numeroFacturaElectronica()).isNull();
        assertThat(consecutivoDe(segunda)).isEqualTo(consecutivoDe(primera) + 1);
    }

    @Test
    void consumidorFinalSeFacturaConElNitGenericoSinGuardarSusDatos() {
        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(arroz().id(), 1)),
                FormaPago.EFECTIVO, 5000L, null, FacturaElectronicaRequest.consumidorFinal()));

        assertThat(venta.numeroFacturaElectronica()).isNotNull();
        assertThat(venta.facturaElectronica()).isNull();
        assertThat(adquirienteRepository.count()).isZero();

        FacturaElectronicaResponse factura = facturas.generar(venta.id());
        assertThat(factura.adquiriente().nombre()).isEqualTo("Consumidor final");
        assertThat(factura.adquiriente().numeroDocumento()).isEqualTo("222222222222");
        assertThat(factura.correoUrl()).as("no hay correo al cual enviarla").isNull();
        assertThat(factura.whatsappUrl()).startsWith("https://wa.me/?text=");
        assertThat(factura.texto())
                .contains("Cliente: Consumidor final")
                .contains("NIT: 222222222222")
                .contains("Cambio: $ 2.200");
    }

    @Test
    void clienteIdentificadoDebeTraerDocumentoNombreYCorreo() {
        ProductoResponse arroz = arroz();
        FacturaElectronicaRequest sinNombre = new FacturaElectronicaRequest(TipoDocumento.CC, "1023456789", " ",
                "ana@example.com", null, null, null);

        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(arroz.id(), 1)),
                FormaPago.EFECTIVO, null, null, sinNombre)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("nombre o razón social");
        assertThat(stockDe(arroz)).isEqualTo(20);
    }

    @Test
    void elClienteSeReutilizaPorSuDocumento() {
        List<LineaProductoRequest> unArroz = List.of(new LineaProductoRequest(arroz().id(), 1));
        ventas.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null, new FacturaElectronicaRequest(
                TipoDocumento.NIT, "900123456-7", "Tienda Ejemplo SAS", "compras@example.com", null, null, null)));

        assertThat(adquirientes.buscar(TipoDocumento.NIT, "900123456-7")).hasValueSatisfying(a ->
                assertThat(a.nombre()).isEqualTo("Tienda Ejemplo SAS"));
        assertThat(adquirientes.buscar(TipoDocumento.CC, "900123456-7")).as("otro tipo de documento").isEmpty();

        VentaResponse segunda = ventas.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null,
                new FacturaElectronicaRequest(TipoDocumento.NIT, "900123456-7", "Tienda Ejemplo SAS",
                        "facturas@example.com", null, null, null)));

        assertThat(adquirienteRepository.count()).isEqualTo(1);
        assertThat(segunda.facturaElectronica().correo()).as("se actualiza el correo").isEqualTo("facturas@example.com");
    }

    @Test
    void ventaFiadaSeFacturaACredito() {
        ClienteResponse cliente = clientes.crear(new ClienteRequest("Carlos Ruiz", "3105556677", null));

        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(arroz().id(), 2)),
                FormaPago.CREDITO, null, cliente.id(), FacturaElectronicaRequest.consumidorFinal()));

        FacturaElectronicaResponse factura = facturas.generar(venta.id());
        assertThat(factura.formaPago()).isEqualTo("Crédito");
        assertThat(factura.medioPago()).isNull();
        assertThat(factura.clienteFiado()).isEqualTo("Carlos Ruiz");
        assertThat(factura.saldoCliente()).isEqualTo(5600);
        assertThat(factura.whatsappUrl()).as("al celular del cliente fiado").startsWith("https://wa.me/573105556677?text=");
    }

    @Test
    void ventaSinFacturaElectronicaNoLaGenera() {
        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(arroz().id(), 1)), FormaPago.EFECTIVO, null, null));

        assertThat(venta.numeroFacturaElectronica()).isNull();
        assertThat(venta.facturaElectronica()).isNull();
        assertThatThrownBy(() -> facturas.generar(venta.id()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene factura electrónica");
    }

    @Test
    void facturaAnteriorALaNumeracionPorResolucionConservaSuNumero() {
        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(arroz().id(), 1)),
                FormaPago.EFECTIVO, null, null, ANA));
        // Así quedaron guardadas las facturas expedidas antes de numerar por resolución: solo con su cliente
        entityManager.createQuery("update Venta v set v.numeroFactura = null, v.consecutivoFactura = null, "
                + "v.cufe = null where v.id = :id").setParameter("id", venta.id()).executeUpdate();
        entityManager.clear();

        FacturaElectronicaResponse factura = facturas.generar(venta.id());

        assertThat(factura.numero()).isEqualTo(String.format("FE-%06d", venta.id()));
        assertThat(factura.adquiriente().nombre()).isEqualTo("Ana Gómez");
        assertThat(factura.resolucion()).isNull();
        assertThat(factura.cufe()).isNull();
        assertThat(factura.qr()).isNull();
        assertThat(factura.texto()).doesNotContain("CUFE").contains(factura.aviso());
    }

    @Test
    void numeracionAgotadaRechazaLaVentaSinTocarElStock() {
        ProductoResponse arroz = arroz();
        // Resolución que solo autoriza la factura FE-1: la segunda venta con factura ya no cabe
        VentaService conUnaSolaFactura = ventasCon(new ResolucionFacturacion("18760000001", "2026-01-01", "FE", 1, 1,
                "2099-12-31", "clave-de-prueba"));
        List<LineaProductoRequest> unArroz = List.of(new LineaProductoRequest(arroz.id(), 1));
        conUnaSolaFactura.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null, ANA));

        assertThatThrownBy(() -> conUnaSolaFactura.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null, ANA)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Se agotó la numeración").hasMessageContaining("FE-1 a FE-1");
        assertThat(stockDe(arroz)).as("solo salió la unidad de la primera venta").isEqualTo(19);
    }

    @Test
    void resolucionVencidaRechazaLaVentaConFactura() {
        ProductoResponse arroz = arroz();
        VentaService conResolucionVencida = ventasCon(new ResolucionFacturacion("18760000001", "2020-01-01", "FE", 1,
                5000, "2020-12-31", "clave-de-prueba"));
        List<LineaProductoRequest> unArroz = List.of(new LineaProductoRequest(arroz.id(), 1));

        assertThatThrownBy(() -> conResolucionVencida.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null, ANA)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("venció el 31/12/2020");
        assertThat(stockDe(arroz)).isEqualTo(20);
        assertThat(conResolucionVencida.registrar(new VentaRequest(unArroz, FormaPago.EFECTIVO, null, null))
                .numeroFacturaElectronica()).as("sin factura sí se puede vender").isNull();
    }

    /** Servicio de ventas igual al de la aplicación, pero facturando con otra resolución. */
    private VentaService ventasCon(ResolucionFacturacion resolucion) {
        return new VentaService(ventaRepository, productoRepository, inventario, comprobantes, clientes,
                new FacturaElectronicaService(ventaRepository, adquirientes, clientes, emisor, resolucion));
    }

    private ProductoResponse arroz() {
        return productos.crear(new ProductoRequest("Arroz", "Granos", 5, 20, 2800L, 2200L));
    }

    private static long consecutivoDe(VentaResponse venta) {
        return Long.parseLong(venta.numeroFacturaElectronica().replace("FE-", ""));
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
