package co.tiendabarrio.dto.response;

import co.tiendabarrio.model.Cliente;

/** Cliente con el resumen de su cuenta: lo fiado, lo abonado y lo que debe (SWR-22). */
public record ClienteResponse(
        Long id,
        String nombre,
        String telefono,
        String direccion,
        long totalFiado,
        long totalAbonado,
        long saldoPendiente) {

    public static ClienteResponse de(Cliente c, long totalFiado, long totalAbonado) {
        return new ClienteResponse(c.getId(), c.getNombre(), c.getTelefono(), c.getDireccion(), totalFiado,
                totalAbonado, totalFiado - totalAbonado);
    }
}
