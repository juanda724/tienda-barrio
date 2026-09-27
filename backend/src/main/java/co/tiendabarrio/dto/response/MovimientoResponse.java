package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;

import co.tiendabarrio.model.MovimientoInventario;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.TipoMovimiento;

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
