package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Base64;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CambioEstadoRequest;
import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.response.DevolucionResponse;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.EstadoDevolucion;
import co.tiendabarrio.model.MotivoDevolucion;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.DevolucionService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/** Verificación de F-07: registrar un producto en mal estado al recibirlo y validar la nota de devolución. */
@SpringBootTest
@Transactional
class DevolucionesTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    IngresoService ingresos;
    @Autowired
    InventarioService inventario;
    @Autowired
    DevolucionService devoluciones;

    @Test
    void productoEnMalEstadoGeneraLaNotaDeDevolucionYSaleDelInventario() {
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 2, null, null));
        IngresoResponse ingreso = recibir(leche, 20);
        assertThat(stockDe(leche)).isEqualTo(22);

        DevolucionResponse devolucion = devoluciones.registrar(pedido(ingreso, leche, 3, null, null));

        assertThat(devolucion.numero()).startsWith("ND-");
        assertThat(devolucion.estado()).isEqualTo(EstadoDevolucion.REGISTRADA);
        assertThat(stockDe(leche)).isEqualTo(19);
        assertThat(inventario.historial(leche.id()).get(0).origen()).isEqualTo(OrigenMovimiento.DEVOLUCION_PROVEEDOR);

        String nota = devoluciones.nota(devolucion.id());
        assertThat(nota).contains("NOTA DE DEVOLUCIÓN " + devolucion.numero());
        assertThat(nota).contains("Distribuidora");
        assertThat(nota).contains("Producto: Leche");
        assertThat(nota).contains("Cantidad: 3");
        assertThat(nota).contains("mal estado");
        assertThat(nota).contains("Empaques rotos");
    }

    @Test
    void laEvidenciaFotograficaSeGuardaYSePuedeRecuperar() {
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(leche, 10);
        String foto = Base64.getEncoder().encodeToString(new byte[] {1, 2, 3});

        DevolucionResponse devolucion = devoluciones.registrar(pedido(ingreso, leche, 1, foto, "image/jpeg"));

        assertThat(devolucion.tieneFoto()).isTrue();
        assertThat(devoluciones.foto(devolucion.id()).datos()).hasSize(3);
        assertThat(devoluciones.foto(devolucion.id()).tipo()).isEqualTo("image/jpeg");
    }

    @Test
    void noSePuedeDevolverMasDeLoRecibidoEnElIngreso() {
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(leche, 10);
        devoluciones.registrar(pedido(ingreso, leche, 8, null, null));

        assertThatThrownBy(() -> devoluciones.registrar(pedido(ingreso, leche, 3, null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Solo se pueden devolver 2");
    }

    @Test
    void elSeguimientoLlegaHastaElReemplazoYLasUnidadesVuelvenAlInventario() {
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(leche, 10);
        DevolucionResponse devolucion = devoluciones.registrar(pedido(ingreso, leche, 4, null, null));
        assertThat(stockDe(leche)).isEqualTo(6);

        devoluciones.cambiarEstado(devolucion.id(), new CambioEstadoRequest(EstadoDevolucion.RECLAMADA, "Llamé al proveedor"));
        DevolucionResponse resuelta = devoluciones.cambiarEstado(devolucion.id(),
                new CambioEstadoRequest(EstadoDevolucion.REEMPLAZADA, "Trajeron 4 nuevas"));

        assertThat(resuelta.estado()).isEqualTo(EstadoDevolucion.REEMPLAZADA);
        assertThat(resuelta.fechaResolucion()).isNotNull();
        assertThat(resuelta.seguimiento()).hasSize(3);
        assertThat(stockDe(leche)).isEqualTo(10);
        assertThat(inventario.historial(leche.id()).get(0).origen()).isEqualTo(OrigenMovimiento.REEMPLAZO_PROVEEDOR);

        assertThatThrownBy(() -> devoluciones.cambiarEstado(devolucion.id(),
                new CambioEstadoRequest(EstadoDevolucion.ACREDITADA, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("No se puede pasar");
    }

    @Test
    void conCreditoDelProveedorElInventarioNoVuelveASumar() {
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(leche, 10);
        DevolucionResponse devolucion = devoluciones.registrar(pedido(ingreso, leche, 4, null, null));

        DevolucionResponse resuelta = devoluciones.cambiarEstado(devolucion.id(),
                new CambioEstadoRequest(EstadoDevolucion.ACREDITADA, "Nota crédito NC-5"));

        assertThat(resuelta.estado()).isEqualTo(EstadoDevolucion.ACREDITADA);
        assertThat(stockDe(leche)).isEqualTo(6);
    }

    private DevolucionRequest pedido(IngresoResponse ingreso, ProductoResponse producto, int cantidad,
                                     String foto, String tipoFoto) {
        return new DevolucionRequest(ingreso.id(), producto.id(), cantidad, MotivoDevolucion.MAL_ESTADO,
                "Empaques rotos", foto, tipoFoto);
    }

    private IngresoResponse recibir(ProductoResponse producto, int cantidad) {
        ProveedorResponse proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", "Luis", "3001234567",
                null, List.of(producto.id())));
        return ingresos.registrar(new IngresoRequest(proveedor.id(), "FV-100",
                List.of(new LineaProductoRequest(producto.id(), cantidad))));
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
