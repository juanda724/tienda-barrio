package co.tiendabarrio.model;

/** Ciclo de vida de una devolución al proveedor: se registra, se reclama y se resuelve (F-07). */
public enum EstadoDevolucion {
    REGISTRADA,
    RECLAMADA,
    REEMPLAZADA,
    ACREDITADA;

    public boolean esFinal() {
        return this == REEMPLAZADA || this == ACREDITADA;
    }
}
