package co.tiendabarrio.model;

/** Tipo de documento de identificación de quien pide factura electrónica. */
public enum TipoDocumento {
    CC("Cédula de ciudadanía"),
    NIT("NIT"),
    CE("Cédula de extranjería"),
    PASAPORTE("Pasaporte");

    private final String nombre;

    TipoDocumento(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
