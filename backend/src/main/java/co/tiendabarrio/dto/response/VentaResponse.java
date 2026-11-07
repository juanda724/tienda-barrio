package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.Venta;

public record VentaResponse(
        Long id,
        LocalDateTime fechaHora,
        String formaPago,
        String formaPagoNombre,
        Long total,
        Long montoRecibido,
        Long cambio,
        Long clienteId,
        String clienteNombre,
        List<Linea> lineas,
        String numeroFacturaElectronica,
        AdquirienteResponse facturaElectronica) {

    /** Línea de venta con el precio cobrado y su subtotal. */
    public record Linea(Long productoId, String productoNombre, int cantidad, Long precioUnitario, long subtotal) {

        static Linea de(LineaProducto l) {
            return new Linea(l.getProducto().getId(), l.getProducto().getNombre(), l.getCantidad(),
                    l.getPrecioUnitario(), l.getSubtotal());
        }
    }

    public static VentaResponse de(Venta venta) {
        return new VentaResponse(
                venta.getId(),
                venta.getFechaHora(),
                venta.getFormaPago() == null ? null : venta.getFormaPago().name(),
                venta.getFormaPago() == null ? null : venta.getFormaPago().getNombre(),
                venta.getTotal(),
                venta.getMontoRecibido(),
                venta.getCambio(),
                venta.getCliente() == null ? null : venta.getCliente().getId(),
                venta.getCliente() == null ? null : venta.getCliente().getNombre(),
                venta.getLineas().stream().map(Linea::de).toList(),
                venta.getNumeroFacturaElectronica(),
                venta.getAdquiriente() == null ? null : AdquirienteResponse.de(venta.getAdquiriente()));
    }
}
