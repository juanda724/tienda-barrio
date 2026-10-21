package co.tiendabarrio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.TipoPago;
import co.tiendabarrio.model.Venta;

public record VentaResponse(
        Long id,
        String numero,
        LocalDateTime fechaHora,
        TipoPago tipoPago,
        Long clienteId,
        String clienteNombre,
        List<LineaVentaResponse> lineas,
        BigDecimal total,
        BigDecimal ganancia) {

    public static VentaResponse de(Venta v) {
        return new VentaResponse(v.getId(), v.getNumero(), v.getFechaHora(), v.getTipoPago(),
                v.getCliente() == null ? null : v.getCliente().getId(),
                v.getCliente() == null ? null : v.getCliente().getNombre(),
                v.getLineas().stream().map(LineaVentaResponse::de).toList(), v.getTotal(), v.getGanancia());
    }
}
