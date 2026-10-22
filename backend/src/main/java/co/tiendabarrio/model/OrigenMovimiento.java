package co.tiendabarrio.model;

/** Qué operación del negocio generó el movimiento. */
public enum OrigenMovimiento {
    INVENTARIO_INICIAL,
    VENTA,
    INGRESO_PROVEEDOR,
    /** Productos no conformes que salen del inventario para devolverlos al proveedor (F-07). */
    DEVOLUCION_PROVEEDOR,
    /** Productos nuevos que el proveedor entrega para reemplazar una devolución (F-07). */
    REEMPLAZO_DEVOLUCION
}
