package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.IngresoMercancia;

public record IngresoResponse(
        Long id,
        Long proveedorId,
        String proveedorNombre,
        String numeroFactura,
        LocalDateTime fechaHora,
        List<LineaProductoResponse> lineas) {

    public static IngresoResponse de(IngresoMercancia ingreso) {
        return new IngresoResponse(ingreso.getId(), ingreso.getProveedor().getId(),
                ingreso.getProveedor().getNombre(), ingreso.getNumeroFactura(), ingreso.getFechaHora(),
                ingreso.getLineas().stream().map(LineaProductoResponse::de).toList());
    }
}
