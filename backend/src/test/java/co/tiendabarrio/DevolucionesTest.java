package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CambioEstadoDevolucionRequest;
import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaDevolucionRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.response.DevolucionResponse;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.EstadoDevolucion;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.MotivoDevolucion;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.DevolucionService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/**
 * Verificación de F-07: registrar un producto en mal estado al momento de la entrega y validar que
 * el sistema genere la nota de devolución correspondiente, y su seguimiento hasta la resolución.
 */
@SpringBootTest
@Transactional
@TestPropertySource(properties = "tienda.evidencias.carpeta=target/evidencias-prueba")
class DevolucionesTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    IngresoService ingresos;
    @Autowired
    DevolucionService devoluciones;
    @Autowired
    InventarioService inventario;

    ProductoResponse leche;
    ProveedorResponse proveedor;
    IngresoResponse ingreso;

    @BeforeEach
    void datos() {
        leche = productos.crear(new ProductoRequest("Leche", null, 5, 4, 4200L, 3500L));
        proveedor = proveedores.crear(new ProveedorRequest("Lácteos", "Ana", "3107654321", "ana@example.com",
                List.of(leche.id())));
        ingreso = ingresos.registrar(new IngresoRequest(proveedor.id(), null, "LP-1",
                List.of(new LineaIngresoRequest(leche.id(), 12, 12, 3500L))));
    }

    @Test
    void devolucionSacaLoDevueltoDelStockYGeneraLaNota() {
        DevolucionResponse d = devolver(3, MotivoDevolucion.MAL_ESTADO);

        assertThat(stockDe(leche)).isEqualTo(4 + 12 - 3);
        assertThat(d.estado().estado()).isEqualTo(EstadoDevolucion.PENDIENTE);
        assertThat(d.valor()).isEqualTo(3 * 3500);
        assertThat(d.nota())
                .contains("NOTA DE DEVOLUCIÓN #" + d.id())
                .contains("Factura LP-1")
                .contains("3 x Leche — En mal estado o empaque roto ($ 10.500)")
                .contains("Empaques rotos");
        assertThat(d.whatsappUrl()).startsWith("https://wa.me/573107654321");
        assertThat(d.correoUrl()).startsWith("mailto:ana@example.com");
        assertThat(inventario.historial(leche.id()).get(0).origen().name()).isEqualTo("DEVOLUCION_PROVEEDOR");
    }

    @Test
    void devolucionEnCursoBloqueaElPagoDelIngreso() {
        DevolucionResponse d = devolver(3, MotivoDevolucion.MAL_ESTADO);

        assertThat(ingresos.listar().get(0).pagable()).isFalse();
        assertThat(ingresos.listar().get(0).devolucionEnCursoId()).isEqualTo(d.id());
        assertThatThrownBy(() -> ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("devolución #" + d.id());
    }

    @Test
    void notaCreditoDescuentaElValorDelPago() {
        DevolucionResponse d = devolver(3, MotivoDevolucion.MAL_ESTADO);
        cambiar(d, EstadoDevolucion.ENVIADA);

        DevolucionResponse resuelta = cambiar(d, EstadoDevolucion.CON_NOTA_CREDITO);

        assertThat(resuelta.montoCredito()).isEqualTo(10500L);
        IngresoResponse actualizado = ingresos.listar().get(0);
        assertThat(actualizado.pagable()).isTrue();
        assertThat(actualizado.totalAPagar()).isEqualTo(12 * 3500 - 10500);
        assertThat(ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null)).montoPagado())
                .isEqualTo(31500L);
    }

    @Test
    void reemplazoDevuelveLosProductosAlInventario() {
        DevolucionResponse d = devolver(3, MotivoDevolucion.VENCIDO);

        DevolucionResponse resuelta = cambiar(d, EstadoDevolucion.REEMPLAZADA);

        assertThat(stockDe(leche)).isEqualTo(16);
        assertThat(resuelta.historial()).extracting(c -> c.estado().estado())
                .containsExactly(EstadoDevolucion.PENDIENTE, EstadoDevolucion.REEMPLAZADA);
        assertThat(resuelta.siguientesEstados()).isEmpty();
        assertThat(ingresos.listar().get(0).totalAPagar()).as("el pago es por la factura completa").isEqualTo(42000L);
    }

    @Test
    void devolucionRechazadaDesbloqueaElPagoSinDescuento() {
        DevolucionResponse d = devolver(2, MotivoDevolucion.MAL_ESTADO);

        cambiar(d, EstadoDevolucion.RECHAZADA);

        assertThat(ingresos.listar().get(0).pagable()).isTrue();
        assertThat(ingresos.listar().get(0).totalAPagar()).isEqualTo(42000L);
        assertThat(stockDe(leche)).as("lo rechazado no vuelve al inventario").isEqualTo(14);
    }

    @Test
    void noSePuedeDevolverMasDeLoRecibido() {
        devolver(10, MotivoDevolucion.MAL_ESTADO);

        assertThatThrownBy(() -> devolver(3, MotivoDevolucion.MAL_ESTADO))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("solo se pueden devolver 2 unidades");
    }

    @Test
    void noSePuedeReabrirUnaDevolucionResuelta() {
        DevolucionResponse d = devolver(1, MotivoDevolucion.OTRO);
        cambiar(d, EstadoDevolucion.RECHAZADA);

        assertThatThrownBy(() -> cambiar(d, EstadoDevolucion.ENVIADA))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("No se puede pasar");
    }

    @Test
    void fotosDeEvidenciaSeGuardanYSePuedenLeer() {
        DevolucionResponse d = devolver(1, MotivoDevolucion.MAL_ESTADO);
        byte[] contenido = {(byte) 0xFF, (byte) 0xD8, 1, 2, 3};

        DevolucionResponse conFoto = devoluciones.agregarFotos(d.id(),
                List.of(new MockMultipartFile("fotos", "empaque.jpg", "image/jpeg", contenido)));

        assertThat(conFoto.fotos()).hasSize(1);
        assertThat(conFoto.nota()).contains("1 foto");
        var foto = devoluciones.foto(d.id(), conFoto.fotos().get(0).id());
        assertThat(devoluciones.leer(foto)).isEqualTo(contenido);
    }

    @Test
    void soloSeAceptanImagenes() {
        DevolucionResponse d = devolver(1, MotivoDevolucion.MAL_ESTADO);

        assertThatThrownBy(() -> devoluciones.agregarFotos(d.id(),
                List.of(new MockMultipartFile("fotos", "nota.txt", "text/plain", new byte[] {1}))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Solo se aceptan fotos");
    }

    private DevolucionResponse devolver(int cantidad, MotivoDevolucion motivo) {
        return devoluciones.registrar(new DevolucionRequest(ingreso.id(), "Empaques rotos al recibir",
                List.of(new LineaDevolucionRequest(leche.id(), cantidad, motivo))));
    }

    private DevolucionResponse cambiar(DevolucionResponse d, EstadoDevolucion estado) {
        return devoluciones.cambiarEstado(d.id(), new CambioEstadoDevolucionRequest(estado, null));
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
