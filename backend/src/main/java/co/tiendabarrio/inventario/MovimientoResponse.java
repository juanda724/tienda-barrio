package co.tiendabarrio.inventario;

import java.time.LocalDateTime;

public record MovimientoResponse(
        Long id,
        Long productoId,
        String productoNombre,
        TipoMovimiento tipo,
        OrigenMovimiento origen,
        int cantidad,
        int stockResultante,
        LocalDateTime fechaHora,
        String referencia) {

    public static MovimientoResponse de(MovimientoInventario m) {
        return new MovimientoResponse(m.getId(), m.getProducto().getId(), m.getProducto().getNombre(),
                m.getTipo(), m.getOrigen(), m.getCantidad(), m.getStockResultante(), m.getFechaHora(),
                m.getReferencia());
    }
}
