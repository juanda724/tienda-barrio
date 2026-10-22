package co.tiendabarrio.dto.request;

import jakarta.validation.constraints.NotBlank;

/** Cómo se resolvieron las diferencias entre la factura y lo recibido. */
public record ResolverDiferenciasRequest(
        @NotBlank(message = "Describa cómo se resolvieron las diferencias") String nota) {
}
