package co.tiendabarrio.model;

/**
 * Tratamiento de un producto frente al IVA (SWR-33). La ley define cuál le corresponde a cada bien; la tienda
 * solo lo clasifica. El precio de venta ya incluye el IVA.
 */
public enum TratamientoIva {
    /** No causa IVA. */
    EXCLUIDO("Excluido", "Excluido", 0),
    /** Causa IVA a tarifa cero. En la factura se distingue del excluido aunque ambos den $ 0 de IVA. */
    EXENTO("Exento (0 %)", "Exento", 0),
    TARIFA_5("5 %", "IVA 5 %", 5),
    /** Tarifa general: la de todo lo que la ley no clasifica en otro tratamiento. */
    TARIFA_19("19 %", "IVA 19 %", 19);

    private final String nombre;
    private final String etiqueta;
    private final int porcentaje;

    TratamientoIva(String nombre, String etiqueta, int porcentaje) {
        this.nombre = nombre;
        this.etiqueta = etiqueta;
        this.porcentaje = porcentaje;
    }

    public String getNombre() {
        return nombre;
    }

    /** Como se anota junto a cada producto de la factura: "IVA 19 %", "Exento", "Excluido". */
    public String getEtiqueta() {
        return etiqueta;
    }

    public int getPorcentaje() {
        return porcentaje;
    }

    /**
     * IVA contenido en un valor que ya lo incluye, redondeado al peso: valor × tarifa / (100 + tarifa).
     * En $ 3.000 al 19 % hay $ 479 de IVA.
     */
    public long ivaIncluidoEn(long valorConIva) {
        long divisor = 100 + porcentaje;
        return (2 * valorConIva * porcentaje + divisor) / (2 * divisor);
    }
}
