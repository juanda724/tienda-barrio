package co.tiendabarrio.dto.response;

import java.math.BigDecimal;

/** Cliente con lo que se le ha fiado, lo que ha abonado y lo que aún debe (SWR-22). */
public record ClienteCreditoResponse(
        Long id,
        String nombre,
        String telefono,
        BigDecimal totalFiado,
        BigDecimal totalAbonado,
        BigDecimal saldoPendiente) {
}
