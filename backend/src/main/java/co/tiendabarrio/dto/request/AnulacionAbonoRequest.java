package co.tiendabarrio.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Por qué se anula un abono registrado por error (SWR-34). */
public record AnulacionAbonoRequest(
        @NotBlank(message = "Indique por qué se anula el abono") String motivo) {
}
