package co.tiendabarrio.dto.response;

/** Comprobante de venta en texto y el enlace para enviarlo al cliente por WhatsApp. */
public record ComprobanteResponse(Long ventaId, String texto, String whatsappUrl) {
}
