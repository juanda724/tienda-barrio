package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CambioEstadoRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.PedidoProveedorRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.request.ResolverDiferenciasRequest;
import co.tiendabarrio.dto.response.EstadoResponse;
import co.tiendabarrio.dto.response.FacturaPedidoResponse;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.PedidoProveedorResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.EstadoPedido;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.FacturaPedidoService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.PedidoProveedorService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/** Pedido a proveedor: envío por WhatsApp/correo, estados y recepción con ingreso de mercancía. */
@SpringBootTest
@Transactional
class PedidosProveedorTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    PedidoProveedorService pedidos;
    @Autowired
    IngresoService ingresos;
    @Autowired
    FacturaPedidoService facturas;

    ProductoResponse arroz;
    ProductoResponse leche;
    ProveedorResponse distribuidora;

    @BeforeEach
    void datos() {
        arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 10, 4, 1000L, 800L));
        leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 3, 1000L, 800L));
        distribuidora = proveedores.crear(new ProveedorRequest("Distribuidora", "Luis", "300 123 4567",
                "pedidos@example.com", List.of(arroz.id())));
    }

    @Test
    void pedidoNuevoQuedaEnEsperaConMensajeYEnlaces() {
        PedidoProveedorResponse pedido = crearPedido(20);

        assertThat(pedido.estado().estado()).isEqualTo(EstadoPedido.EN_ESPERA);
        assertThat(pedido.historial()).hasSize(1);
        assertThat(pedido.siguientesEstados()).extracting(EstadoResponse::estado)
                .containsExactly(EstadoPedido.ACEPTADO, EstadoPedido.RECHAZADO, EstadoPedido.CANCELADO);
        assertThat(pedido.mensaje()).contains("Hola Luis").contains("- Arroz: 20").contains("Pedido #" + pedido.id());
        assertThat(pedido.whatsappUrl()).startsWith("https://wa.me/573001234567?text=").doesNotContain("+");
        assertThat(pedido.correoUrl()).startsWith("https://mail.google.com/mail/?view=cm&fs=1&to=pedidos%40example.com&su=Pedido");
        assertThat(stockDe(arroz)).as("crear el pedido no cambia el stock").isEqualTo(4);
    }

    @Test
    void sinCorreoNoHayEnlaceDeCorreo() {
        ProveedorResponse sinCorreo = proveedores.crear(new ProveedorRequest("Lácteos", null, "3107654321",
                null, List.of(leche.id())));

        PedidoProveedorResponse pedido = pedidos.crear(new PedidoProveedorRequest(sinCorreo.id(),
                List.of(new LineaProductoRequest(leche.id(), 6)), null, null));

        assertThat(pedido.correoUrl()).isNull();
        assertThat(pedido.whatsappUrl()).startsWith("https://wa.me/573107654321");
    }

    @Test
    void pedidoAvanzaPorSusEstadosYGuardaElHistorial() {
        Long id = crearPedido(20).id();
        LocalDate entrega = LocalDate.now().plusDays(2);

        pedidos.cambiarEstado(id, new CambioEstadoRequest(EstadoPedido.ACEPTADO, entrega, "Confirmó por WhatsApp"));
        pedidos.cambiarEstado(id, new CambioEstadoRequest(EstadoPedido.EN_PROCESO, null, null));
        PedidoProveedorResponse pedido = pedidos.cambiarEstado(id,
                new CambioEstadoRequest(EstadoPedido.EN_CAMINO, null, null));

        assertThat(pedido.estado().estado()).isEqualTo(EstadoPedido.EN_CAMINO);
        assertThat(pedido.fechaEstimadaEntrega()).isEqualTo(entrega);
        assertThat(pedido.historial()).extracting(c -> c.estado().estado()).containsExactly(
                EstadoPedido.EN_ESPERA, EstadoPedido.ACEPTADO, EstadoPedido.EN_PROCESO, EstadoPedido.EN_CAMINO);
        assertThat(pedido.historial().get(1).nota()).isEqualTo("Confirmó por WhatsApp");
        assertThat(pedido.siguientesEstados()).isEmpty();
        assertThat(pedido.puedeRecibirse()).isTrue();
    }

    @Test
    void noSePuedeSaltarEstadosNiMarcarEntregadoAMano() {
        Long id = crearPedido(20).id();

        assertThatThrownBy(() -> pedidos.cambiarEstado(id, new CambioEstadoRequest(EstadoPedido.EN_CAMINO, null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("No se puede pasar");
        assertThatThrownBy(() -> pedidos.cambiarEstado(id, new CambioEstadoRequest(EstadoPedido.ENTREGADO, null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("registre el ingreso");
    }

    @Test
    void ingresoDelPedidoSumaStockYLoMarcaEntregado() {
        Long id = crearPedido(20).id();
        pedidos.cambiarEstado(id, new CambioEstadoRequest(EstadoPedido.ACEPTADO, null, null));

        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(distribuidora.id(), id, "FV-1",
                List.of(new LineaIngresoRequest(arroz.id(), 20, 20, 1000L))));

        assertThat(ingreso.pedidoId()).isEqualTo(id);
        assertThat(stockDe(arroz)).isEqualTo(24);
        PedidoProveedorResponse pedido = pedidos.obtener(id);
        assertThat(pedido.estado().estado()).isEqualTo(EstadoPedido.ENTREGADO);
        assertThat(pedido.puedeRecibirse()).isFalse();
        assertThat(pedido.historial().get(pedido.historial().size() - 1).nota())
                .isEqualTo("Recibido con el ingreso #" + ingreso.id());
    }

    @Test
    void pedidoCanceladoOYaEntregadoNoPuedeRecibirse() {
        Long cancelado = crearPedido(5).id();
        pedidos.cambiarEstado(cancelado, new CambioEstadoRequest(EstadoPedido.CANCELADO, null, "Ya no se necesita"));

        assertThatThrownBy(() -> ingresos.registrar(new IngresoRequest(distribuidora.id(), cancelado, null,
                List.of(new LineaIngresoRequest(arroz.id(), 5, 5, 1000L)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("cancelado");
        assertThat(stockDe(arroz)).isEqualTo(4);
    }

    @Test
    void ingresoConPedidoDeOtroProveedorSeRechaza() {
        ProveedorResponse otro = proveedores.crear(new ProveedorRequest("Otro", null, "3000000000", null,
                List.of(arroz.id())));
        Long id = crearPedido(20).id();

        assertThatThrownBy(() -> ingresos.registrar(new IngresoRequest(otro.id(), id, null,
                List.of(new LineaIngresoRequest(arroz.id(), 20, 20, 1000L)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("es de Distribuidora");
    }

    @Test
    void pedidoConProductoNoAsociadoSeRechaza() {
        assertThatThrownBy(() -> pedidos.crear(new PedidoProveedorRequest(distribuidora.id(),
                List.of(new LineaProductoRequest(leche.id(), 5)), null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no está asociado");
    }

    @Test
    void facturaDelPedidoSoloCuandoEstaRecibidoYPagado() {
        Long id = crearPedido(20).id();
        assertThatThrownBy(() -> facturas.generar(id))
                .isInstanceOf(NegocioException.class).hasMessageContaining("todavía no se ha recibido");

        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(distribuidora.id(), id, "FV-9",
                List.of(new LineaIngresoRequest(arroz.id(), 20, 20, 1000L))));
        PedidoProveedorResponse recibido = pedidos.obtener(id);
        assertThat(recibido.ingresoId()).isEqualTo(ingreso.id());
        assertThat(recibido.estadoPagoIngreso()).isEqualTo("PENDIENTE");
        assertThatThrownBy(() -> facturas.generar(id))
                .isInstanceOf(NegocioException.class).hasMessageContaining("todavía no está pagado");

        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, 25000L));
        assertThat(pedidos.obtener(id).estadoPagoIngreso()).isEqualTo("PAGADO");
        FacturaPedidoResponse factura = facturas.generar(id);

        assertThat(factura.ingresoId()).isEqualTo(ingreso.id());
        assertThat(factura.proveedorContacto()).isEqualTo("Luis");
        assertThat(factura.proveedorCorreo()).isEqualTo("pedidos@example.com");
        assertThat(factura.lineas()).singleElement().satisfies(l -> {
            assertThat(l.cantidadPedida()).isEqualTo(20);
            assertThat(l.cantidadCobrada()).isEqualTo(20);
            assertThat(l.subtotal()).isEqualTo(20_000L);
        });
        assertThat(factura.totalPagado()).isEqualTo(20_000L);
        assertThat(factura.cambio()).isEqualTo(5_000L);
        assertThat(factura.texto())
                .contains("Factura del pedido #" + id)
                .contains("Contacto: Luis")
                .contains("Teléfono: 300 123 4567")
                .contains("Correo: pedidos@example.com")
                .contains("Factura del proveedor: FV-9")
                .contains("- 20 x Arroz a $ 1.000 = $ 20.000")
                .contains("TOTAL PAGADO: $ 20.000")
                .contains("Cambio devuelto: $ 5.000");
        assertThat(factura.correoUrl()).startsWith("https://mail.google.com/mail/?view=cm&fs=1&to=pedidos%40example.com&su=Factura");
    }

    @Test
    void facturaDeUnPedidoConFacturaAjustadaCobraLoRecibido() {
        Long id = crearPedido(20).id();
        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(distribuidora.id(), id, null,
                List.of(new LineaIngresoRequest(arroz.id(), 18, 20, 1000L))));
        ingresos.resolverDiferencias(ingreso.id(), new ResolverDiferenciasRequest("Ajustaron la factura"));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.TRANSFERENCIA, null, null));

        FacturaPedidoResponse factura = facturas.generar(id);

        assertThat(factura.lineas().get(0).cantidadCobrada()).isEqualTo(18);
        assertThat(factura.subtotal()).isEqualTo(18_000L);
        assertThat(factura.totalPagado()).isEqualTo(18_000L);
    }

    private PedidoProveedorResponse crearPedido(int cantidadArroz) {
        return pedidos.crear(new PedidoProveedorRequest(distribuidora.id(),
                List.of(new LineaProductoRequest(arroz.id(), cantidadArroz)), null, null));
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
