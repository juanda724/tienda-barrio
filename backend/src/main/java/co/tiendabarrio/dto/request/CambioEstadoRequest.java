package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.EstadoDevolucion;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoRequest(
        @NotNull(message = "Seleccione el nuevo estado") EstadoDevolucion estado,
        String nota) {
}
