package co.tiendabarrio.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.ComprobantePagoResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.EntregaFaltantes;
import co.tiendabarrio.model.EstadoPago;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.util.Dinero;
import co.tiendabarrio.util.Enlaces;

/**
 * Comprobante del pago de un ingreso al proveedor, para archivarlo o enviárselo ("Archivar factura,
 * comprobante y acuerdo de pago" en el proceso de la tienda).
 */
@Service
public class ComprobantePagoService {

    private final IngresoRepository ingresos;
    private final RedaccionPagoProveedor redaccion;

    public ComprobantePagoService(IngresoRepository ingresos, RedaccionPagoProveedor redaccion) {
        this.ingresos = ingresos;
        this.redaccion = redaccion;
    }

    @Transactional(readOnly = true)
    public ComprobantePagoResponse generar(Long ingresoId) {
        IngresoMercancia ingreso = ingresos.findById(ingresoId)
                .orElseThrow(() -> new NoEncontradoException("Ingreso no encontrado"));
        if (ingreso.getEstadoPago() != EstadoPago.PAGADO) {
            throw new NegocioException("El ingreso #" + ingresoId + " todavía no está pagado");
        }
        String texto = texto(ingreso);
        Proveedor proveedor = ingreso.getProveedor();
        String whatsapp = proveedor.getTelefono() == null ? null : Enlaces.whatsapp(proveedor.getTelefono(), texto);
        String correo = Enlaces.correo(proveedor.getCorreo(), "Comprobante de pago - Ingreso #" + ingresoId + " - "
                + redaccion.nombreTienda(), texto);
        return new ComprobantePagoResponse(ingresoId, texto, whatsapp, correo);
    }

    String texto(IngresoMercancia i) {
        StringBuilder t = new StringBuilder()
                .append(redaccion.nombreTienda()).append('\n')
                .append("Comprobante de pago al proveedor\n")
                .append("Proveedor: ").append(i.getProveedor().getNombre()).append('\n')
                .append("Ingreso #").append(i.getId());
        if (i.getNumeroFactura() != null) {
            t.append(" · Factura ").append(i.getNumeroFactura());
        }
        t.append('\n').append("Fecha de pago: ").append(redaccion.fecha(i.getFechaPago())).append("\n\n")
                .append("Valor facturado: ").append(Dinero.formatear(i.getTotalFacturado())).append('\n');
        if (i.getMontoPagado() != i.getTotalFacturado()) {
            if (i.getTotalCreditoDevoluciones() > 0) {
                t.append(redaccion.notasCredito(i));
            }
            if (i.getNotaVerificacion() != null) {
                t.append("Ajuste de factura: ").append(i.getNotaVerificacion()).append('\n');
            }
        }
        if (!i.getEntregasFaltantes().isEmpty()) {
            t.append("\nEntregas posteriores de faltantes:\n");
            for (EntregaFaltantes entrega : i.getEntregasFaltantes()) {
                t.append("- ").append(redaccion.fecha(entrega.getFechaHora())).append(": ")
                        .append(redaccion.productosEntregados(entrega));
                if (entrega.getNota() != null) {
                    t.append(" (").append(entrega.getNota()).append(')');
                }
                t.append('\n');
            }
            t.append('\n');
        }
        redaccion.agregarPago(t, i);
        return t.append(redaccion.despedida()).toString();
    }
}
