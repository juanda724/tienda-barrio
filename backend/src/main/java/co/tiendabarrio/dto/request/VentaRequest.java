package co.tiendabarrio.dto.request;

import java.util.List;

import co.tiendabarrio.model.TipoPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Una venta de contado o a crédito. El cliente solo es obligatorio cuando la venta es a crédito. */
public record VentaRequest(
        @NotNull(message = "Seleccione la forma de pago") TipoPago tipoPago,
        String clienteNombre,
        String clienteTelefono,
        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid LineaProductoRequest> lineas) {
}
