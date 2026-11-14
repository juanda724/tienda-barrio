package co.tiendabarrio.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lo vendido separado en base gravable e IVA por cada tratamiento, como lo discrimina la factura de un
 * responsable de IVA (SWR-33). El IVA se calcula sobre el total de cada tratamiento, no línea por línea,
 * para que base más IVA dé exactamente lo cobrado.
 */
public final class DesgloseIva {

    /** Base e IVA de los productos de un mismo tratamiento; base + iva es lo cobrado por ellos. */
    public record Grupo(TratamientoIva tratamiento, long base, long iva) {
    }

    private final List<Grupo> grupos;

    private DesgloseIva(List<Grupo> grupos) {
        this.grupos = grupos;
    }

    /**
     * Desglose de las líneas de una venta. Las líneas sin tratamiento de IVA no entran: son de ventas en las
     * que la tienda no discriminó el impuesto.
     */
    public static DesgloseIva de(List<LineaProducto> lineas) {
        Map<TratamientoIva, Long> cobradoPorTratamiento = new EnumMap<>(TratamientoIva.class);
        for (LineaProducto linea : lineas) {
            if (linea.getTratamientoIva() != null) {
                cobradoPorTratamiento.merge(linea.getTratamientoIva(), linea.getSubtotal(), Long::sum);
            }
        }
        return new DesgloseIva(cobradoPorTratamiento.entrySet().stream()
                .map(e -> {
                    long iva = e.getKey().ivaIncluidoEn(e.getValue());
                    return new Grupo(e.getKey(), e.getValue() - iva, iva);
                })
                .toList());
    }

    /** Un grupo por tratamiento vendido, de excluido a tarifa general. */
    public List<Grupo> grupos() {
        return grupos;
    }

    /** true si la venta no discrimina IVA: ninguna de sus líneas tiene tratamiento. */
    public boolean vacio() {
        return grupos.isEmpty();
    }

    public long totalBase() {
        return grupos.stream().mapToLong(Grupo::base).sum();
    }

    public long totalIva() {
        return grupos.stream().mapToLong(Grupo::iva).sum();
    }
}
