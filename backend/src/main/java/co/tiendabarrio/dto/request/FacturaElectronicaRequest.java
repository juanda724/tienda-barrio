package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.TipoDocumento;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/** Datos del cliente que pide factura electrónica en una venta. */
public record FacturaElectronicaRequest(
        @NotNull(message = "Indique el tipo de documento del cliente") TipoDocumento tipoDocumento,
        @NotBlank(message = "Indique el número de documento del cliente")
        @Pattern(regexp = "[0-9A-Za-z .-]{3,20}", message = "El número de documento no es válido")
        String numeroDocumento,
        @NotBlank(message = "Indique el nombre o razón social del cliente") String nombre,
        @NotBlank(message = "Indique el correo al que se enviará la factura electrónica")
        @Email(message = "El correo del cliente no es válido")
        String correo,
        String telefono,
        String direccion,
        String ciudad) {
}
