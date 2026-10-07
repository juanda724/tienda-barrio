package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.FormaPago;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Pago parcial o total de la deuda de un cliente (SWR-23). Monto en pesos. */
public record AbonoRequest(
        @NotNull(message = "El monto del abono es obligatorio")
        @Min(value = 1, message = "El abono debe ser mayor que cero") Long monto,
        @NotNull(message = "Debe indicar cómo pagó el cliente") FormaPago formaPago,
        String nota) {
}
