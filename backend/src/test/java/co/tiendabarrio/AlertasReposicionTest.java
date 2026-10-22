package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.groups.Tuple.tuple;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoProveedorRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.AlertaResponse;
import co.tiendabarrio.dto.response.PedidoProveedorResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.dto.response.ReposicionResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.service.AlertaService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.PedidoProveedorService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;
import co.tiendabarrio.service.ReposicionService;
import co.tiendabarrio.service.VentaService;

/**
 * Verificación de F-03: reducir el stock de un producto por debajo del mínimo y validar que el
 * sistema genere la alerta y la lista de pedido automáticamente.
 */
@SpringBootTest
@Transactional
class AlertasReposicionTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    VentaService ventas;
    @Autowired
    IngresoService ingresos;
    @Autowired
    PedidoProveedorService pedidos;
    @Autowired
    AlertaService alertas;
    @Autowired
    ReposicionService reposicion;

    @Test
    void ventaQueDejaElProductoEnElMinimoGeneraUnaSolaAlerta() {
        ProductoResponse arroz = producto("Arroz", 10, 12);

        vender(arroz, 2);
        vender(arroz, 1);

        List<AlertaResponse> activas = alertas.activas();
        assertThat(activas).hasSize(1);
        assertThat(activas.get(0).productoNombre()).isEqualTo("Arroz");
        assertThat(activas.get(0).stockActual()).isEqualTo(9);
        assertThat(activas.get(0).vista()).isFalse();
    }

    @Test
    void ventaQueNoLlegaAlMinimoNoGeneraAlerta() {
        ProductoResponse arroz = producto("Arroz", 10, 20);

        vender(arroz, 5);

        assertThat(alertas.activas()).isEmpty();
    }

    @Test
    void reabastecerPorEncimaDelMinimoCierraLaAlerta() {
        ProductoResponse arroz = producto("Arroz", 10, 11);
        ProveedorResponse proveedor = proveedor("Distribuidora", arroz);
        vender(arroz, 2);
        assertThat(alertas.activas()).hasSize(1);

        ingresos.registrar(new IngresoRequest(proveedor.id(), null, null, List.of(new LineaIngresoRequest(arroz.id(), 15, 15, 1000L))));

        assertThat(alertas.activas()).isEmpty();
    }

    @Test
    void productoNuevoConPocoStockYCambioDeMinimoGeneranAlerta() {
        producto("Pan", 5, 3);
        ProductoResponse leche = producto("Leche", 5, 8);
        assertThat(alertas.activas()).extracting(AlertaResponse::productoNombre).containsExactly("Pan");

        productos.actualizar(leche.id(), new ProductoRequest("Leche", null, 10, null, 1000L, 800L));

        assertThat(alertas.activas()).extracting(AlertaResponse::productoNombre).containsExactlyInAnyOrder("Pan", "Leche");
    }

    @Test
    void marcarVistasDejaLasAlertasActivasPeroYaNoNuevas() {
        producto("Pan", 5, 3);

        alertas.marcarVistas();

        assertThat(alertas.activas()).singleElement().extracting(AlertaResponse::vista).isEqualTo(true);
    }

    @Test
    void listaDePedidoAgrupaPorProveedorYSugiereLaCantidad() {
        ProductoResponse arroz = producto("Arroz", 10, 4);
        ProductoResponse azucar = producto("Azúcar", 8, 8);
        ProductoResponse leche = producto("Leche", 12, 5);
        producto("Sal", 3, 1);
        producto("Aceite", 5, 20);
        proveedor("Distribuidora", arroz, azucar);
        proveedor("Lácteos", leche);

        ReposicionResponse lista = reposicion.lista();

        assertThat(lista.grupos()).extracting(ReposicionResponse.Grupo::proveedorNombre)
                .containsExactly("Distribuidora", "Lácteos");
        assertThat(lista.grupos().get(0).productos())
                .extracting(ReposicionResponse.Producto::nombre, ReposicionResponse.Producto::cantidadSugerida)
                .containsExactly(
                        tuple("Arroz", 16),
                        tuple("Azúcar", 8));
        assertThat(lista.grupos().get(1).productos()).singleElement()
                .extracting(ReposicionResponse.Producto::cantidadSugerida).isEqualTo(19);
        assertThat(lista.sinProveedor()).extracting(ReposicionResponse.Producto::nombre).containsExactly("Sal");
    }

    @Test
    void productoYaPedidoApareceConSuPedidoPendiente() {
        ProductoResponse arroz = producto("Arroz", 10, 4);
        ProveedorResponse proveedor = proveedor("Distribuidora", arroz);
        PedidoProveedorResponse pedido = pedidos.crear(new PedidoProveedorRequest(proveedor.id(),
                List.of(new LineaProductoRequest(arroz.id(), 16)), null, null));

        ReposicionResponse.Producto item = reposicion.lista().grupos().get(0).productos().get(0);

        assertThat(item.pedidoPendienteId()).isEqualTo(pedido.id());
    }

    @Test
    void proveedorHabitualEsElQueEntregoPorUltimaVez() {
        ProductoResponse arroz = producto("Arroz", 10, 20);
        proveedor("A primero", arroz);
        ProveedorResponse habitual = proveedor("Z habitual", arroz);
        ingresos.registrar(new IngresoRequest(habitual.id(), null, null, List.of(new LineaIngresoRequest(arroz.id(), 1, 1, 1000L))));
        vender(arroz, 15);

        assertThat(reposicion.lista().grupos()).singleElement()
                .extracting(ReposicionResponse.Grupo::proveedorNombre).isEqualTo("Z habitual");
    }

    @Test
    void pedidoDeProductoConStockSuficienteSeRechazaPorRN02() {
        ProductoResponse aceite = producto("Aceite", 5, 20);
        ProveedorResponse proveedor = proveedor("Distribuidora", aceite);

        assertThatThrownBy(() -> pedidos.crear(new PedidoProveedorRequest(proveedor.id(),
                List.of(new LineaProductoRequest(aceite.id(), 6)), null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("RN-02");
    }

    private ProductoResponse producto(String nombre, int minimo, int stock) {
        return productos.crear(new ProductoRequest(nombre, null, minimo, stock, 1000L, 800L));
    }

    private ProveedorResponse proveedor(String nombre, ProductoResponse... suministra) {
        return proveedores.crear(new ProveedorRequest(nombre, null, "3001234567", null,
                Arrays.stream(suministra).map(ProductoResponse::id).toList()));
    }

    private void vender(ProductoResponse producto, int cantidad) {
        ventas.registrar(new VentaRequest(List.of(new LineaProductoRequest(producto.id(), cantidad)),
                FormaPago.EFECTIVO, null, null));
    }
}
