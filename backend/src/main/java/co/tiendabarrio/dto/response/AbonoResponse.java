package co.tiendabarrio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import co.tiendabarrio.model.Abono;

public record AbonoResponse(Long id, BigDecimal monto, LocalDateTime fechaHora, String nota) {

    public static AbonoResponse de(Abono a) {
        return new AbonoResponse(a.getId(), a.getMonto(), a.getFechaHora(), a.getNota());
    }
}
