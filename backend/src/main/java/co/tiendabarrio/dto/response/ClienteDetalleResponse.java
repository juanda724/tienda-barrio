package co.tiendabarrio.dto.response;

import java.util.List;

public record ClienteDetalleResponse(
        ClienteCreditoResponse cliente,
        List<VentaResponse> ventasACredito,
        List<AbonoResponse> abonos) {
}
