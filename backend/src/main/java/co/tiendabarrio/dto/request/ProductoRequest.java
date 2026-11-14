package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.TratamientoIva;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Datos para crear o editar un producto. stockInicial solo se usa al crear. Precios en pesos, sin decimales.
 * Sin tratamientoIva, un producto nuevo queda en la tarifa general y uno existente conserva el que tiene.
 */
public record ProductoRequest(
        @NotBlank(message = "El nombre del producto es obligatorio") String nombre,
        String categoria,
        @NotNull(message = "El stock mínimo es obligatorio")
        @Min(value = 0, message = "El stock mínimo no puede ser negativo") Integer stockMinimo,
        @Min(value = 0, message = "El stock inicial no puede ser negativo") Integer stockInicial,
        @NotNull(message = "El precio de venta es obligatorio")
        @Min(value = 0, message = "El precio de venta no puede ser negativo") Long precioVenta,
        @NotNull(message = "El costo es obligatorio")
        @Min(value = 0, message = "El costo no puede ser negativo") Long costo,
        TratamientoIva tratamientoIva) {

    /** Producto sin indicar su tratamiento de IVA. */
    public ProductoRequest(String nombre, String categoria, Integer stockMinimo, Integer stockInicial,
                           Long precioVenta, Long costo) {
        this(nombre, categoria, stockMinimo, stockInicial, precioVenta, costo, null);
    }
}
