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

    /**
     * tipo es VENTA (aumenta la deuda), ABONO (la reduce) o ANULACION (devuelve a la deuda un abono registrado
     * por error). numero es el de la venta o el del abono. anulado marca el ABONO que después se anuló: sigue
     * en la libreta con su fecha, pero su valor vuelve a la deuda en la fila ANULACION.
     */
    public record Movimiento(
            String tipo,
            Long numero,
            LocalDateTime fechaHora,
            String descripcion,
            long cargo,
            long abono,
            long saldo,
            boolean anulado) {
    }
}
