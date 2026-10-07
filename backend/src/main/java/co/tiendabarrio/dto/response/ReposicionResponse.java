package co.tiendabarrio.dto.response;

import java.util.List;

/**
 * Lista de pedido generada automáticamente con los productos en o por debajo del stock mínimo
 * (SWR-08), agrupada por el proveedor al que conviene pedírselos.
 */
public record ReposicionResponse(List<Grupo> grupos, List<Producto> sinProveedor) {

    public record Grupo(Long proveedorId, String proveedorNombre, List<Producto> productos) {
    }

    /**
     * cantidadSugerida lleva el stock al doble del mínimo. pedidoPendienteId es el número de un pedido
     * activo que ya incluye el producto (null si no hay), para no pedirlo dos veces.
     */
    public record Producto(
            Long productoId,
            String nombre,
            int stockActual,
            int stockMinimo,
            int cantidadSugerida,
            Long pedidoPendienteId) {
    }
}
