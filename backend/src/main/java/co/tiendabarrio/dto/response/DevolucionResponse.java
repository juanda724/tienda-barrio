package co.tiendabarrio.dto.response;

import java.time.LocalDateTime;
import java.util.List;

import co.tiendabarrio.model.CambioEstadoDevolucion;
import co.tiendabarrio.model.Devolucion;
import co.tiendabarrio.model.EstadoDevolucion;
import co.tiendabarrio.model.EvidenciaFoto;
import co.tiendabarrio.model.LineaDevolucion;

/**
 * Devolución con su evidencia (SWR-18), la nota de devolución en texto y sus enlaces de envío
 * (SWR-19), y el estado con su historial y los cambios permitidos (SWR-20).
 */
public record DevolucionResponse(
        Long id,
        Long ingresoId,
        Long proveedorId,
        String proveedorNombre,
        String numeroFactura,
        LocalDateTime fechaHora,
        String descripcion,
        Estado estado,
        boolean enCurso,
        List<Linea> lineas,
        long valor,
        Long montoCredito,
        List<Foto> fotos,
        List<Cambio> historial,
        List<Estado> siguientesEstados,
        String nota,
        String whatsappUrl,
        String correoUrl) {

    public record Estado(EstadoDevolucion estado, String nombre) {
        static Estado de(EstadoDevolucion e) {
            return new Estado(e, e.getNombre());
        }
    }

    public record Linea(Long productoId, String productoNombre, int cantidad, String motivo, String motivoNombre,
                        long costoUnitario, long valor) {
        static Linea de(LineaDevolucion l) {
            return new Linea(l.getProducto().getId(), l.getProducto().getNombre(), l.getCantidad(), l.getMotivo().name(),
                    l.getMotivo().getNombre(), l.getCostoUnitario(), l.getValor());
        }
    }

    /** url es la ruta de la API desde la que se descarga la imagen. */
    public record Foto(Long id, String url) {
    }

    public record Cambio(Estado estado, LocalDateTime fechaHora, String nota) {
        static Cambio de(CambioEstadoDevolucion c) {
            return new Cambio(Estado.de(c.getEstado()), c.getFechaHora(), c.getNota());
        }
    }

    public static DevolucionResponse de(Devolucion d, String nota, String whatsappUrl, String correoUrl) {
        return new DevolucionResponse(
                d.getId(),
                d.getIngreso().getId(),
                d.getIngreso().getProveedor().getId(),
                d.getIngreso().getProveedor().getNombre(),
                d.getIngreso().getNumeroFactura(),
                d.getFechaHora(),
                d.getDescripcion(),
                Estado.de(d.getEstado()),
                d.getEstado().enCurso(),
                d.getLineas().stream().map(Linea::de).toList(),
                d.getValor(),
                d.getMontoCredito(),
                d.getFotos().stream().map(f -> foto(d, f)).toList(),
                d.getHistorial().stream().map(Cambio::de).toList(),
                d.getEstado().siguientes().stream().map(Estado::de).toList(),
                nota,
                whatsappUrl,
                correoUrl);
    }

    private static Foto foto(Devolucion d, EvidenciaFoto f) {
        return new Foto(f.getId(), "/api/devoluciones/" + d.getId() + "/fotos/" + f.getId());
    }
}
