package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Factura electrónica de una venta (SWR-32) con lo que exige el documento: emisor, adquirente, numeración
 * autorizada, detalle, pago, CUFE y código QR. Incluye el texto y los enlaces para enviarla.
 * resolucion, cufe y qr son null en las facturas anteriores a la numeración por resolución.
 */
public record FacturaElectronicaResponse(
        Long ventaId,
        String numero,
        LocalDateTime fechaHora,
        Emisor emisor,
        String resolucion,
        Adquiriente adquiriente,
        List<Linea> lineas,
        long subtotal,
        long descuentos,
        long total,
        /** "Contado" o "Crédito". */
        String formaPago,
        /** Efectivo, transferencia o tarjeta; null en las ventas a crédito. */
        String medioPago,
        Long montoRecibido,
        Long cambio,
        /** Cliente al que se le fió y su saldo actual; null en las ventas de contado. */
        String clienteFiado,
        Long saldoCliente,
        String cufe,
        /** Imagen del código QR como data URI. */
        String qr,
        String aviso,
        String texto,
        String whatsappUrl,
        /** Enlace de Gmail al correo del cliente; null si es consumidor final. */
        String correoUrl) {

    /** La misma factura con el texto para enviarla y sus enlaces de WhatsApp y Gmail. */
    public FacturaElectronicaResponse conEnvio(String texto, String whatsappUrl, String correoUrl) {
        return new FacturaElectronicaResponse(ventaId, numero, fechaHora, emisor, resolucion, adquiriente, lineas,
                subtotal, descuentos, total, formaPago, medioPago, montoRecibido, cambio, clienteFiado, saldoCliente,
                cufe, qr, aviso, texto, whatsappUrl, correoUrl);
    }

    /** direccion y telefono son null si no se configuraron. */
    public record Emisor(String nombre, String razonSocial, String nit, String regimen, String direccion,
                         String telefono) {
    }

    /** A un consumidor final solo se le conocen el nombre genérico y el NIT 222222222222. */
    public record Adquiriente(String nombre, String tipoDocumento, String numeroDocumento, String correo,
                              String telefono, String direccion) {
    }

    public record Linea(String codigo, String descripcion, int cantidad, String unidad, Long precioUnitario,
                        long subtotal) {
    }
}
