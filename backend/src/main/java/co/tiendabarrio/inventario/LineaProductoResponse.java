package co.tiendabarrio.inventario;

public record LineaProductoResponse(Long productoId, String productoNombre, int cantidad) {

    public static LineaProductoResponse de(LineaProducto linea) {
        return new LineaProductoResponse(linea.getProducto().getId(), linea.getProducto().getNombre(),
                linea.getCantidad());
    }
}
