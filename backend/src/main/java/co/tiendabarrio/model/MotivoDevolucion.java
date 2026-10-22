package co.tiendabarrio.model;

/** Por qué se devuelve un producto al proveedor. */
public enum MotivoDevolucion {
    MAL_ESTADO("En mal estado o empaque roto"),
    VENCIDO("Vencido o próximo a vencer"),
    EQUIVOCADO("Producto equivocado o no pedido"),
    OTRO("Otro motivo");

    private final String nombre;

    MotivoDevolucion(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
