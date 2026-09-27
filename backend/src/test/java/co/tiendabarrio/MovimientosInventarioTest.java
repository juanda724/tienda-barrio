package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CompraRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.response.MovimientoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.TipoMovimiento;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.PedidoService;
import co.tiendabarrio.service.ProductoService;

/** Verificación de F-01 / PBI-01: registrar una compra y una venta y validar el stock resultante. */
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
    PedidoService pedidos;

    @Test
    void compraIncrementaStockYRegistraMovimiento() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10));

        inventario.registrarCompra(new CompraRequest(arroz.id(), 7, "Factura 123"));

        assertThat(stockDe(arroz)).isEqualTo(17);
        MovimientoResponse ultimo = inventario.historial(arroz.id()).get(0);
        assertThat(ultimo.tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(ultimo.origen()).isEqualTo(OrigenMovimiento.COMPRA);
        assertThat(ultimo.cantidad()).isEqualTo(7);
        assertThat(ultimo.stockResultante()).isEqualTo(17);
        assertThat(ultimo.fechaHora()).isNotNull();
    }

    @Test
    void pedidoDescargaProductosDelInventario() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10));
        ProductoResponse aceite = productos.crear(new ProductoRequest("Aceite", "Aceites", 5, 8));

        pedidos.registrar(new PedidoRequest(List.of(
                new LineaProductoRequest(arroz.id(), 3),
                new LineaProductoRequest(aceite.id(), 2),
                new LineaProductoRequest(arroz.id(), 1))));

        assertThat(stockDe(arroz)).isEqualTo(6);
        assertThat(stockDe(aceite)).isEqualTo(6);
        MovimientoResponse salida = inventario.historial(arroz.id()).get(0);
        assertThat(salida.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(salida.cantidad()).isEqualTo(4);
        assertThat(salida.referencia()).startsWith("Pedido #");
    }

    @Test
    void pedidoSinStockSuficienteSeRechaza() {
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 2, 3));

        assertThatThrownBy(() -> pedidos.registrar(new PedidoRequest(List.of(
                new LineaProductoRequest(leche.id(), 5)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(stockDe(leche)).isEqualTo(3);
    }

    @Test
    void stockInicialQuedaRegistradoComoMovimiento() {
        ProductoResponse pan = productos.crear(new ProductoRequest("Pan", "Panadería", 2, 9));

        List<MovimientoResponse> historial = inventario.historial(pan.id());
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).origen()).isEqualTo(OrigenMovimiento.INVENTARIO_INICIAL);
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
