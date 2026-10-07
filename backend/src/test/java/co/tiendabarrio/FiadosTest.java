package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.AbonoRequest;
import co.tiendabarrio.dto.request.ClienteRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ClienteResponse;
import co.tiendabarrio.dto.response.EstadoCuentaResponse;
import co.tiendabarrio.dto.response.EstadoCuentaResponse.Movimiento;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.ClienteService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.VentaService;

/**
 * Verificación de F-08: registrar una venta a crédito y un abono parcial, y validar que el sistema
 * actualice correctamente el saldo pendiente del cliente.
 */
@SpringBootTest
@Transactional
class FiadosTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    VentaService ventas;
    @Autowired
    ClienteService clientes;

    ProductoResponse leche;
    ClienteResponse maria;

    @BeforeEach
    void datos() {
        leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 2, 20, 4200L, 3500L));
        maria = clientes.crear(new ClienteRequest("María Elena", "310 555 1234", null));
    }

    @Test
    void ventaACreditoQuedaANombreDelClienteYSumaASuDeuda() {
        VentaResponse venta = fiar(3);

        assertThat(venta.clienteNombre()).isEqualTo("María Elena");
        assertThat(venta.total()).isEqualTo(12600L);
        assertThat(venta.montoRecibido()).isNull();
        assertThat(clientes.obtener(maria.id()).saldoPendiente()).isEqualTo(12600L);
        assertThat(stockDe(leche)).as("el fiado también descarga el inventario").isEqualTo(17);
    }

    @Test
    void abonoParcialReduceElSaldo() {
        fiar(3);

        EstadoCuentaResponse cuenta = clientes.registrarAbono(maria.id(), new AbonoRequest(5000L, FormaPago.EFECTIVO, null));

        assertThat(cuenta.cliente().totalFiado()).isEqualTo(12600L);
        assertThat(cuenta.cliente().totalAbonado()).isEqualTo(5000L);
        assertThat(cuenta.cliente().saldoPendiente()).isEqualTo(7600L);
        assertThat(cuenta.movimientos()).extracting(Movimiento::tipo).containsExactly("VENTA", "ABONO");
        assertThat(cuenta.movimientos()).extracting(Movimiento::saldo).containsExactly(12600L, 7600L);
        assertThat(cuenta.recordatorio()).contains("$ 7.600");
        assertThat(cuenta.whatsappUrl()).startsWith("https://wa.me/573105551234");
    }

    @Test
    void abonosHastaSaldarLaDeuda() {
        fiar(1);
        clientes.registrarAbono(maria.id(), new AbonoRequest(2000L, FormaPago.EFECTIVO, null));

        EstadoCuentaResponse cuenta = clientes.registrarAbono(maria.id(),
                new AbonoRequest(2200L, FormaPago.TRANSFERENCIA, "Nequi"));

        assertThat(cuenta.cliente().saldoPendiente()).isZero();
        assertThat(cuenta.recordatorio()).contains("al día");
        assertThatThrownBy(() -> clientes.registrarAbono(maria.id(), new AbonoRequest(100L, FormaPago.EFECTIVO, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene saldo pendiente");
    }

    @Test
    void abonoMayorQueElSaldoSeRechaza() {
        fiar(1);

        assertThatThrownBy(() -> clientes.registrarAbono(maria.id(), new AbonoRequest(10000L, FormaPago.EFECTIVO, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("supera el saldo pendiente");
    }

    @Test
    void ventaACreditoSinClienteSeRechazaSinTocarElStock() {
        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(leche.id(), 2)), FormaPago.CREDITO, null, null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("Seleccione el cliente");
        assertThat(stockDe(leche)).isEqualTo(20);
    }

    @Test
    void ventaDeContadoNoQuedaAsociadaAlCliente() {
        VentaResponse venta = ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(leche.id(), 1)), FormaPago.EFECTIVO, null, maria.id()));

        assertThat(venta.clienteId()).isNull();
        assertThat(clientes.obtener(maria.id()).saldoPendiente()).isZero();
    }

    @Test
    void comprobanteDelFiadoIncluyeClienteYSaldo() {
        VentaResponse venta = fiar(2);

        var comprobante = ventas.comprobante(venta.id(), null);

        assertThat(comprobante.texto()).contains("Cliente: María Elena").contains("Saldo pendiente de su cuenta: $ 8.400");
        assertThat(comprobante.whatsappUrl()).as("va al teléfono del cliente").startsWith("https://wa.me/573105551234");
    }

    @Test
    void clientesConDeudaAparecenPrimero() {
        clientes.crear(new ClienteRequest("Aaron al día", null, null));
        fiar(1);

        assertThat(clientes.listar()).extracting(ClienteResponse::nombre).startsWith("María Elena");
    }

    private VentaResponse fiar(int cantidad) {
        return ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(leche.id(), cantidad)), FormaPago.CREDITO, null, maria.id()));
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
