package co.tiendabarrio.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.Producto;

/** Reporte con la cantidad disponible de cada producto (F-04, SWR-09). */
public record ReporteInventarioResponse(
        LocalDateTime generadoEn,
        int totalProductos,
        int totalUnidades,
        int productosBajoMinimo,
        BigDecimal valorEnCosto,
        BigDecimal valorEnVenta,
        List<Fila> filas) {

    public record Fila(
            Long productoId,
            String nombre,
            String categoria,
            int stockActual,
            int stockMinimo,
            boolean bajoMinimo,
            int totalEntradas,
            int totalSalidas,
            BigDecimal costoUnitario,
            BigDecimal precioVenta,
            BigDecimal valorEnCosto) {

        public static Fila de(Producto p, int entradas, int salidas) {
            return new Fila(p.getId(), p.getNombre(), p.getCategoria(), p.getStockActual(), p.getStockMinimo(),
                    p.isBajoMinimo(), entradas, salidas, p.getCosto(), p.getPrecioVenta(),
                    p.getCosto().multiply(BigDecimal.valueOf(p.getStockActual())));
        }
    }
}
