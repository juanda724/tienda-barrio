package co.tiendabarrio.dto.response;

/**
 * Comprobante de venta en texto y el enlace para enviarlo al cliente por WhatsApp. Si se pidió factura
 * electrónica, correoUrl abre Gmail para enviarla al correo del cliente (null en las demás ventas).
 */
public record ComprobanteResponse(Long ventaId, String texto, String whatsappUrl, String correoUrl) {
}
