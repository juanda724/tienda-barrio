package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.request.EntregaFaltantesRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaDevolucionRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.request.ResolverDiferenciasRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.MotivoDevolucion;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.ComprobantePagoService;
import co.tiendabarrio.service.DevolucionService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/** El proveedor entrega después, en una o varias entregas, los productos que faltaron en un ingreso. */
@SpringBootTest
@Transactional
class EntregaFaltantesTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    IngresoService ingresos;
    @Autowired
    InventarioService inventario;
    @Autowired
    ComprobantePagoService comprobantes;
    @Autowired
    DevolucionService devoluciones;

    ProductoResponse aceite;
    ProductoResponse arroz;
    ProveedorResponse proveedor;

    @BeforeEach
    void datos() {
        aceite = productos.crear(new ProductoRequest("Aceite", null, 5, 2, 9500L, 7800L));
        arroz = productos.crear(new ProductoRequest("Arroz", null, 5, 2, 2800L, 2200L));
        proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", null, "3001234567", null,
                List.of(aceite.id(), arroz.id())));
    }

    @Test
    void entregaCompletaDeFaltantesResuelveElIngresoYSePagaLoFacturado() {
        IngresoResponse ingreso = recibir(linea(aceite, 10, 12, 7800L));

        IngresoResponse resuelto = ingresos.registrarEntregaFaltantes(ingreso.id(), entrega("Llegó con el repartidor", aceite, 2));

        assertThat(resuelto.resultadoVerificacion()).isEqualTo("DIFERENCIAS_RESUELTAS");
        assertThat(resuelto.notaVerificacion()).contains("entregó los productos faltantes");
        assertThat(resuelto.lineas().get(0).cantidadRecibida()).as("lo del primer día no cambia").isEqualTo(10);
        assertThat(resuelto.lineas().get(0).cantidadEntregadaDespues()).isEqualTo(2);
        assertThat(resuelto.lineas().get(0).diferencia()).isZero();
        assertThat(resuelto.totalAPagar()).isEqualTo(12 * 7800);
        assertThat(resuelto.pagable()).isTrue();
        assertThat(resuelto.entregasFaltantes()).hasSize(1);
        assertThat(stockDe(aceite)).isEqualTo(2 + 12);
        assertThat(inventario.historial(aceite.id()).get(0).referencia()).contains("Entrega de faltantes del ingreso #" + ingreso.id());
    }

    @Test
    void entregaParcialSigueBloqueandoElPagoHastaCompletar() {
        IngresoResponse ingreso = recibir(linea(aceite, 7, 12, 7800L), linea(arroz, 0, 5, 2200L));

        IngresoResponse parcial = ingresos.registrarEntregaFaltantes(ingreso.id(), entrega(null, aceite, 3));

        assertThat(parcial.resultadoVerificacion()).isEqualTo("CON_DIFERENCIAS");
        assertThat(parcial.lineas().get(0).diferencia()).isEqualTo(-2);
        assertThat(parcial.pagable()).isFalse();
        assertThatThrownBy(() -> ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, null)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("RN-01");

        IngresoResponse completo = ingresos.registrarEntregaFaltantes(ingreso.id(), new EntregaFaltantesRequest(
                List.of(new EntregaFaltantesRequest.Linea(aceite.id(), 2), new EntregaFaltantesRequest.Linea(arroz.id(), 5)), null));

        assertThat(completo.resultadoVerificacion()).isEqualTo("DIFERENCIAS_RESUELTAS");
        assertThat(completo.entregasFaltantes()).hasSize(2);
        assertThat(completo.totalAPagar()).isEqualTo(12 * 7800 + 5 * 2200);
        assertThat(stockDe(arroz)).isEqualTo(2 + 5);
    }

    @Test
    void entregaParcialYLuegoAjusteDeFacturaPagaLoRecibidoEnTotal() {
        IngresoResponse ingreso = recibir(linea(aceite, 7, 12, 7800L));
        ingresos.registrarEntregaFaltantes(ingreso.id(), entrega(null, aceite, 3));

        IngresoResponse ajustado = ingresos.resolverDiferencias(ingreso.id(),
                new ResolverDiferenciasRequest("Las otras 2 no las tenían; ajustaron la factura"));

        assertThat(ajustado.totalAPagar()).isEqualTo(10 * 7800);
    }

    @Test
    void noSePuedeEntregarMasDeLoQueFaltaNiProductosSinFaltante() {
        IngresoResponse ingreso = recibir(linea(aceite, 10, 12, 7800L), linea(arroz, 5, 5, 2200L));

        assertThatThrownBy(() -> ingresos.registrarEntregaFaltantes(ingreso.id(), entrega(null, aceite, 3)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("solo faltan 2");
        assertThatThrownBy(() -> ingresos.registrarEntregaFaltantes(ingreso.id(), entrega(null, arroz, 1)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no faltan unidades");
        assertThatThrownBy(() -> ingresos.registrarEntregaFaltantes(ingreso.id(), entrega(null, aceite, 0)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("al menos un producto");
        assertThat(stockDe(aceite)).as("nada entra al stock si la entrega se rechaza").isEqualTo(2 + 10);
    }

    @Test
    void ingresoSinDiferenciasNoAdmiteEntregaDeFaltantes() {
        IngresoResponse ingreso = recibir(linea(aceite, 12, 12, 7800L));

        assertThatThrownBy(() -> ingresos.registrarEntregaFaltantes(ingreso.id(), entrega(null, aceite, 1)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("no tiene diferencias");
    }

    @Test
    void lasUnidadesEntregadasDespuesSePuedenDevolver() {
        IngresoResponse ingreso = recibir(linea(aceite, 0, 2, 7800L));
        IngresoResponse resuelto = ingresos.registrarEntregaFaltantes(ingreso.id(), entrega(null, aceite, 2));

        assertThat(resuelto.totalRecibido()).isEqualTo(2 * 7800);
        var devolucion = devoluciones.registrar(new DevolucionRequest(ingreso.id(), "Llegaron golpeadas",
                List.of(new LineaDevolucionRequest(aceite.id(), 2, MotivoDevolucion.MAL_ESTADO))));
        assertThat(devolucion.lineas()).hasSize(1);
        assertThatThrownBy(() -> devoluciones.registrar(new DevolucionRequest(ingreso.id(), "Otra",
                List.of(new LineaDevolucionRequest(aceite.id(), 1, MotivoDevolucion.MAL_ESTADO)))))
                .isInstanceOf(NegocioException.class).hasMessageContaining("solo se pueden devolver 0");
    }

    @Test
    void comprobanteDePagoMuestraLasEntregasPosteriores() {
        IngresoResponse ingreso = recibir(linea(aceite, 10, 12, 7800L));
        ingresos.registrarEntregaFaltantes(ingreso.id(), entrega("Las trajo el repartidor", aceite, 2));
        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, null));

        assertThat(comprobantes.generar(ingreso.id()).texto())
                .contains("Entregas posteriores de faltantes:")
                .contains("2 x Aceite (Las trajo el repartidor)")
                .contains("TOTAL PAGADO: $ 93.600");
    }

    private IngresoResponse recibir(LineaIngresoRequest... lineas) {
        return ingresos.registrar(new IngresoRequest(proveedor.id(), null, "FV-1", List.of(lineas)));
    }

    private static LineaIngresoRequest linea(ProductoResponse p, int recibida, int facturada, long costo) {
        return new LineaIngresoRequest(p.id(), recibida, facturada, costo);
    }

    private static EntregaFaltantesRequest entrega(String nota, ProductoResponse p, int cantidad) {
        return new EntregaFaltantesRequest(List.of(new EntregaFaltantesRequest.Linea(p.id(), cantidad)), nota);
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
