package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reporte de inventario con la cantidad disponible de cada producto (SWR-09) y su valor, tomado
 * del estado actual de la base de datos en el momento "generado".
 */
public record ReporteInventarioResponse(
        LocalDateTime generado,
        String categoria,
        List<Fila> filas,
        Totales totales,
        List<String> categorias) {

    /** valorCosto = stock × costo; valorVenta = stock × precio de venta (0 si falta el dato). */
    public record Fila(
            Long productoId,
            String nombre,
            String categoria,
            int stockActual,
            int stockMinimo,
            boolean bajoMinimo,
            Long costo,
            Long precioVenta,
            long valorCosto,
            long valorVenta) {
    }

    public record Totales(int productos, int unidades, int bajoMinimo, long valorCosto, long valorVenta) {
    }
}
