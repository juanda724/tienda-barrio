package co.tiendabarrio.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/** pedidoId es opcional: si viene, el ingreso recibe ese pedido a proveedor y lo marca entregado. */
public record IngresoRequest(
        @NotNull(message = "Debe seleccionar un proveedor") Long proveedorId,
        Long pedidoId,
        String numeroFactura,
        @NotEmpty(message = "El ingreso debe tener al menos un producto")
        List<@Valid LineaIngresoRequest> lineas) {
}
