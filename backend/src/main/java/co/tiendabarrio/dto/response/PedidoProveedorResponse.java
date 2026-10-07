package co.tiendabarrio.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.PedidoProveedor;

/**
 * Pedido a proveedor con todo lo que la pantalla necesita: su estado, los cambios de estado
 * permitidos y los enlaces para enviarlo por WhatsApp o correo (null si falta el dato de contacto).
 */
public record PedidoProveedorResponse(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        EstadoResponse estado,
        LocalDateTime fechaCreacion,
        LocalDate fechaEstimadaEntrega,
        String observaciones,
        List<LineaProductoResponse> lineas,
        List<CambioEstadoResponse> historial,
        List<EstadoResponse> siguientesEstados,
        boolean puedeRecibirse,
        String mensaje,
        String whatsappUrl,
        String correoUrl) {

    public static PedidoProveedorResponse de(PedidoProveedor pedido, String mensaje, String whatsappUrl,
                                             String correoUrl) {
        return new PedidoProveedorResponse(
                pedido.getId(),
                pedido.getProveedor().getId(),
                pedido.getProveedor().getNombre(),
                EstadoResponse.de(pedido.getEstado()),
                pedido.getFechaCreacion(),
                pedido.getFechaEstimadaEntrega(),
                pedido.getObservaciones(),
                pedido.getLineas().stream().map(LineaProductoResponse::de).toList(),
                pedido.getHistorial().stream().map(CambioEstadoResponse::de).toList(),
                pedido.getEstado().siguientesManuales().stream().map(EstadoResponse::de).toList(),
                pedido.getEstado().esActivo(),
                mensaje,
                whatsappUrl,
                correoUrl);
    }
}
