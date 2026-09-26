package co.tiendabarrio.producto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/** Datos para crear o editar un producto. stockInicial solo se usa al crear. */
public record ProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio") String nombre,
        String categoria,
        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo,
        @Min(value = 0, message = "El stock inicial no puede ser negativo") Integer stockInicial) {
}
