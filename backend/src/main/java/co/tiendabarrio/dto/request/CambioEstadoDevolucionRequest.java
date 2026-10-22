package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.EstadoDevolucion;
import jakarta.validation.constraints.NotNull;

public record CambioEstadoDevolucionRequest(
        @NotNull(message = "Debe indicar el nuevo estado") EstadoDevolucion estado,
        String nota) {
}
