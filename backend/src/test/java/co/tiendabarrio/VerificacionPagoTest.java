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

import co.tiendabarrio.dto.request.IngresoRequest;
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
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.ComprobantePagoService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/**
 * Verificación de F-06: recibir un pedido con una diferencia entre lo facturado y lo entregado, y
 * validar que el sistema impida confirmar el pago hasta resolver la diferencia.
 */
@SpringBootTest
@Transactional
class VerificacionPagoTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    IngresoService ingresos;
    @Autowired
    ComprobantePagoService comprobantes;

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
    void facturaQueCoincideQuedaAprobadaYSePuedePagar() {
        IngresoResponse ingreso = recibir(linea(aceite, 12, 12, 7800L), linea(arroz, 20, 20, 2200L));

        assertThat(ingreso.resultadoVerificacion()).isEqualTo("APROBADO");
        assertThat(ingreso.totalFacturado()).isEqualTo(12 * 7800 + 20 * 2200);
        assertThat(ingreso.estadoPago()).isEqualTo("PENDIENTE");
        assertThat(ingreso.pagable()).isTrue();

        IngresoResponse pagado = ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, null));

        assertThat(pagado.estadoPago()).isEqualTo("PAGADO");
        assertThat(pagado.montoPagado()).isEqualTo(ingreso.totalFacturado());
        assertThat(pagado.fechaPago()).isNotNull();
    }

    @Test
    void diferenciaBloqueaElPagoYElCredito() {
        IngresoResponse ingreso = recibir(linea(aceite, 10, 12, 7800L));

        assertThat(ingreso.resultadoVerificacion()).isEqualTo("CON_DIFERENCIAS");
        assertThat(ingreso.lineas().get(0).diferencia()).isEqualTo(-2);
        assertThat(ingreso.pagable()).isFalse();
        assertThat(stockDe(aceite)).as("al stock entra solo lo recibido").isEqualTo(12);

        assertThatThrownBy(() -> ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, null)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("RN-01");
        assertThatThrownBy(() -> ingresos.pagar(ingreso.id(),
                new PagoProveedorRequest(FormaPago.CREDITO, LocalDate.now().plusDays(15), null)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("diferencias");
    }

    @Test
    void resolverLaDiferenciaDesbloqueaElPagoPorLoRecibido() {
        IngresoResponse ingreso = recibir(linea(aceite, 10, 12, 7800L));

        IngresoResponse resuelto = ingresos.resolverDiferencias(ingreso.id(),
                new ResolverDiferenciasRequest("El proveedor envió nota crédito por 2 unidades"));

        assertThat(resuelto.resultadoVerificacion()).isEqualTo("DIFERENCIAS_RESUELTAS");
        assertThat(resuelto.totalAPagar()).as("se paga lo recibido").isEqualTo(10 * 7800);
        assertThat(resuelto.notaVerificacion()).contains("nota crédito");

        IngresoResponse pagado = ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.TRANSFERENCIA, null, null));
        assertThat(pagado.montoPagado()).isEqualTo(10 * 7800);
    }

    @Test
    void creditoConVencimientoYLuegoPago() {
        IngresoResponse ingreso = recibir(linea(arroz, 20, 20, 2200L));
        LocalDate vence = LocalDate.now().plusDays(15);

        IngresoResponse credito = ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.CREDITO, vence, null));

        assertThat(credito.estadoPago()).isEqualTo("CREDITO");
        assertThat(credito.fechaVencimiento()).isEqualTo(vence);
        assertThat(credito.vencido()).isFalse();
        assertThat(credito.pagable()).isTrue();

        IngresoResponse pagado = ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, null));
        assertThat(pagado.estadoPago()).isEqualTo("PAGADO");
        assertThatThrownBy(() -> ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, null)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("ya está pagado");
    }

    @Test
    void productoFacturadoQueNoLlegoNoSumaStockPeroQuedaComoDiferencia() {
        IngresoResponse ingreso = recibir(linea(aceite, 12, 12, 7800L), linea(arroz, 0, 5, 2200L));

        assertThat(ingreso.resultadoVerificacion()).isEqualTo("CON_DIFERENCIAS");
        assertThat(ingreso.lineas()).hasSize(2);
        assertThat(stockDe(arroz)).isEqualTo(2);
    }

    @Test
    void elCostoDelProductoSeActualizaConElDeLaFactura() {
        recibir(linea(aceite, 12, 12, 8100L));

        assertThat(productoRepository.findById(aceite.id()).orElseThrow().getCosto()).isEqualTo(8100L);
    }

    @Test
    void ingresoSinCantidadesSeRechaza() {
        assertThatThrownBy(() -> recibir(linea(aceite, 0, 0, 7800L)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("al menos un producto");
    }

    @Test
    void pagoEnEfectivoRegistraLoEntregadoYElCambioDelRepartidor() {
        IngresoResponse ingreso = recibir(linea(aceite, 12, 12, 7800L)); // $ 93.600

        IngresoResponse pagado = ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, 100000L));

        assertThat(pagado.montoEntregado()).isEqualTo(100000L);
        assertThat(pagado.cambio()).isEqualTo(6400L);
    }

    @Test
    void efectivoEntregadoInsuficienteSeRechazaYNoQuedaPagado() {
        IngresoResponse ingreso = recibir(linea(aceite, 12, 12, 7800L));

        assertThatThrownBy(() -> ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, 90000L)))
                .isInstanceOf(NegocioException.class).hasMessageContaining("menor que el total a pagar");
        assertThat(ingresos.listar().get(0).estadoPago()).isEqualTo("PENDIENTE");
    }

    @Test
    void loEntregadoSoloSeGuardaEnPagosEnEfectivo() {
        IngresoResponse ingreso = recibir(linea(aceite, 12, 12, 7800L));

        IngresoResponse pagado = ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.TRANSFERENCIA, null, 100000L));

        assertThat(pagado.montoEntregado()).isNull();
        assertThat(pagado.cambio()).isNull();
    }

    @Test
    void comprobanteDePagoConTotalYCambio() {
        IngresoResponse ingreso = recibir(linea(aceite, 12, 12, 7800L));
        assertThatThrownBy(() -> comprobantes.generar(ingreso.id()))
                .isInstanceOf(NegocioException.class).hasMessageContaining("todavía no está pagado");

        ingresos.pagar(ingreso.id(), new PagoProveedorRequest(FormaPago.EFECTIVO, null, 100000L));
        var comprobante = comprobantes.generar(ingreso.id());

        assertThat(comprobante.texto())
                .contains("Comprobante de pago al proveedor")
                .contains("Ingreso #" + ingreso.id() + " · Factura FV-1")
                .contains("TOTAL PAGADO: $ 93.600")
                .contains("Efectivo entregado: $ 100.000")
                .contains("Cambio devuelto: $ 6.400");
        assertThat(comprobante.whatsappUrl()).startsWith("https://wa.me/573001234567");
        assertThat(comprobante.correoUrl()).isNull();
    }

    private IngresoResponse recibir(LineaIngresoRequest... lineas) {
        return ingresos.registrar(new IngresoRequest(proveedor.id(), null, "FV-1", List.of(lineas)));
    }

    private static LineaIngresoRequest linea(ProductoResponse p, int recibida, int facturada, long costo) {
        return new LineaIngresoRequest(p.id(), recibida, facturada, costo);
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
