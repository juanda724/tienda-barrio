package co.tiendabarrio.dto.response;

import java.math.BigDecimal;

import co.tiendabarrio.model.LineaVenta;

public record LineaVentaResponse(
        Long productoId,
        String productoNombre,
        int cantidad,
        BigDecimal precioUnitario,
        BigDecimal subtotal) {

    public static LineaVentaResponse de(LineaVenta l) {
        return new LineaVentaResponse(l.getProducto().getId(), l.getProducto().getNombre(), l.getCantidad(),
                l.getPrecioUnitario(), l.getSubtotal());
    }
}
