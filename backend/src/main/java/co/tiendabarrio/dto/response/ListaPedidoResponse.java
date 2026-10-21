package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.Producto;

/** Lista de pedido generada automáticamente con los productos que están en su stock mínimo o por debajo (SWR-08). */
public record ListaPedidoResponse(
        LocalDateTime generadaEn,
        int totalProductos,
        List<Grupo> proveedores,
        List<Item> sinProveedor) {

    /** Productos que se le deben pedir a un mismo proveedor, con sus datos de contacto. */
    public record Grupo(
            Long proveedorId,
            String proveedorNombre,
            String nombreContacto,
            String telefono,
            String correo,
            List<Item> productos) {
    }

    public record Item(Long productoId, String nombre, int stockActual, int stockMinimo, int cantidadSugerida) {

        /** Sugiere pedir lo necesario para duplicar el stock mínimo (y al menos una unidad). */
        public static Item de(Producto p) {
            int sugerida = Math.max(1, p.getStockMinimo() * 2 - p.getStockActual());
            return new Item(p.getId(), p.getNombre(), p.getStockActual(), p.getStockMinimo(), sugerida);
        }
    }
}
