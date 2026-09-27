package co.tiendabarrio.model;

/** Qué operación del negocio generó el movimiento. */
public enum OrigenMovimiento {
    INVENTARIO_INICIAL,
    COMPRA,
    PEDIDO,
    INGRESO_PROVEEDOR
}
