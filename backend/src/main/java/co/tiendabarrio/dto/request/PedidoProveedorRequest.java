package co.tiendabarrio.dto.request;

import java.time.LocalDate;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public record PedidoProveedorRequest(
        @NotNull(message = "Debe seleccionar un proveedor") Long proveedorId,
        @NotEmpty(message = "El pedido debe tener al menos un producto")
        List<@Valid LineaProductoRequest> lineas,
        @FutureOrPresent(message = "La fecha deseada de entrega no puede estar en el pasado")
        LocalDate fechaEstimadaEntrega,
        String observaciones) {
}
