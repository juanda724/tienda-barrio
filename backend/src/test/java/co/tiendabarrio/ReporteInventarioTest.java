package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ReporteInventarioResponse;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.PedidoService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ReporteService;

/** Verificación de F-04: el reporte de cantidades por producto coincide con el estado real del inventario. */
@SpringBootTest
@Transactional
class ReporteInventarioTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    PedidoService pedidos;
    @Autowired
    ReporteService reportes;

    @Test
    void elReporteCoincideConElStockRealDeCadaProducto() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10,
                new BigDecimal("3000"), new BigDecimal("2000")));
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 3,
                new BigDecimal("4000"), new BigDecimal("3500")));
        pedidos.registrar(new PedidoRequest(List.of(new LineaProductoRequest(arroz.id(), 4))));

        ReporteInventarioResponse reporte = reportes.inventario(null, false);

        assertThat(reporte.totalProductos()).isEqualTo(2);
        assertThat(reporte.totalUnidades()).isEqualTo(9);
        assertThat(reporte.productosBajoMinimo()).isEqualTo(1);
        // 6 arroces a $2.000 + 3 leches a $3.500
        assertThat(reporte.valorEnCosto()).isEqualByComparingTo("22500");

        ReporteInventarioResponse.Fila filaArroz = reporte.filas().stream()
                .filter(f -> f.nombre().equals("Arroz")).findFirst().orElseThrow();
        assertThat(filaArroz.stockActual()).isEqualTo(productoRepository.findById(arroz.id()).orElseThrow().getStockActual());
        assertThat(filaArroz.totalEntradas()).isEqualTo(10);
        assertThat(filaArroz.totalSalidas()).isEqualTo(4);
        assertThat(filaArroz.bajoMinimo()).isFalse();

        ReporteInventarioResponse.Fila filaLeche = reporte.filas().stream()
                .filter(f -> f.nombre().equals("Leche")).findFirst().orElseThrow();
        assertThat(filaLeche.stockActual()).isEqualTo(3);
        assertThat(filaLeche.bajoMinimo()).isTrue();
        assertThat(leche.id()).isNotNull();
    }

    @Test
    void sePuedeFiltrarPorCategoriaYPorStockMinimo() {
        productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10, null, null));
        productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 3, null, null));

        assertThat(reportes.inventario("granos", false).filas()).hasSize(1);
        assertThat(reportes.inventario(null, true).filas()).hasSize(1);
        assertThat(reportes.inventario(null, true).filas().get(0).nombre()).isEqualTo("Leche");
    }

    @Test
    void elReporteSeExportaEnCsv() {
        productos.crear(new ProductoRequest("Leche \"entera\"", "Lácteos", 5, 3, null, null));

        String csv = reportes.inventarioCsv(null, false);

        assertThat(csv).contains("Producto;Categoría;Stock actual");
        assertThat(csv).contains("\"Leche \"\"entera\"\"\";\"Lácteos\";3;5;Reabastecer");
    }
}
