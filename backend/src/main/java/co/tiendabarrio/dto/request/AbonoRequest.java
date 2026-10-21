package co.tiendabarrio.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record AbonoRequest(
        @NotNull(message = "El valor del abono es obligatorio")
        @DecimalMin(value = "0.01", message = "El abono debe ser mayor que cero") BigDecimal monto,
        String nota) {
}
