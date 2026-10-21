package co.tiendabarrio.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Cantidad que el proveedor puso en la factura para un producto (puede ser 0 si no llegó nada). */
public record LineaFacturadaRequest(
        @NotNull(message = "Debe seleccionar un producto en cada línea") Long productoId,
        @NotNull(message = "La cantidad facturada es obligatoria")
        @Min(value = 0, message = "La cantidad facturada no puede ser negativa") Integer cantidadFacturada) {
}
