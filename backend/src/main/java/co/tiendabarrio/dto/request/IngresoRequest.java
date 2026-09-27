package co.tiendabarrio.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record IngresoRequest(
        @NotNull(message = "Debe seleccionar un proveedor") Long proveedorId,
        String numeroFactura,
        @NotEmpty(message = "El ingreso debe tener al menos un producto")
        List<@Valid LineaProductoRequest> lineas) {
}
