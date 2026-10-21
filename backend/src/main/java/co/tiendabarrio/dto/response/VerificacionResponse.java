package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.EstadoVerificacion;
import co.tiendabarrio.model.LineaVerificacion;
import co.tiendabarrio.model.VerificacionIngreso;

public record VerificacionResponse(
        Long id,
        Long ingresoId,
        LocalDateTime fechaHora,
        EstadoVerificacion resultado,
        String observacion,
        List<Linea> lineas) {

    /** diferencia = facturado − recibido: positiva si el proveedor cobró más de lo que entregó. */
    public record Linea(Long productoId, String productoNombre, int cantidadFacturada, int cantidadRecibida,
                        int diferencia) {

        public static Linea de(LineaVerificacion l) {
            return new Linea(l.getProducto().getId(), l.getProducto().getNombre(), l.getCantidadFacturada(),
                    l.getCantidadRecibida(), l.getDiferencia());
        }
    }

    public static VerificacionResponse de(VerificacionIngreso v) {
        return new VerificacionResponse(v.getId(), v.getIngreso().getId(), v.getFechaHora(), v.getResultado(),
                v.getObservacion(), v.getLineas().stream().map(Linea::de).toList());
    }
}
