package co.tiendabarrio.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Ventas por producto en un rango de fechas, de la más vendida a la menos, con un resumen del
 * período. La ganancia es estimada: total vendido menos unidades × costo actual del producto.
 */
public record ReporteVentasResponse(
        LocalDateTime generado,
        LocalDate desde,
        LocalDate hasta,
        List<Fila> filas,
        Resumen resumen) {

    public record Fila(Long productoId, String nombre, int unidades, long total, long gananciaEstimada) {
    }

    public record PorFormaPago(String formaPago, String nombre, int ventas, long total) {
    }

    public record Resumen(int ventas, int unidades, long total, long gananciaEstimada, long ticketPromedio,
                          List<PorFormaPago> porFormaPago) {
    }
}
