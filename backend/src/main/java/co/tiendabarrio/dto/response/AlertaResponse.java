package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;

import co.tiendabarrio.model.AlertaStock;

/** Alerta de stock mínimo con el stock actual del producto (puede haber bajado más desde que se creó). */
public record AlertaResponse(
        Long id,
        Long productoId,
        String productoNombre,
        int stockActual,
        int stockMinimo,
        LocalDateTime creada,
        boolean vista) {

    public static AlertaResponse de(AlertaStock a) {
        return new AlertaResponse(a.getId(), a.getProducto().getId(), a.getProducto().getNombre(),
                a.getProducto().getStockActual(), a.getProducto().getStockMinimo(), a.getCreada(), a.isVista());
    }
}
