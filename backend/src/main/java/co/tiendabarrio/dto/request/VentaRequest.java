package co.tiendabarrio.dto.request;

import java.util.List;

import co.tiendabarrio.model.FormaPago;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * El precio no se envía: el sistema lo toma de cada producto y calcula el total (SWR-12).
 * clienteId es obligatorio en las ventas a crédito (fiado) y se ignora en las demás.
 */
public record VentaRequest(
        @NotEmpty(message = "La venta debe tener al menos un producto")
        List<@Valid LineaProductoRequest> lineas,
        @NotNull(message = "Debe indicar la forma de pago") FormaPago formaPago,
        @Min(value = 0, message = "El monto recibido no puede ser negativo") Long montoRecibido,
        Long clienteId) {
}
