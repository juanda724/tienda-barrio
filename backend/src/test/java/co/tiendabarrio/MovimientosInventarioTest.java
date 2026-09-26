package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.comun.NegocioException;
import co.tiendabarrio.inventario.CompraRequest;
import co.tiendabarrio.inventario.InventarioService;
import co.tiendabarrio.inventario.LineaProductoRequest;
import co.tiendabarrio.inventario.MovimientoResponse;
import co.tiendabarrio.inventario.OrigenMovimiento;
import co.tiendabarrio.inventario.TipoMovimiento;
import co.tiendabarrio.pedido.PedidoRequest;
import co.tiendabarrio.pedido.PedidoService;
import co.tiendabarrio.producto.Producto;
import co.tiendabarrio.producto.ProductoRepository;
import co.tiendabarrio.producto.ProductoRequest;
import co.tiendabarrio.producto.ProductoService;

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
        Producto arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10));

        inventario.registrarCompra(new CompraRequest(arroz.getId(), 7, "Factura 123"));

        assertThat(stockDe(arroz)).isEqualTo(17);
        MovimientoResponse ultimo = inventario.historial(arroz.getId()).get(0);
        assertThat(ultimo.tipo()).isEqualTo(TipoMovimiento.ENTRADA);
        assertThat(ultimo.origen()).isEqualTo(OrigenMovimiento.COMPRA);
        assertThat(ultimo.cantidad()).isEqualTo(7);
        assertThat(ultimo.stockResultante()).isEqualTo(17);
        assertThat(ultimo.fechaHora()).isNotNull();
    }

    @Test
    void pedidoDescargaProductosDelInventario() {
        Producto arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10));
        Producto aceite = productos.crear(new ProductoRequest("Aceite", "Aceites", 5, 8));

        pedidos.registrar(new PedidoRequest(List.of(
                new LineaProductoRequest(arroz.getId(), 3),
                new LineaProductoRequest(aceite.getId(), 2),
                new LineaProductoRequest(arroz.getId(), 1))));

        assertThat(stockDe(arroz)).isEqualTo(6);
        assertThat(stockDe(aceite)).isEqualTo(6);
        MovimientoResponse salida = inventario.historial(arroz.getId()).get(0);
        assertThat(salida.tipo()).isEqualTo(TipoMovimiento.SALIDA);
        assertThat(salida.cantidad()).isEqualTo(4);
        assertThat(salida.referencia()).startsWith("Pedido #");
    }

    @Test
    void pedidoSinStockSuficienteSeRechaza() {
        Producto leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 2, 3));

        assertThatThrownBy(() -> pedidos.registrar(new PedidoRequest(List.of(
                new LineaProductoRequest(leche.getId(), 5)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(stockDe(leche)).isEqualTo(3);
    }

    @Test
    void stockInicialQuedaRegistradoComoMovimiento() {
        Producto pan = productos.crear(new ProductoRequest("Pan", "Panadería", 2, 9));

        List<MovimientoResponse> historial = inventario.historial(pan.getId());
        assertThat(historial).hasSize(1);
        assertThat(historial.get(0).origen()).isEqualTo(OrigenMovimiento.INVENTARIO_INICIAL);
    }

    private int stockDe(Producto producto) {
        return productoRepository.findById(producto.getId()).orElseThrow().getStockActual();
    }
}
