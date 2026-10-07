package co.tiendabarrio.model;

import java.util.List;

/**
 * Seguimiento de una devolución hasta su resolución (SWR-20).
 *
 * <pre>
 * PENDIENTE → ENVIADA → REEMPLAZADA | CON_NOTA_CREDITO | RECHAZADA
 *     └──────────────→ REEMPLAZADA | CON_NOTA_CREDITO | RECHAZADA  (si el proveedor resuelve en el acto)
 * </pre>
 */
public enum EstadoDevolucion {
    PENDIENTE("Pendiente de envío"),
    ENVIADA("Enviada al proveedor"),
    /** El proveedor entregó productos nuevos: vuelven a entrar al inventario. */
    REEMPLAZADA("Resuelta con reemplazo"),
    /** El proveedor reconoció el valor: se descuenta del pago del ingreso. */
    CON_NOTA_CREDITO("Resuelta con nota crédito"),
    /** El proveedor no aceptó la devolución: la tienda asume la pérdida. */
    RECHAZADA("Rechazada por el proveedor");

    private final String nombre;

    EstadoDevolucion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }

    public List<EstadoDevolucion> siguientes() {
        return switch (this) {
            case PENDIENTE -> List.of(ENVIADA, REEMPLAZADA, CON_NOTA_CREDITO, RECHAZADA);
            case ENVIADA -> List.of(REEMPLAZADA, CON_NOTA_CREDITO, RECHAZADA);
            case REEMPLAZADA, CON_NOTA_CREDITO, RECHAZADA -> List.of();
        };
    }

    /** Mientras la devolución está en curso, el pago del ingreso queda bloqueado (RN-01). */
    public boolean enCurso() {
        return this == PENDIENTE || this == ENVIADA;
    }
}
