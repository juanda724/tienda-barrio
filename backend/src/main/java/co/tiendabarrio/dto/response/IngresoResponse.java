package co.tiendabarrio.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.LineaProducto;

/**
 * Ingreso con su verificación factura vs. recibido (SWR-15, SWR-17), el estado del pago al proveedor
 * y las devoluciones que lo afectan (F-07).
 */
public record IngresoResponse(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        Long pedidoId,
        String numeroFactura,
        LocalDateTime fechaHora,
        List<Linea> lineas,
        String resultadoVerificacion,
        String resultadoVerificacionNombre,
        String notaVerificacion,
        long totalFacturado,
        long totalRecibido,
        long totalAPagar,
        String estadoPago,
        String estadoPagoNombre,
        LocalDate fechaVencimiento,
        boolean vencido,
        LocalDateTime fechaPago,
        String formaPagoPagoNombre,
        Long montoPagado,
        boolean pagable,
        Long devolucionEnCursoId,
        long totalCreditoDevoluciones) {

    /** diferencia = recibida − facturada: negativa si faltaron unidades. */
    public record Linea(
            Long productoId,
            String productoNombre,
            int cantidadFacturada,
            int cantidadRecibida,
            int diferencia,
            Long costoUnitario,
            long subtotalFacturado) {

        static Linea de(LineaProducto l) {
            return new Linea(l.getProducto().getId(), l.getProducto().getNombre(), l.getCantidadFacturada(),
                    l.getCantidad(), l.getDiferencia(), l.getPrecioUnitario(), l.getSubtotalFacturado());
        }
    }

    public static IngresoResponse de(IngresoMercancia i) {
        return new IngresoResponse(
                i.getId(),
                i.getProveedor().getId(),
                i.getProveedor().getNombre(),
                i.getPedido() == null ? null : i.getPedido().getId(),
                i.getNumeroFactura(),
                i.getFechaHora(),
                i.getLineas().stream().map(Linea::de).toList(),
                i.getResultadoVerificacion() == null ? null : i.getResultadoVerificacion().name(),
                i.getResultadoVerificacion() == null ? "Sin verificar" : i.getResultadoVerificacion().getNombre(),
                i.getNotaVerificacion(),
                i.getTotalFacturado(),
                i.getTotalRecibido(),
                i.getTotalAPagar(),
                i.getEstadoPago().name(),
                i.isVencido() ? "Crédito vencido" : i.getEstadoPago().getNombre(),
                i.getFechaVencimiento(),
                i.isVencido(),
                i.getFechaPago(),
                i.getFormaPagoPago() == null ? null : i.getFormaPagoPago().getNombre(),
                i.getMontoPagado(),
                i.isPagable(),
                i.getDevolucionEnCurso() == null ? null : i.getDevolucionEnCurso().getId(),
                i.getTotalCreditoDevoluciones());
    }
}
