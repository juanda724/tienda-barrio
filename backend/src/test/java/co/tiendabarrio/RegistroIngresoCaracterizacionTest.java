package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CambioEstadoRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoProveedorRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.MovimientoResponse;
import co.tiendabarrio.dto.response.PedidoProveedorResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.model.EstadoPedido;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.PedidoProveedorService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/**
 * Pruebas de caracterización de {@link IngresoService#registrar} (F-02/F-06, SWR-03, SWR-05, SWR-06, SWR-15,
 * SWR-17), escritas antes de refactorizar el hallazgo H-03 (docs/refactorizacion). Fijan todos los efectos
 * observables del registro de un ingreso: líneas y verificación, stock y costo de cada producto, movimientos de
 * inventario con su referencia, cierre del pedido y el mensaje exacto de cada error de validación.
 *
 * <p>Las referencias se guardan en src/test/resources/caracterizacion y se generan una sola vez, con el código
 * anterior a la refactorización, con
 * {@code mvnw test -Dtest=RegistroIngresoCaracterizacionTest -Dcaracterizacion.generar=true}. Los números de
 * ingreso, pedido y producto se reemplazan por {INGRESO}, {PEDIDO} y el nombre del producto.
 */
@SpringBootTest
@Transactional
class RegistroIngresoCaracterizacionTest {

    private static final Path REFERENCIAS = Path.of("src", "test", "resources", "caracterizacion");

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    PedidoProveedorService pedidos;
    @Autowired
    IngresoService ingresos;
    @Autowired
    InventarioService inventario;

    ProductoResponse aceite;
    ProductoResponse arroz;
    ProductoResponse leche;
    ProveedorResponse nogal;
    ProveedorResponse pradera;

    @BeforeEach
    void datos() {
        aceite = productos.crear(new ProductoRequest("Aceite 1 L", "Aceites", 10, 2, 9500L, 7800L));
        arroz = productos.crear(new ProductoRequest("Arroz 500 g", "Granos", 10, 3, 2800L, 2200L));
        leche = productos.crear(new ProductoRequest("Leche 1 L", "Lácteos", 10, 4, 4200L, 3500L));
        nogal = proveedores.crear(new ProveedorRequest("Distribuidora El Nogal", "Luis", "3001234567", null,
                List.of(aceite.id(), arroz.id())));
        pradera = proveedores.crear(new ProveedorRequest("Lácteos La Pradera", null, "3107654321", null,
                List.of(leche.id())));
    }

    @Test
    void ingresoSinPedidoConDiferenciaActualizaStockCostoYMovimientos() throws IOException {
        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(nogal.id(), null, "  FV-201  ", List.of(
                linea(aceite, 10, 12, 8100L),
                linea(arroz, 5, 5, 2300L))));

        comparar("10-registro-sin-pedido-con-diferencia", ingreso, null);
    }

    @Test
    void lineasRepetidasSeUnenYLasVaciasSeIgnoran() throws IOException {
        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(nogal.id(), null, null, List.of(
                linea(aceite, 4, 4, 7900L),
                linea(arroz, 0, 0, 9999L),
                linea(aceite, 2, 3, 8000L),
                linea(arroz, 0, 3, 2400L))));

        comparar("11-registro-lineas-repetidas-y-vacias", ingreso, null);
    }

    @Test
    void lineaSinCantidadesNoSeValidaNiSeRegistra() throws IOException {
        // La leche no es de este proveedor: si la línea vacía se procesara, el ingreso se rechazaría
        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(nogal.id(), null, "FV-214", List.of(
                linea(aceite, 3, 3, 7800L),
                linea(leche, 0, 0, 3500L))));

        comparar("14-registro-linea-vacia-de-producto-ajeno", ingreso, null);
    }

    @Test
    void ingresoDeUnPedidoLoMarcaEntregado() throws IOException {
        Long pedido = pedidos.crear(new PedidoProveedorRequest(nogal.id(), List.of(
                new LineaProductoRequest(aceite.id(), 6), new LineaProductoRequest(arroz.id(), 8)), null, null)).id();
        pedidos.cambiarEstado(pedido, new CambioEstadoRequest(EstadoPedido.ACEPTADO, null, null));
        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(nogal.id(), pedido, "   ", List.of(
                linea(aceite, 6, 6, 7800L),
                linea(arroz, 8, 8, 2200L))));

        comparar("12-registro-con-pedido", ingreso, pedido);
    }

    @Test
    void erroresDeValidacionConservanSuMensaje() throws IOException {
        Long pedidoNogal = pedidos.crear(new PedidoProveedorRequest(nogal.id(),
                List.of(new LineaProductoRequest(arroz.id(), 5)), null, null)).id();
        Long pedidoCancelado = pedidos.crear(new PedidoProveedorRequest(nogal.id(),
                List.of(new LineaProductoRequest(aceite.id(), 5)), null, null)).id();
        pedidos.cambiarEstado(pedidoCancelado, new CambioEstadoRequest(EstadoPedido.CANCELADO, null, null));

        List<String> casos = new ArrayList<>();
        casos.add(error("proveedor inexistente", () -> ingresos.registrar(
                new IngresoRequest(999_999L, null, null, List.of(linea(aceite, 1, 1, 7800L))))));
        casos.add(error("todas las cantidades en cero", () -> ingresos.registrar(
                new IngresoRequest(nogal.id(), null, null, List.of(linea(aceite, 0, 0, 7800L), linea(arroz, 0, 0, 2200L))))));
        casos.add(error("producto inexistente", () -> ingresos.registrar(
                new IngresoRequest(nogal.id(), null, null, List.of(new LineaIngresoRequest(999_999L, 1, 1, 100L))))));
        casos.add(error("producto que el proveedor no suministra", () -> ingresos.registrar(
                new IngresoRequest(nogal.id(), null, null, List.of(linea(leche, 2, 2, 3500L))))));
        casos.add(error("pedido inexistente", () -> ingresos.registrar(
                new IngresoRequest(nogal.id(), 999_999L, null, List.of(linea(arroz, 5, 5, 2200L))))));
        casos.add(error("pedido de otro proveedor", () -> ingresos.registrar(
                new IngresoRequest(pradera.id(), pedidoNogal, null, List.of(linea(leche, 5, 5, 3500L))))));
        casos.add(error("pedido cancelado", () -> ingresos.registrar(
                new IngresoRequest(nogal.id(), pedidoCancelado, null, List.of(linea(aceite, 5, 5, 7800L))))));

        String actual = String.join("\n", casos)
                .replace("#" + pedidoNogal + " ", "#{PEDIDO} ")
                .replace("#" + pedidoCancelado + " ", "#{PEDIDO_CANCELADO} ") + "\n";
        aprobar("13-registro-errores-de-validacion", actual);
    }

    /** Arma la foto de todos los efectos del registro y la compara con su referencia. */
    private void comparar(String escenario, IngresoResponse ingreso, Long pedidoId) throws IOException {
        StringBuilder t = new StringBuilder()
                .append("== Ingreso\n")
                .append("proveedor=").append(ingreso.proveedorNombre())
                .append(" factura=").append(ingreso.numeroFactura())
                .append(" pedido=").append(ingreso.pedidoId() == null ? "null" : "{PEDIDO}").append('\n')
                .append("verificacion=").append(ingreso.resultadoVerificacion())
                .append(" (").append(ingreso.resultadoVerificacionNombre()).append(")")
                .append(" totalFacturado=").append(ingreso.totalFacturado())
                .append(" totalRecibido=").append(ingreso.totalRecibido())
                .append(" totalAPagar=").append(ingreso.totalAPagar())
                .append(" estadoPago=").append(ingreso.estadoPago())
                .append(" pagable=").append(ingreso.pagable()).append('\n');
        for (IngresoResponse.Linea l : ingreso.lineas()) {
            t.append("- ").append(l.productoNombre()).append(": facturada=").append(l.cantidadFacturada())
                    .append(" recibida=").append(l.cantidadRecibida()).append(" diferencia=").append(l.diferencia())
                    .append(" costo=").append(l.costoUnitario()).append(" subtotalFacturado=")
                    .append(l.subtotalFacturado()).append('\n');
        }
        t.append("\n== Productos (stock y costo del catálogo después del ingreso)\n");
        for (ProductoResponse p : List.of(aceite, arroz, leche)) {
            var producto = productoRepository.findById(p.id()).orElseThrow();
            t.append("- ").append(producto.getNombre()).append(": stock=").append(producto.getStockActual())
                    .append(" costo=").append(producto.getCosto()).append('\n');
        }
        t.append("\n== Movimientos de inventario (del más reciente al más antiguo)\n");
        for (ProductoResponse p : List.of(aceite, arroz, leche)) {
            for (MovimientoResponse m : inventario.historial(p.id())) {
                t.append("- ").append(m.productoNombre()).append(": ").append(m.tipo()).append(' ').append(m.origen())
                        .append(" cantidad=").append(m.cantidad()).append(" stock=").append(m.stockResultante())
                        .append(" documento=").append(ingreso.id().equals(m.numeroDocumento()) ? "{INGRESO}" : m.numeroDocumento())
                        .append(" referencia=").append(m.referencia()).append('\n');
            }
        }
        if (pedidoId != null) {
            PedidoProveedorResponse pedido = pedidos.obtener(pedidoId);
            t.append("\n== Pedido\n")
                    .append("estado=").append(pedido.estado().estado())
                    .append(" puedeRecibirse=").append(pedido.puedeRecibirse())
                    .append(" ultimaNota=").append(pedido.historial().get(pedido.historial().size() - 1).nota()
                            .replace("#" + ingreso.id(), "#{INGRESO}"))
                    .append('\n');
        }
        aprobar(escenario, t.toString());
    }

    private static String error(String caso, Supplier<?> accion) {
        try {
            accion.get();
            return caso + " -> SIN ERROR";
        } catch (RuntimeException e) {
            return caso + " -> " + e.getClass().getSimpleName() + ": " + e.getMessage();
        }
    }

    private static void aprobar(String escenario, String actual) throws IOException {
        Path referencia = REFERENCIAS.resolve(escenario + ".txt");
        if (Boolean.getBoolean("caracterizacion.generar")) {
            Files.createDirectories(REFERENCIAS);
            Files.writeString(referencia, actual, StandardCharsets.UTF_8);
        }
        assertThat(referencia).as("falta la referencia; genérela con el código anterior a la refactorización")
                .exists();
        // git puede guardar la referencia con saltos de línea de Windows; el contenido es lo que importa
        assertThat(actual).as("efectos del escenario " + escenario)
                .isEqualTo(Files.readString(referencia, StandardCharsets.UTF_8).replace("\r\n", "\n"));
    }

    private static LineaIngresoRequest linea(ProductoResponse p, int recibida, int facturada, long costo) {
        return new LineaIngresoRequest(p.id(), recibida, facturada, costo);
    }
}
