package co.tiendabarrio.dto.response;

import co.tiendabarrio.model.Adquiriente;

/** Datos del cliente para la factura electrónica. */
public record AdquirienteResponse(
        Long id,
        String tipoDocumento,
        String tipoDocumentoNombre,
        String numeroDocumento,
        String nombre,
        String correo,
        String telefono,
        String direccion,
        String ciudad) {

    public static AdquirienteResponse de(Adquiriente a) {
        return new AdquirienteResponse(a.getId(), a.getTipoDocumento().name(), a.getTipoDocumento().getNombre(),
                a.getNumeroDocumento(), a.getNombre(), a.getCorreo(), a.getTelefono(), a.getDireccion(), a.getCiudad());
    }
}
