package co.tiendabarrio.dto.response;

import co.tiendabarrio.model.Producto;

public record ProductoResponse(
        Long id,
        String nombre,
        String categoria,
        int stockActual,
        int stockMinimo,
        boolean bajoMinimo) {

    public static ProductoResponse de(Producto p) {
        return new ProductoResponse(p.getId(), p.getNombre(), p.getCategoria(), p.getStockActual(),
                p.getStockMinimo(), p.isBajoMinimo());
    }
}
