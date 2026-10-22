package co.tiendabarrio.model;

/** Situación del pago de un ingreso de mercancía al proveedor. */
public enum EstadoPago {
    PENDIENTE("Pendiente de pago"),
    /** Se acordó pagar más adelante, con fecha de vencimiento. */
    CREDITO("A crédito"),
    PAGADO("Pagado");

    private final String nombre;

    EstadoPago(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
