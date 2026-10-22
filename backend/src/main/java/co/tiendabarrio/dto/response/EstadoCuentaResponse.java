package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

/**
 * "Libreta" del cliente: ventas fiadas y abonos en orden de fecha, con el saldo después de cada
 * uno, y el recordatorio de saldo para enviarle por WhatsApp (null si no tiene teléfono).
 */
public record EstadoCuentaResponse(
        ClienteResponse cliente,
        List<Movimiento> movimientos,
        String recordatorio,
        String whatsappUrl) {

    /** tipo es VENTA (aumenta la deuda) o ABONO (la reduce); numero es el de la venta o el abono. */
    public record Movimiento(
            String tipo,
            Long numero,
            LocalDateTime fechaHora,
            String descripcion,
            long cargo,
            long abono,
            long saldo) {
    }
}
