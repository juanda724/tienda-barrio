package co.tiendabarrio.model;

/** Tipo de documento de identificación de quien pide factura electrónica. */
public enum TipoDocumento {
    CC("Cédula de ciudadanía"),
    NIT("NIT"),
    CE("Cédula de extranjería"),
    PASAPORTE("Pasaporte"),
    /**
     * Cliente que no se identifica: la factura sale a nombre de "Consumidor final" con el NIT genérico y no
     * se guardan datos suyos.
     */
    CONSUMIDOR_FINAL("Consumidor final");

    /** NIT genérico con el que se factura a un consumidor final. */
    public static final String NIT_CONSUMIDOR_FINAL = "222222222222";

    private final String nombre;

    TipoDocumento(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
