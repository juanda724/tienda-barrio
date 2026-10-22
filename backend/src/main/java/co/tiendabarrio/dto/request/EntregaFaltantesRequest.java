package co.tiendabarrio.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** Productos faltantes que el proveedor entregó después del ingreso; puede ser una entrega parcial. */
public record EntregaFaltantesRequest(
        @NotEmpty(message = "Indique los productos entregados") List<@Valid Linea> lineas,
        String nota) {

    public record Linea(
            @NotNull(message = "Falta el producto") Long productoId,
            @NotNull(message = "Falta la cantidad") @Min(value = 0, message = "La cantidad no puede ser negativa") Integer cantidad) {
    }
}
