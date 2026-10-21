package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CompraRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.response.AlertaResponse;
import co.tiendabarrio.dto.response.ListaPedidoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.model.EstadoAlerta;
import co.tiendabarrio.service.AlertaService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.PedidoService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;
import co.tiendabarrio.service.ReposicionService;

/** Verificación de F-03: bajar un producto del mínimo y validar la alerta y la lista de pedido automáticas. */
@SpringBootTest
@Transactional
class ReposicionAlertasTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    PedidoService pedidos;
    @Autowired
    InventarioService inventario;
    @Autowired
    AlertaService alertas;
    @Autowired
    ReposicionService reposicion;

    @Test
    void alLlegarAlMinimoSeGeneraLaAlertaYLaListaDePedido() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10, null, null));
        proveedores.crear(new ProveedorRequest("Distribuidora", "Luis", "3001234567", null, List.of(arroz.id())));
        assertThat(alertas.listar(EstadoAlerta.ACTIVA)).isEmpty();

        pedidos.registrar(new PedidoRequest(List.of(new LineaProductoRequest(arroz.id(), 5))));

        List<AlertaResponse> activas = alertas.listar(EstadoAlerta.ACTIVA);
        assertThat(activas).hasSize(1);
        assertThat(activas.get(0).productoNombre()).isEqualTo("Arroz");
        assertThat(activas.get(0).stockActual()).isEqualTo(5);

        ListaPedidoResponse lista = reposicion.listaPedido();
        assertThat(lista.totalProductos()).isEqualTo(1);
        assertThat(lista.proveedores()).hasSize(1);
        assertThat(lista.proveedores().get(0).proveedorNombre()).isEqualTo("Distribuidora");
        assertThat(lista.proveedores().get(0).productos().get(0).nombre()).isEqualTo("Arroz");
        // se sugiere pedir lo necesario para duplicar el mínimo: 5 * 2 - 5
        assertThat(lista.proveedores().get(0).productos().get(0).cantidadSugerida()).isEqualTo(5);
    }

    @Test
    void noSeDuplicanAlertasYSeResuelvenAlReabastecer() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 10, null, null));

        pedidos.registrar(new PedidoRequest(List.of(new LineaProductoRequest(arroz.id(), 6))));
        pedidos.registrar(new PedidoRequest(List.of(new LineaProductoRequest(arroz.id(), 1))));
        assertThat(alertas.listar(EstadoAlerta.ACTIVA)).hasSize(1);

        inventario.registrarCompra(new CompraRequest(arroz.id(), 20, null));

        assertThat(alertas.listar(EstadoAlerta.ACTIVA)).isEmpty();
        assertThat(alertas.listar(EstadoAlerta.RESUELTA)).hasSize(1);
        assertThat(reposicion.listaPedido().totalProductos()).isEqualTo(0);
    }

    @Test
    void productoSinProveedorApareceAparteEnLaLista() {
        productos.crear(new ProductoRequest("Pan", "Panadería", 5, 2, null, null));

        ListaPedidoResponse lista = reposicion.listaPedido();

        assertThat(lista.proveedores()).isEmpty();
        assertThat(lista.sinProveedor()).hasSize(1);
        assertThat(lista.sinProveedor().get(0).nombre()).isEqualTo("Pan");
    }
}
