package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;

import co.tiendabarrio.model.CambioEstadoPedido;

public record CambioEstadoResponse(EstadoResponse estado, LocalDateTime fechaHora, String nota) {

    public static CambioEstadoResponse de(CambioEstadoPedido cambio) {
        return new CambioEstadoResponse(EstadoResponse.de(cambio.getEstado()), cambio.getFechaHora(), cambio.getNota());
    }
}
