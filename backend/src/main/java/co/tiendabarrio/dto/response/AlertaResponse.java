package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;

import co.tiendabarrio.model.Alerta;
import co.tiendabarrio.model.EstadoAlerta;

public record AlertaResponse(
        Long id,
        Long productoId,
        String productoNombre,
        int stockActual,
        int stockMinimo,
        int stockAlGenerar,
        LocalDateTime fechaHora,
        EstadoAlerta estado,
        LocalDateTime fechaResolucion) {

    public static AlertaResponse de(Alerta a) {
        return new AlertaResponse(a.getId(), a.getProducto().getId(), a.getProducto().getNombre(),
                a.getProducto().getStockActual(), a.getStockMinimo(), a.getStockAlGenerar(), a.getFechaHora(),
                a.getEstado(), a.getFechaResolucion());
    }
}
