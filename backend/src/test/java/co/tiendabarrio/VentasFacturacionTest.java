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
import co.tiendabarrio.dto.response.ComprobanteResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.VentaService;

/**
 * Verificación de F-05: registrar una venta completa y validar que el sistema calcule el total,
 * descuente el inventario y genere el comprobante digital.
 */
@SpringBootTest
@Transactional
class VentasFacturacionTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    VentaService ventas;

    @Test
    void ventaCalculaElTotalConLosPreciosDeVenta() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 20, 2800L, 2200L));
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 20, 4200L, 3500L));

        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(arroz.id(), 2),
                new LineaProductoRequest(leche.id(), 1)), FormaPago.TRANSFERENCIA, null, null));

        assertThat(venta.total()).isEqualTo(2 * 2800 + 4200);
        assertThat(venta.lineas()).extracting(VentaResponse.Linea::subtotal).containsExactly(5600L, 4200L);
        assertThat(venta.formaPagoNombre()).isEqualTo("Transferencia");
        assertThat(venta.montoRecibido()).isNull();
        assertThat(venta.fechaHora()).isNotNull();
        assertThat(stockDe(arroz)).isEqualTo(18);
    }

    @Test
    void pagoEnEfectivoCalculaElCambio() {
        ProductoResponse aceite = productos.crear(new ProductoRequest("Aceite", "Aceites", 5, 10, 9500L, 7800L));

        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(aceite.id(), 1)), FormaPago.EFECTIVO, 20000L, null));

        assertThat(venta.montoRecibido()).isEqualTo(20000L);
        assertThat(venta.cambio()).isEqualTo(10500L);
    }

    @Test
    void efectivoInsuficienteSeRechazaSinTocarElStock() {
        ProductoResponse aceite = productos.crear(new ProductoRequest("Aceite", "Aceites", 5, 10, 9500L, 7800L));

        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(aceite.id(), 2)), FormaPago.EFECTIVO, 10000L, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("menor que el total");
        assertThat(stockDe(aceite)).isEqualTo(10);
    }

    @Test
    void cambiarElPrecioNoAlteraVentasAnteriores() {
        ProductoResponse pan = productos.crear(new ProductoRequest("Pan", "Panadería", 2, 10, 6500L, 5000L));
        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(pan.id(), 1)), FormaPago.EFECTIVO, null, null));

        productos.actualizar(pan.id(), new ProductoRequest("Pan", "Panadería", 2, null, 7000L, 5200L));

        VentaResponse guardada = ventas.obtener(venta.id());
        assertThat(guardada.total()).isEqualTo(6500L);
        assertThat(guardada.lineas().get(0).precioUnitario()).isEqualTo(6500L);
    }

    @Test
    void productoSinPrecioNoSePuedeVender() {
        Producto sinPrecio = productoRepository.save(new Producto("Producto viejo", null, 1, null, null));
        sinPrecio.aumentarStock(5);

        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(sinPrecio.getId(), 1)), FormaPago.EFECTIVO, null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene precio de venta");
    }

    @Test
    void comprobanteIncluyeProductosTotalYCambio() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 20, 2800L, 2200L));
        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(arroz.id(), 3)), FormaPago.EFECTIVO, 10000L, null));

        ComprobanteResponse comprobante = ventas.comprobante(venta.id(), "300 123 4567");

        assertThat(comprobante.texto())
                .contains("Comprobante de venta #" + venta.id())
                .contains("3 x Arroz ($ 2.800 c/u): $ 8.400")
                .contains("TOTAL: $ 8.400")
                .contains("Cambio: $ 1.600");
        assertThat(comprobante.whatsappUrl()).startsWith("https://wa.me/573001234567?text=");
        assertThat(ventas.comprobante(venta.id(), null).whatsappUrl()).startsWith("https://wa.me/?text=");
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
