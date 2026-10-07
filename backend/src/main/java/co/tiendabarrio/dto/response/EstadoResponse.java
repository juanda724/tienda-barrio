package co.tiendabarrio.dto.response;

import co.tiendabarrio.model.EstadoPedido;

/** Estado de un pedido con su nombre para mostrar (p. ej. EN_CAMINO → "En camino"). */
public record EstadoResponse(EstadoPedido estado, String nombre) {

    public static EstadoResponse de(EstadoPedido estado) {
        return new EstadoResponse(estado, estado.getNombre());
    }
}
