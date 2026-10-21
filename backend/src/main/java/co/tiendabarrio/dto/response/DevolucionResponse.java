package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.Devolucion;
import co.tiendabarrio.model.EstadoDevolucion;
import co.tiendabarrio.model.MotivoDevolucion;
import co.tiendabarrio.model.SeguimientoDevolucion;

public record DevolucionResponse(
        Long id,
        String numero,
        Long ingresoId,
        String numeroFactura,
        String proveedorNombre,
        Long productoId,
        String productoNombre,
        int cantidad,
        MotivoDevolucion motivo,
        String descripcion,
        boolean tieneFoto,
        EstadoDevolucion estado,
        LocalDateTime fechaHora,
        LocalDateTime fechaResolucion,
        List<Paso> seguimiento) {

    public record Paso(EstadoDevolucion estado, String nota, LocalDateTime fechaHora) {

        public static Paso de(SeguimientoDevolucion s) {
            return new Paso(s.getEstado(), s.getNota(), s.getFechaHora());
        }
    }

    public static DevolucionResponse de(Devolucion d) {
        return new DevolucionResponse(d.getId(), d.getNumero(), d.getIngreso().getId(),
                d.getIngreso().getNumeroFactura(), d.getIngreso().getProveedor().getNombre(),
                d.getProducto().getId(), d.getProducto().getNombre(), d.getCantidad(), d.getMotivo(),
                d.getDescripcion(), d.tieneFoto(), d.getEstado(), d.getFechaHora(), d.getFechaResolucion(),
                d.getSeguimiento().stream().map(Paso::de).toList());
    }
}
