package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.MotivoDevolucion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record LineaDevolucionRequest(
        @NotNull(message = "Debe seleccionar un producto en cada línea") Long productoId,
        @NotNull(message = "La cantidad a devolver es obligatoria")
        @Min(value = 1, message = "La cantidad a devolver debe ser mayor que cero") Integer cantidad,
        @NotNull(message = "Indique el motivo de la devolución") MotivoDevolucion motivo) {
}
