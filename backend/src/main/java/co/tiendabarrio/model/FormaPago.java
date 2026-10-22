package co.tiendabarrio.model;

/** Cómo pagó el cliente una venta, o cómo hizo un abono a su deuda. */
public enum FormaPago {
    EFECTIVO("Efectivo"),
    TRANSFERENCIA("Transferencia"),
    TARJETA("Tarjeta / datáfono"),
    /** Venta a crédito ("fiado"): queda como deuda del cliente hasta que la abone (F-08). */
    CREDITO("Crédito (fiado)");

    private final String nombre;

    FormaPago(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
