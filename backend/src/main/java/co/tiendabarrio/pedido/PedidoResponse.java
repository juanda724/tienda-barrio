package co.tiendabarrio.pedido;

import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.inventario.LineaProductoResponse;

public record PedidoResponse(Long id, LocalDateTime fechaHora, List<LineaProductoResponse> lineas) {

    public static PedidoResponse de(Pedido pedido) {
        return new PedidoResponse(pedido.getId(), pedido.getFechaHora(),
                pedido.getLineas().stream().map(LineaProductoResponse::de).toList());
    }
}
