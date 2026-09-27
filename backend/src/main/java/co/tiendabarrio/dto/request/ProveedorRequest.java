package co.tiendabarrio.dto.request;

import java.util.List;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ProveedorRequest(
        @NotBlank(message = "El nombre del proveedor es obligatorio") String nombre,
        String nombreContacto,
        @NotBlank(message = "El teléfono de contacto es obligatorio")
        @Pattern(regexp = "^[0-9 +()-]{7,20}$", message = "El teléfono solo puede tener números, espacios y + ( ) -")
        String telefono,
        @Email(message = "El correo no tiene un formato válido") String correo,
        List<Long> productoIds) {
}
