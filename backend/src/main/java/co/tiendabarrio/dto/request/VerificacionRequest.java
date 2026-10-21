package co.tiendabarrio.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record VerificacionRequest(
        @NotEmpty(message = "Ingrese las cantidades de la factura del proveedor")
        List<@Valid LineaFacturadaRequest> lineas,
        String observacion) {
}
