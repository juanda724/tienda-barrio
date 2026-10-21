package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.TipoPago;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.VentaService;

/** Verificación de F-05 / PBI-05: registrar una venta completa, con total, descuento de inventario y comprobante. */
@SpringBootTest
@Transactional
class VentasFacturacionTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    InventarioService inventario;
    @Autowired
    VentaService ventas;

    @Test
    void ventaCalculaElTotalDescuentaElInventarioYGeneraElComprobante() {
        ProductoResponse arroz = conPrecio("Arroz", 10, "3000", "2000");
        ProductoResponse aceite = conPrecio("Aceite", 5, "10000", "8000");

        VentaResponse venta = ventas.registrar(new VentaRequest(TipoPago.CONTADO, null, null, List.of(
                new LineaProductoRequest(arroz.id(), 2),
                new LineaProductoRequest(aceite.id(), 1))));

        assertThat(venta.total()).isEqualByComparingTo("16000");
        assertThat(venta.ganancia()).isEqualByComparingTo("4000");
        assertThat(venta.fechaHora()).isNotNull();
        assertThat(stockDe(arroz)).isEqualTo(8);
        assertThat(stockDe(aceite)).isEqualTo(4);
        assertThat(inventario.historial(arroz.id()).get(0).origen()).isEqualTo(OrigenMovimiento.VENTA);

        String comprobante = ventas.comprobante(venta.id());
        assertThat(comprobante).contains(venta.numero());
        assertThat(comprobante).contains("2 x Arroz a $3.000 = $6.000");
        assertThat(comprobante).contains("TOTAL: $16.000");
    }

    @Test
    void ventaSinPrecioDefinidoSeRechazaYNoCambiaElStock() {
        ProductoResponse pan = productos.crear(new ProductoRequest("Pan", "Panadería", 2, 10, null, null));

        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(TipoPago.CONTADO, null, null,
                List.of(new LineaProductoRequest(pan.id(), 1)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene precio de venta");
        assertThat(stockDe(pan)).isEqualTo(10);
    }

    @Test
    void ventaSinStockSuficienteSeRechaza() {
        ProductoResponse leche = conPrecio("Leche", 3, "4000", "3500");

        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(TipoPago.CONTADO, null, null,
                List.of(new LineaProductoRequest(leche.id(), 5)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Stock insuficiente");
        assertThat(stockDe(leche)).isEqualTo(3);
    }

    @Test
    void elDuenoDefinePrecioYCostoYAlEditarSeConservanSiNoVienen() {
        ProductoResponse arroz = conPrecio("Arroz", 10, "3000", "2000");
        assertThat(arroz.precioVenta()).isEqualByComparingTo("3000");
        assertThat(arroz.costo()).isEqualByComparingTo("2000");

        ProductoResponse editado = productos.actualizar(arroz.id(),
                new ProductoRequest("Arroz", "Granos", 5, null, new BigDecimal("3300"), null));

        assertThat(editado.precioVenta()).isEqualByComparingTo("3300");
        assertThat(editado.costo()).isEqualByComparingTo("2000");
    }

    private ProductoResponse conPrecio(String nombre, int stock, String precio, String costo) {
        return productos.crear(new ProductoRequest(nombre, "Granos", 1, stock, new BigDecimal(precio),
                new BigDecimal(costo)));
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }

    @Test
    void cambiarElPrecioDelProductoNoAlteraLasVentasAnteriores() {
        ProductoResponse arroz = conPrecio("Arroz", 10, "3000", "2000");
        VentaResponse venta = ventas.registrar(new VentaRequest(TipoPago.CONTADO, null, null,
                List.of(new LineaProductoRequest(arroz.id(), 2))));

        productos.actualizar(arroz.id(),
                new ProductoRequest("Arroz", "Granos", 1, null, new BigDecimal("5000"), null));

        VentaResponse consultada = ventas.obtener(venta.id());
        assertThat(consultada.lineas().get(0).precioUnitario()).isEqualByComparingTo("3000");
        assertThat(consultada.total()).isEqualByComparingTo("6000");
    }
}
