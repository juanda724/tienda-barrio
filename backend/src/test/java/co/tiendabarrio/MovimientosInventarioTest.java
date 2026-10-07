package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.MovimientoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.TipoMovimiento;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.VentaService;

/**
 * Verificación de F-01 / PBI-01: las ventas descargan el inventario y cada movimiento queda
 * registrado con fecha, hora y cantidad. Las entradas se prueban en ProveedoresIngresoTest.
 */
@SpringBootTest
@Transactional
class MovimientosInventarioTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    InventarioService inventario;
    @Autowired
    VentaService ventas;

    @Test
    void ventaDescargaProductosDelInventario() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10, 1000L, 800L));
        ProductoResponse aceite = productos.crear(new ProductoRequest("Aceite", "Aceites", 5, 8, 1000L, 800L));

        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(arroz.id(), 3),
                new LineaProductoRequest(aceite.id(), 2),
                new LineaProductoRequest(arroz.id(), 1)), FormaPago.EFECTIVO, null, null));

        assertThat(stockDe(arroz)).isEqualTo(6);
        assertThat(stockDe(aceite)).isEqualTo(6);
        MovimientoResponse salida = inventario.historial(arroz.id()).get(0);
        assertThat(salida.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(salida.origen()).isEqualTo(OrigenMovimiento.VENTA);
        assertThat(salida.cantidad()).isEqualTo(4);
        assertThat(salida.stockResultante()).isEqualTo(6);
        assertThat(salida.fechaHora()).isNotNull();
        assertThat(salida.numeroDocumento()).as("número de la venta").isEqualTo(venta.id());
    }

    @Test
    void ventaSinStockSuficienteSeRechaza() {
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 2, 3, 1000L, 800L));

        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(leche.id(), 5)), FormaPago.EFECTIVO, null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(stockDe(leche)).isEqualTo(3);
    }

    @Test
    void stockInicialQuedaRegistradoComoMovimiento() {
        ProductoResponse pan = productos.crear(new ProductoRequest("Pan", "Panadería", 2, 9, 1000L, 800L));

        List<MovimientoResponse> historial = inventario.historial(pan.id());
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(historial.get(0).origen()).isEqualTo(OrigenMovimiento.INVENTARIO_INICIAL);
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
