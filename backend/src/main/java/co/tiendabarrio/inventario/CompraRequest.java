package co.tiendabarrio.inventario;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CompraRequest(
        @NotNull(message = "Debe seleccionar un producto") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero") Integer cantidad,
        String referencia) {
}
