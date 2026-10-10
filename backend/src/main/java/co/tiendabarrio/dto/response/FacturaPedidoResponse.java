package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Factura de compra de un pedido a proveedor ya recibido y pagado: lo pedido, lo recibido y lo cobrado
 * producto por producto, con el pago. Incluye el texto y los enlaces para enviarla (null si falta el contacto).
 */
public record FacturaPedidoResponse(
        Long pedidoId,
        Long ingresoId,
        String proveedorNombre,
        String proveedorContacto,
        String proveedorTelefono,
        String proveedorCorreo,
        String numeroFactura,
        LocalDateTime fechaPedido,
        LocalDateTime fechaRecepcion,
        LocalDateTime fechaPago,
        List<Linea> lineas,
        long subtotal,
        long totalCreditoDevoluciones,
        long totalPagado,
        String formaPagoNombre,
        Long montoEntregado,
        Long cambio,
        List<IngresoResponse.Entrega> entregasFaltantes,
        String texto,
        String whatsappUrl,
        String correoUrl) {

    /**
     * cantidadPedida es null si el producto llegó sin estar en el pedido. cantidadCobrada es la que se paga:
     * la facturada, o la recibida si la factura se ajustó a lo que llegó.
     */
    public record Linea(
            Long productoId,
            String productoNombre,
            Integer cantidadPedida,
            int cantidadFacturada,
            int cantidadRecibida,
            int cantidadCobrada,
            long costoUnitario,
            long subtotal) {
    }
}
