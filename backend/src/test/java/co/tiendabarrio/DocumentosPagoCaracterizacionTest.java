package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CambioEstadoDevolucionRequest;
import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.request.EntregaFaltantesRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaDevolucionRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.PedidoProveedorRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.request.ResolverDiferenciasRequest;
import co.tiendabarrio.dto.response.ComprobantePagoResponse;
import co.tiendabarrio.dto.response.FacturaPedidoResponse;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.model.EstadoDevolucion;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.MotivoDevolucion;
import co.tiendabarrio.service.ComprobantePagoService;
import co.tiendabarrio.service.DevolucionService;
import co.tiendabarrio.service.FacturaPedidoService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.PedidoProveedorService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/**
 * Pruebas de caracterización de los documentos del pago al proveedor (F-06, SWR-29), escritas antes de la
 * refactorización de H-01 y H-02 (docs/refactorizacion). Cada escenario guarda en
 * src/test/resources/caracterizacion el texto exacto del comprobante de pago y de la factura del pedido, sus
 * enlaces y los valores que calcula el sistema. Después de refactorizar, todo debe seguir siendo idéntico.
 *
 * <p>Los archivos de referencia se generan una sola vez, con el código anterior a la refactorización, con
 * {@code mvnw test -Dtest=DocumentosPagoCaracterizacionTest -Dcaracterizacion.generar=true}. Sin esa opción,
 * un archivo que falte hace fallar la prueba en vez de crearse en silencio.
 *
 * <p>Para que el texto sea comparable entre ejecuciones se reemplazan las fechas y horas por {FECHA} y los
 * números de ingreso, pedido y devolución por {INGRESO}, {PEDIDO} y {DEVOLUCION}.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "tienda.evidencias.carpeta=target/evidencias-prueba")
class DocumentosPagoCaracterizacionTest {

    private static final Path REFERENCIAS = Path.of("src", "test", "resources", "caracterizacion");
    /** "07/10/2026 03:39 p. m." hasta el salto de línea, los dos puntos o el paréntesis que la siguen. */
    private static final String FECHA = "\\d{2}/\\d{2}/\\d{4} \\d{2}:\\d{2}[^\\n(]*?(?=\\n|: | \\(|$)";

    @Autowired
    ProductoService productos;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    PedidoProveedorService pedidos;
    @Autowired
    IngresoService ingresos;
    @Autowired
    DevolucionService devoluciones;
    @Autowired
    ComprobantePagoService comprobantes;
    @Autowired
    FacturaPedidoService facturas;

    ProductoResponse aceite;
    ProductoResponse arroz;
    /** Con contacto, teléfono y correo. */
    ProveedorResponse completo;
    /** Solo con teléfono: la factura omite contacto y correo. */
    ProveedorResponse minimo;

    @BeforeEach
    void datos() {
        // Stock por debajo del mínimo, para poder pedirlos (RN-02)
        aceite = productos.crear(new ProductoRequest("Aceite 1 L", "Aceites", 10, 2, 9500L, 7800L));
        arroz = productos.crear(new ProductoRequest("Arroz 500 g", "Granos", 10, 3, 2800L, 2200L));
        completo = proveedores.crear(new ProveedorRequest("Distribuidora El Nogal", "Luis Pérez", "300 123 4567",
                "pedidos@nogal.example", List.of(aceite.id(), arroz.id())));
        minimo = proveedores.crear(new ProveedorRequest("Granos del Valle", null, "3107654321", null,
                List.of(aceite.id(), arroz.id())));
    }

    @Test
    void pedidoAprobadoPagadoPorTransferencia() throws IOException {
        Long pedido = pedir(completo, arroz, 10);
        IngresoResponse ingreso = recibir(completo, pedido, "FV-101", linea(arroz, 10, 10, 2200L));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.TRANSFERENCIA, null, null));

        comparar("01-pedido-aprobado-transferencia", ingreso.id(), pedido);
    }

    @Test
    void pedidoPagadoEnEfectivoConCambio() throws IOException {
        Long pedido = pedidos.crear(new PedidoProveedorRequest(completo.id(), List.of(
                new LineaProductoRequest(aceite.id(), 6), new LineaProductoRequest(arroz.id(), 12)), null, null)).id();
        IngresoResponse ingreso = recibir(completo, pedido, "FV-102",
                linea(aceite, 6, 6, 7800L), linea(arroz, 12, 12, 2200L));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, 100000L));

        comparar("02-pedido-efectivo-con-cambio", ingreso.id(), pedido);
    }

    @Test
    void pedidoConFaltantesEntregadosEnDosPartes() throws IOException {
        Long pedido = pedidos.crear(new PedidoProveedorRequest(completo.id(), List.of(
                new LineaProductoRequest(aceite.id(), 12), new LineaProductoRequest(arroz.id(), 4)), null, null)).id();
        IngresoResponse ingreso = recibir(completo, pedido, "FV-103",
                linea(aceite, 7, 12, 7800L), linea(arroz, 0, 4, 2200L));
        ingresos.registrarEntregaFaltantes(ingreso.id(), new EntregaFaltantesRequest(
                List.of(new EntregaFaltantesRequest.Linea(aceite.id(), 3)), "Primera entrega"));
        ingresos.registrarEntregaFaltantes(ingreso.id(), new EntregaFaltantesRequest(List.of(
                new EntregaFaltantesRequest.Linea(aceite.id(), 2), new EntregaFaltantesRequest.Linea(arroz.id(), 4)), null));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, null));

        comparar("03-pedido-entregas-posteriores", ingreso.id(), pedido);
    }

    @Test
    void pedidoConFacturaAjustadaALoRecibido() throws IOException {
        Long pedido = pedir(minimo, aceite, 12);
        IngresoResponse ingreso = recibir(minimo, pedido, null, linea(aceite, 7, 12, 7800L));
        ingresos.registrarEntregaFaltantes(ingreso.id(), new EntregaFaltantesRequest(
                List.of(new EntregaFaltantesRequest.Linea(aceite.id(), 3)), "Llegaron el martes"));
        ingresos.resolverDiferencias(ingreso.id(), new ResolverDiferenciasRequest("Las otras 2 no las tenían"));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.TRANSFERENCIA, null, null));

        comparar("04-pedido-factura-ajustada", ingreso.id(), pedido);
    }

    @Test
    void pedidoConNotaCreditoPorDevolucion() throws IOException {
        Long pedido = pedir(completo, aceite, 10);
        IngresoResponse ingreso = recibir(completo, pedido, "FV-105", linea(aceite, 10, 10, 7800L));
        var devolucion = devoluciones.registrar(new DevolucionRequest(ingreso.id(), "Dos botellas rotas",
                List.of(new LineaDevolucionRequest(aceite.id(), 2, MotivoDevolucion.MAL_ESTADO))));
        devoluciones.cambiarEstado(devolucion.id(), new CambioEstadoDevolucionRequest(EstadoDevolucion.ENVIADA, null));
        devoluciones.cambiarEstado(devolucion.id(),
                new CambioEstadoDevolucionRequest(EstadoDevolucion.CON_NOTA_CREDITO, "NC-77"));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, 70000L));

        comparar("05-pedido-nota-credito", ingreso.id(), pedido);
    }

    @Test
    void ingresoSinPedidoSoloTieneComprobante() throws IOException {
        IngresoResponse ingreso = recibir(completo, null, "SIN-1", linea(aceite, 3, 3, 7800L));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.TARJETA, null, null));

        comparar("06-ingreso-sin-pedido", ingreso.id(), null);
    }

    /** Arma el documento del escenario y lo compara con su referencia, o la genera si se pidió. */
    private void comparar(String escenario, Long ingresoId, Long pedidoId) throws IOException {
        IngresoResponse ingreso = ingresos.obtener(ingresoId);
        ComprobantePagoResponse comprobante = comprobantes.generar(ingresoId);
        StringBuilder salida = new StringBuilder()
                .append("== Ingreso\n")
                .append("totalFacturado=").append(ingreso.totalFacturado())
                .append(" totalRecibido=").append(ingreso.totalRecibido())
                .append(" totalAPagar=").append(ingreso.totalAPagar())
                .append(" creditoDevoluciones=").append(ingreso.totalCreditoDevoluciones())
                .append(" montoPagado=").append(ingreso.montoPagado())
                .append(" entregado=").append(ingreso.montoEntregado())
                .append(" cambio=").append(ingreso.cambio()).append('\n')
                .append("\n== Comprobante de pago\n").append(comprobante.texto()).append('\n')
                .append("whatsapp=").append(decodificar(comprobante.whatsappUrl())).append('\n')
                .append("correo=").append(decodificar(comprobante.correoUrl())).append('\n');
        if (pedidoId != null) {
            FacturaPedidoResponse factura = facturas.generar(pedidoId);
            salida.append("\n== Factura del pedido\n")
                    .append(factura.lineas().stream()
                            .map(l -> l.productoNombre() + ": pedida=" + l.cantidadPedida() + " facturada="
                                    + l.cantidadFacturada() + " recibida=" + l.cantidadRecibida() + " cobrada="
                                    + l.cantidadCobrada() + " costo=" + l.costoUnitario() + " subtotal=" + l.subtotal())
                            .collect(Collectors.joining("\n"))).append('\n')
                    .append("subtotal=").append(factura.subtotal())
                    .append(" creditoDevoluciones=").append(factura.totalCreditoDevoluciones())
                    .append(" totalPagado=").append(factura.totalPagado()).append('\n')
                    .append(factura.texto()).append('\n')
                    .append("whatsapp=").append(decodificar(factura.whatsappUrl())).append('\n')
                    .append("correo=").append(decodificar(factura.correoUrl())).append('\n');

            // La factura y el pago deben dar lo mismo: subtotal de la factura menos notas crédito = total pagado
            assertThat(Math.max(factura.subtotal() - factura.totalCreditoDevoluciones(), 0))
                    .as("la factura del pedido cuadra con lo pagado").isEqualTo(factura.totalPagado());
        }

        String actual = normalizar(salida.toString(), ingresoId, pedidoId);
        Path referencia = REFERENCIAS.resolve(escenario + ".txt");
        if (Boolean.getBoolean("caracterizacion.generar")) {
            Files.createDirectories(REFERENCIAS);
            Files.writeString(referencia, actual, StandardCharsets.UTF_8);
        }
        assertThat(referencia).as("falta la referencia; genérela con el código anterior a la refactorización")
                .exists();
        // git puede guardar la referencia con saltos de línea de Windows; el contenido es lo que importa
        assertThat(actual).as("documentos del escenario " + escenario)
                .isEqualTo(Files.readString(referencia, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    private static String normalizar(String texto, Long ingresoId, Long pedidoId) {
        String t = texto.replaceAll(FECHA, "{FECHA}")
                .replace("Ingreso #" + ingresoId, "Ingreso #{INGRESO}")
                .replace("ingreso #" + ingresoId, "ingreso #{INGRESO}");
        if (pedidoId != null) {
            t = t.replace("pedido #" + pedidoId, "pedido #{PEDIDO}");
        }
        return t;
    }

    private static String decodificar(String url) {
        return url == null ? "null" : URLDecoder.decode(url, StandardCharsets.UTF_8);
    }

    private Long pedir(ProveedorResponse proveedor, ProductoResponse producto, int cantidad) {
        return pedidos.crear(new PedidoProveedorRequest(proveedor.id(),
                List.of(new LineaProductoRequest(producto.id(), cantidad)), null, null)).id();
    }

    private IngresoResponse recibir(ProveedorResponse proveedor, Long pedidoId, String factura,
                                    LineaIngresoRequest... lineas) {
        return ingresos.registrar(new IngresoRequest(proveedor.id(), pedidoId, factura, List.of(lineas)));
    }

    private static LineaIngresoRequest linea(ProductoResponse p, int recibida, int facturada, long costo) {
        return new LineaIngresoRequest(p.id(), recibida, facturada, costo);
    }
}
