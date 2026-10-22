package co.tiendabarrio.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Devolución de productos no conformes de un ingreso. Las fotos se suben después, aparte. */
public record DevolucionRequest(
        @NotNull(message = "Indique el ingreso de mercancía al que corresponde") Long ingresoId,
        @NotBlank(message = "Describa el problema encontrado (evidencia escrita)")
        @Size(max = 1000, message = "La descripción no puede superar 1000 caracteres") String descripcion,
        @NotEmpty(message = "La devolución debe tener al menos un producto")
        List<@Valid LineaDevolucionRequest> lineas) {
}
