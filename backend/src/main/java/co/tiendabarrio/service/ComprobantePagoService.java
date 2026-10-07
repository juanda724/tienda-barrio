package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
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

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final IngresoRepository ingresos;
    private final String nombreTienda;

    public ComprobantePagoService(IngresoRepository ingresos, @Value("${tienda.nombre:Tienda de Barrio}") String nombreTienda) {
        this.ingresos = ingresos;
        this.nombreTienda = nombreTienda;
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
                + nombreTienda, texto);
        return new ComprobantePagoResponse(ingresoId, texto, whatsapp, correo);
    }

    String texto(IngresoMercancia i) {
        StringBuilder t = new StringBuilder()
                .append(nombreTienda).append('\n')
                .append("Comprobante de pago al proveedor\n")
                .append("Proveedor: ").append(i.getProveedor().getNombre()).append('\n')
                .append("Ingreso #").append(i.getId());
        if (i.getNumeroFactura() != null) {
            t.append(" · Factura ").append(i.getNumeroFactura());
        }
        t.append('\n').append("Fecha de pago: ").append(i.getFechaPago().format(FECHA_HORA)).append("\n\n")
                .append("Valor facturado: ").append(Dinero.formatear(i.getTotalFacturado())).append('\n');
        if (i.getMontoPagado() != i.getTotalFacturado()) {
            if (i.getTotalCreditoDevoluciones() > 0) {
                t.append("Notas crédito por devoluciones: -").append(Dinero.formatear(i.getTotalCreditoDevoluciones()))
                        .append('\n');
            }
            if (i.getNotaVerificacion() != null) {
                t.append("Ajuste de factura: ").append(i.getNotaVerificacion()).append('\n');
            }
        }
        if (!i.getEntregasFaltantes().isEmpty()) {
            t.append("\nEntregas posteriores de faltantes:\n");
            for (EntregaFaltantes entrega : i.getEntregasFaltantes()) {
                t.append("- ").append(entrega.getFechaHora().format(FECHA_HORA)).append(": ");
                t.append(String.join(", ", entrega.getLineas().stream()
                        .map(l -> l.getCantidad() + " x " + l.getProducto().getNombre()).toList()));
                if (entrega.getNota() != null) {
                    t.append(" (").append(entrega.getNota()).append(')');
                }
                t.append('\n');
            }
            t.append('\n');
        }
        t.append("TOTAL PAGADO: ").append(Dinero.formatear(i.getMontoPagado())).append('\n')
                .append("Forma de pago: ").append(i.getFormaPagoPago().getNombre()).append('\n');
        if (i.getMontoEntregado() != null) {
            t.append("Efectivo entregado: ").append(Dinero.formatear(i.getMontoEntregado())).append('\n')
                    .append("Cambio devuelto: ").append(Dinero.formatear(i.getCambio())).append('\n');
        }
        return t.append("\nGracias por su atención.").toString();
    }
}
