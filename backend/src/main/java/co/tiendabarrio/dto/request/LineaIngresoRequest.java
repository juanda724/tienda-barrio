package co.tiendabarrio.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Producto de un ingreso de mercancía: lo que llegó, lo que cobra la factura y su costo unitario
 * (SWR-15). Al inventario solo entra lo recibido.
 */
public record LineaIngresoRequest(
        @NotNull(message = "Debe seleccionar un producto en cada línea") Long productoId,
        @NotNull(message = "La cantidad recibida es obligatoria")
        @Min(value = 0, message = "La cantidad recibida no puede ser negativa") Integer cantidadRecibida,
        @NotNull(message = "La cantidad facturada es obligatoria")
        @Min(value = 0, message = "La cantidad facturada no puede ser negativa") Integer cantidadFacturada,
        @NotNull(message = "El costo unitario es obligatorio")
        @Min(value = 0, message = "El costo unitario no puede ser negativo") Long costoUnitario) {
}
