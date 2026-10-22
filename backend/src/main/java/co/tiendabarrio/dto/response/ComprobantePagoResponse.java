package co.tiendabarrio.dto.response;

/** Comprobante del pago de un ingreso al proveedor, con enlaces para enviárselo (null si falta el contacto). */
public record ComprobantePagoResponse(Long ingresoId, String texto, String whatsappUrl, String correoUrl) {
}
