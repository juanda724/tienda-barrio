package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaFacturadaRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.request.VerificacionRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.dto.response.VerificacionResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.EstadoPago;
import co.tiendabarrio.model.EstadoVerificacion;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;
import co.tiendabarrio.service.VerificacionService;

/** Verificación de F-06: con una diferencia entre lo facturado y lo recibido, el pago queda bloqueado. */
@SpringBootTest
@Transactional
class ValidacionRecepcionTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    IngresoService ingresos;
    @Autowired
    VerificacionService verificaciones;

    @Test
    void conDiferenciasElPagoSeBloqueaHastaResolverlas() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(arroz, 20);

        // la factura dice 25 pero solo llegaron 20
        VerificacionResponse conDiferencia = verificaciones.verificar(ingreso.id(), new VerificacionRequest(
                List.of(new LineaFacturadaRequest(arroz.id(), 25)), "Faltaron 5 bultos"));

        assertThat(conDiferencia.resultado()).isEqualTo(EstadoVerificacion.CON_DIFERENCIAS);
        assertThat(conDiferencia.lineas().get(0).cantidadFacturada()).isEqualTo(25);
        assertThat(conDiferencia.lineas().get(0).cantidadRecibida()).isEqualTo(20);
        assertThat(conDiferencia.lineas().get(0).diferencia()).isEqualTo(5);

        assertThatThrownBy(() -> verificaciones.confirmarPago(ingreso.id()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("diferencias");

        // el proveedor corrige la factura y se verifica de nuevo
        VerificacionResponse aprobada = verificaciones.verificar(ingreso.id(), new VerificacionRequest(
                List.of(new LineaFacturadaRequest(arroz.id(), 20)), "Factura corregida"));
        assertThat(aprobada.resultado()).isEqualTo(EstadoVerificacion.APROBADO);

        IngresoResponse pagado = verificaciones.confirmarPago(ingreso.id());
        assertThat(pagado.estadoPago()).isEqualTo(EstadoPago.PAGADO);
        assertThat(pagado.fechaPago()).isNotNull();
        assertThat(verificaciones.listar(ingreso.id())).hasSize(2);
    }

    @Test
    void noSePuedePagarSinVerificarLaFactura() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(arroz, 20);

        assertThat(ingreso.estadoVerificacion()).isEqualTo(EstadoVerificacion.PENDIENTE);
        assertThatThrownBy(() -> verificaciones.confirmarPago(ingreso.id()))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("verificar la factura");
    }

    @Test
    void unProductoFacturadoQueNoLlegoCuentaComoDiferencia() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 0, null, null));
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(arroz, 10);

        VerificacionResponse resultado = verificaciones.verificar(ingreso.id(), new VerificacionRequest(List.of(
                new LineaFacturadaRequest(arroz.id(), 10),
                new LineaFacturadaRequest(leche.id(), 3)), null));

        assertThat(resultado.resultado()).isEqualTo(EstadoVerificacion.CON_DIFERENCIAS);
        assertThat(resultado.lineas()).hasSize(2);
        assertThat(resultado.lineas().stream().filter(l -> l.productoNombre().equals("Leche"))
                .findFirst().orElseThrow().diferencia()).isEqualTo(3);
    }

    @Test
    void unIngresoPagadoNoSeVerificaDeNuevo() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 0, null, null));
        IngresoResponse ingreso = recibir(arroz, 20);
        verificaciones.verificar(ingreso.id(), new VerificacionRequest(
                List.of(new LineaFacturadaRequest(arroz.id(), 20)), null));
        verificaciones.confirmarPago(ingreso.id());

        assertThatThrownBy(() -> verificaciones.verificar(ingreso.id(), new VerificacionRequest(
                List.of(new LineaFacturadaRequest(arroz.id(), 20)), null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("ya fue pagado");
    }

    private IngresoResponse recibir(ProductoResponse producto, int cantidad) {
        ProveedorResponse proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", null, "3001234567",
                null, List.of(producto.id())));
        return ingresos.registrar(new IngresoRequest(proveedor.id(), "FV-100",
                List.of(new LineaProductoRequest(producto.id(), cantidad))));
    }
}
