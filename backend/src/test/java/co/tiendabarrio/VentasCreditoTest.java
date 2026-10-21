package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.AbonoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ClienteCreditoResponse;
import co.tiendabarrio.dto.response.ClienteDetalleResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.TipoPago;
import co.tiendabarrio.service.CreditoService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.VentaService;

/** Verificación de F-08: registrar una venta a crédito y un abono parcial y validar el saldo pendiente. */
@SpringBootTest
@Transactional
class VentasCreditoTest {

    @Autowired
    ProductoService productos;
    @Autowired
    VentaService ventas;
    @Autowired
    CreditoService credito;

    @Test
    void ventaAFiadoYAbonoParcialActualizanElSaldoPendiente() {
        ProductoResponse aceite = aceite();

        VentaResponse venta = ventas.registrar(fiado("Doña Rosa", aceite, 1));
        assertThat(venta.tipoPago()).isEqualTo(TipoPago.CREDITO);
        assertThat(venta.clienteNombre()).isEqualTo("Doña Rosa");
        assertThat(saldoDe(venta.clienteId())).isEqualByComparingTo("10000");

        ClienteCreditoResponse despuesDelAbono = credito.registrarAbono(venta.clienteId(),
                new AbonoRequest(new BigDecimal("4000"), "Abono parcial"));

        assertThat(despuesDelAbono.totalFiado()).isEqualByComparingTo("10000");
        assertThat(despuesDelAbono.totalAbonado()).isEqualByComparingTo("4000");
        assertThat(despuesDelAbono.saldoPendiente()).isEqualByComparingTo("6000");

        ClienteDetalleResponse detalle = credito.detalle(venta.clienteId());
        assertThat(detalle.ventasACredito()).hasSize(1);
        assertThat(detalle.abonos()).hasSize(1);
        assertThat(detalle.abonos().get(0).nota()).isEqualTo("Abono parcial");
    }

    @Test
    void elSaldoSumaTodasLasVentasFiadasDelMismoClienteSinImportarMayusculas() {
        ProductoResponse aceite = aceite();

        ventas.registrar(fiado("Rosa", aceite, 1));
        ventas.registrar(fiado("ROSA", aceite, 2));

        assertThat(credito.listarClientes()).hasSize(1);
        assertThat(credito.listarClientes().get(0).saldoPendiente()).isEqualByComparingTo("30000");
    }

    @Test
    void unAbonoMayorAlSaldoSeRechazaYUnoQueSaldaDejaElSaldoEnCero() {
        ProductoResponse aceite = aceite();
        VentaResponse venta = ventas.registrar(fiado("Pedro", aceite, 1));

        assertThatThrownBy(() -> credito.registrarAbono(venta.clienteId(),
                new AbonoRequest(new BigDecimal("10001"), null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("supera el saldo pendiente");

        ClienteCreditoResponse saldado = credito.registrarAbono(venta.clienteId(),
                new AbonoRequest(new BigDecimal("10000"), null));
        assertThat(saldado.saldoPendiente()).isEqualByComparingTo("0");

        assertThatThrownBy(() -> credito.registrarAbono(venta.clienteId(),
                new AbonoRequest(new BigDecimal("1"), null)))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no tiene saldo pendiente");
    }

    @Test
    void ventaACreditoSinClienteSeRechazaYLasDeContadoNoGeneranDeuda() {
        ProductoResponse aceite = aceite();

        assertThatThrownBy(() -> ventas.registrar(new VentaRequest(TipoPago.CREDITO, " ", null,
                List.of(new LineaProductoRequest(aceite.id(), 1)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("necesita el nombre del cliente");

        VentaResponse contado = ventas.registrar(new VentaRequest(TipoPago.CONTADO, null, null,
                List.of(new LineaProductoRequest(aceite.id(), 1))));
        assertThat(contado.clienteId()).isNull();
        assertThat(credito.listarClientes()).isEmpty();
    }

    private VentaRequest fiado(String cliente, ProductoResponse producto, int cantidad) {
        return new VentaRequest(TipoPago.CREDITO, cliente, "3001112233",
                List.of(new LineaProductoRequest(producto.id(), cantidad)));
    }

    private ProductoResponse aceite() {
        return productos.crear(new ProductoRequest("Aceite", "Aceites", 1, 20, new BigDecimal("10000"),
                new BigDecimal("8000")));
    }

    private BigDecimal saldoDe(Long clienteId) {
        return credito.detalle(clienteId).cliente().saldoPendiente();
    }
}
