package co.tiendabarrio.inventario;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** Una línea (producto + cantidad) de un pedido o de un ingreso de mercancía. */
public record LineaProductoRequest(
        @NotNull(message = "Debe seleccionar un producto en cada línea") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero") Integer cantidad) {

    /** Suma las cantidades de las líneas que repiten el mismo producto, conservando el orden. */
    public static Map<Long, Integer> agrupar(List<LineaProductoRequest> lineas) {
        Map<Long, Integer> porProducto = new LinkedHashMap<>();
        for (LineaProductoRequest linea : lineas) {
            porProducto.merge(linea.productoId(), linea.cantidad(), Integer::sum);
        }
        return porProducto;
    }
}
