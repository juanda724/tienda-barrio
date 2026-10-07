package co.tiendabarrio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ClienteRequest(
        @NotBlank(message = "El nombre del cliente es obligatorio") String nombre,
        @Pattern(regexp = "^$|^[0-9 +()-]{7,20}$", message = "El teléfono solo puede tener números, espacios y + ( ) -")
        String telefono,
        String direccion) {
}
