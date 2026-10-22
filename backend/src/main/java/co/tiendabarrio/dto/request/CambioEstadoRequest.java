package co.tiendabarrio.dto.request;

import java.time.LocalDate;

import co.tiendabarrio.model.EstadoPedido;
import jakarta.validation.constraints.NotNull;

/** Cambio manual del estado de un pedido; opcionalmente actualiza la fecha estimada de entrega. */
public record CambioEstadoRequest(
        @NotNull(message = "Debe indicar el nuevo estado") EstadoPedido estado,
        LocalDate fechaEstimadaEntrega,
        String nota) {
}
