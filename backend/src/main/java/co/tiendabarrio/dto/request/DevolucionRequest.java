package co.tiendabarrio.dto.request;

import co.tiendabarrio.model.MotivoDevolucion;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** fotoBase64 es opcional: la imagen codificada en Base64, sin el prefijo "data:...". */
public record DevolucionRequest(
        @NotNull(message = "Seleccione el ingreso de mercancía") Long ingresoId,
        @NotNull(message = "Seleccione el producto") Long productoId,
        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad debe ser mayor que cero") Integer cantidad,
        @NotNull(message = "Seleccione el motivo de la devolución") MotivoDevolucion motivo,
        @NotBlank(message = "Describa el problema del producto (evidencia escrita)")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres") String descripcion,
        String fotoBase64,
        String fotoTipo) {
}
