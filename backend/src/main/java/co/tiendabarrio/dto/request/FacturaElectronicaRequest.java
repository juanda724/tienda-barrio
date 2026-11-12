package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * Datos del cliente que pide factura electrónica en una venta. Con tipoDocumento CONSUMIDOR_FINAL los demás
 * datos van vacíos; con los otros tipos, el número de documento, el nombre y el correo son obligatorios.
 */
public record FacturaElectronicaRequest(
        @NotNull(message = "Indique el tipo de documento del cliente") TipoDocumento tipoDocumento,
        @Pattern(regexp = "^$|[0-9A-Za-z .-]{3,20}", message = "El número de documento no es válido")
        String numeroDocumento,
        String nombre,
        @Email(message = "El correo del cliente no es válido")
        String correo,
        String telefono,
        String direccion,
        String ciudad) {

    /** Factura a un cliente que no se identifica. */
    public static FacturaElectronicaRequest consumidorFinal() {
        return new FacturaElectronicaRequest(TipoDocumento.CONSUMIDOR_FINAL, null, null, null, null, null, null);
    }

    public boolean esConsumidorFinal() {
        return tipoDocumento == TipoDocumento.CONSUMIDOR_FINAL;
    }
}
