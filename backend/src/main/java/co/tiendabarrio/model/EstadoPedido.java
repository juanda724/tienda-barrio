package co.tiendabarrio.model;

import java.util.List;

/**
 * Estados de un pedido a proveedor y los cambios que el dueño puede hacer a mano.
 * ENTREGADO no se marca a mano: lo asigna el ingreso de mercancía del pedido.
 *
 * <pre>
 * EN_ESPERA → ACEPTADO → EN_PROCESO → EN_CAMINO → ENTREGADO
 *     ↓           ↓           ↓
 * RECHAZADO   CANCELADO   CANCELADO     (EN_ESPERA también puede cancelarse)
 * </pre>
 */
public enum EstadoPedido {
    EN_ESPERA("En espera"),
    ACEPTADO("Aceptado"),
    EN_PROCESO("En proceso"),
    EN_CAMINO("En camino"),
    ENTREGADO("Entregado"),
    RECHAZADO("Rechazado"),
    CANCELADO("Cancelado");

    private final String nombre;

    EstadoPedido(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    /** Estados a los que el dueño puede pasar manualmente desde este. */
    public List<EstadoPedido> siguientesManuales() {
        return switch (this) {
            case EN_ESPERA -> List.of(ACEPTADO, RECHAZADO, CANCELADO);
            case ACEPTADO -> List.of(EN_PROCESO, CANCELADO);
            case EN_PROCESO -> List.of(EN_CAMINO, CANCELADO);
            case EN_CAMINO, ENTREGADO, RECHAZADO, CANCELADO -> List.of();
        };
    }

    /** Un pedido activo todavía puede recibirse con un ingreso de mercancía. */
    public boolean esActivo() {
        return this == EN_ESPERA || this == ACEPTADO || this == EN_PROCESO || this == EN_CAMINO;
    }
}
