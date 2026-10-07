package co.tiendabarrio.model;

/** Resultado de confrontar la factura del proveedor con lo recibido (SWR-17). */
public enum ResultadoVerificacion {
    /** Lo facturado coincide con lo recibido en todos los productos. */
    APROBADO("Aprobado"),
    /** Algún producto no coincide; el pago queda bloqueado hasta resolverlo (SWR-16). */
    CON_DIFERENCIAS("Con diferencias"),
    /** El proveedor ajustó la factura a lo recibido; ya se puede pagar. */
    DIFERENCIAS_RESUELTAS("Diferencias resueltas");

    private final String nombre;

    ResultadoVerificacion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
