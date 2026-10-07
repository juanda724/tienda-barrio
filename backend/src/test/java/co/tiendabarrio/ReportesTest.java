package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ReporteInventarioResponse;
import co.tiendabarrio.dto.response.ReporteVentasResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ReporteService;
import co.tiendabarrio.service.VentaService;

/**
 * Verificación de F-04: generar un reporte de cantidades por producto y validar que coincida con el
 * estado real del inventario.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ReportesTest {

    @Autowired
    ProductoService productos;
    @Autowired
    VentaService ventas;
    @Autowired
    ReporteService reportes;
    @Autowired
    MockMvc mvc;

    ProductoResponse arroz;
    ProductoResponse leche;

    @BeforeEach
    void datos() {
        arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 10, 20, 2800L, 2200L));
        leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 12, 8, 4200L, 3500L));
    }

    @Test
    void inventarioCoincideConElStockRealYCalculaValores() {
        vender(arroz, 5, FormaPago.EFECTIVO);

        ReporteInventarioResponse r = reportes.inventario(null, false);

        assertThat(r.filas()).extracting(ReporteInventarioResponse.Fila::nombre, ReporteInventarioResponse.Fila::stockActual)
                .containsExactly(tuple("Arroz", 15), tuple("Leche", 8));
        assertThat(r.totales().unidades()).isEqualTo(23);
        assertThat(r.totales().valorCosto()).isEqualTo(15 * 2200 + 8 * 3500);
        assertThat(r.totales().valorVenta()).isEqualTo(15 * 2800 + 8 * 4200);
        assertThat(r.totales().bajoMinimo()).isEqualTo(1);
        assertThat(r.categorias()).containsExactly("Granos", "Lácteos");
    }

    @Test
    void filtrosPorCategoriaYBajoMinimo() {
        assertThat(reportes.inventario("lácteos", false).filas()).extracting(ReporteInventarioResponse.Fila::nombre)
                .containsExactly("Leche");
        assertThat(reportes.inventario(null, true).filas()).extracting(ReporteInventarioResponse.Fila::nombre)
                .containsExactly("Leche");
    }

    @Test
    void ventasPorProductoOrdenadasConResumen() {
        vender(arroz, 2, FormaPago.EFECTIVO);
        vender(leche, 3, FormaPago.TRANSFERENCIA);
        vender(arroz, 1, FormaPago.EFECTIVO);

        ReporteVentasResponse r = reportes.ventas(null, null);

        assertThat(r.filas()).extracting(ReporteVentasResponse.Fila::nombre).containsExactly("Arroz", "Leche");
        assertThat(r.filas().get(0).unidades()).isEqualTo(3);
        assertThat(r.filas().get(0).total()).isEqualTo(3 * 2800);
        assertThat(r.filas().get(0).gananciaEstimada()).isEqualTo(3 * (2800 - 2200));
        assertThat(r.resumen().ventas()).isEqualTo(3);
        assertThat(r.resumen().total()).isEqualTo(3 * 2800 + 3 * 4200);
        assertThat(r.resumen().porFormaPago()).extracting(ReporteVentasResponse.PorFormaPago::formaPago)
                .containsExactly("EFECTIVO", "TRANSFERENCIA");
        assertThat(r.hasta()).isEqualTo(LocalDate.now());
        assertThat(r.desde()).isEqualTo(LocalDate.now().minusDays(29));
    }

    @Test
    void ventasFueraDelRangoNoSeCuentan() {
        vender(arroz, 2, FormaPago.EFECTIVO);

        ReporteVentasResponse r = reportes.ventas(LocalDate.now().minusDays(10), LocalDate.now().minusDays(1));

        assertThat(r.filas()).isEmpty();
        assertThat(r.resumen().ticketPromedio()).isZero();
    }

    @Test
    void rangoInvalidoSeRechaza() {
        assertThatThrownBy(() -> reportes.ventas(LocalDate.now(), LocalDate.now().minusDays(1)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("posterior");
    }

    @Test
    void inventarioSeDescargaComoCsvParaExcel() throws Exception {
        String csv = mvc.perform(get("/api/reportes/inventario.csv"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/csv"))
                .andExpect(header().string("Content-Disposition", containsString("attachment")))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(csv).startsWith(String.valueOf((char) 0xFEFF)).contains("Producto;Categoría;Stock")
                .contains("Arroz;Granos;20;10;Disponible;2200;2800;44000;56000")
                .contains("Leche;Lácteos;8;12;Reabastecer");
    }

    private void vender(ProductoResponse p, int cantidad, FormaPago forma) {
        ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(p.id(), cantidad)), forma, null, null));
    }
}
