package co.tiendabarrio.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.EntregaFaltantes;
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
        Long montoEntregado,
        Long cambio,
        boolean pagable,
        Long devolucionEnCursoId,
        long totalCreditoDevoluciones,
        List<Entrega> entregasFaltantes) {

    /**
     * cantidadRecibida es lo que llegó con el ingreso y cantidadEntregadaDespues lo que el proveedor entregó
     * después; diferencia = (recibida + entregada después) − facturada: negativa si faltan unidades.
     */
    public record Linea(
            Long productoId,
            String productoNombre,
            int cantidadFacturada,
            int cantidadRecibida,
            int cantidadEntregadaDespues,
            int diferencia,
            Long costoUnitario,
            long subtotalFacturado) {

        static Linea de(LineaProducto l) {
            return new Linea(l.getProducto().getId(), l.getProducto().getNombre(), l.getCantidadFacturada(),
                    l.getCantidad(), l.getCantidadEntregadaDespues(), l.getDiferencia(), l.getPrecioUnitario(), l.getSubtotalFacturado());
        }
    }

    /** Entrega posterior de faltantes, con las unidades de cada producto. */
    public record Entrega(Long id, LocalDateTime fechaHora, String nota, List<LineaProductoResponse> lineas) {

        public static Entrega de(EntregaFaltantes e) {
            return new Entrega(e.getId(), e.getFechaHora(), e.getNota(),
                    e.getLineas().stream().map(LineaProductoResponse::de).toList());
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
                i.getMontoEntregado(),
                i.getCambio(),
                i.isPagable(),
                i.getDevolucionEnCurso() == null ? null : i.getDevolucionEnCurso().getId(),
                i.getTotalCreditoDevoluciones(),
                i.getEntregasFaltantes().stream().map(Entrega::de).toList());
    }
}
