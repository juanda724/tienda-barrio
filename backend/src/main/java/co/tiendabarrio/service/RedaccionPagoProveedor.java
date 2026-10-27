package co.tiendabarrio.service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import co.tiendabarrio.model.EntregaFaltantes;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.util.Dinero;

/**
 * Redacción de las partes comunes de los documentos del pago a un proveedor: el comprobante de pago
 * ({@link ComprobantePagoService}) y la factura del pedido ({@link FacturaPedidoService}). Cada documento
 * conserva su propio orden y presentación; aquí está, en un solo lugar, lo que ambos escriben igual.
 */
@Component
public class RedaccionPagoProveedor {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final String nombreTienda;

    public RedaccionPagoProveedor(@Value("${tienda.nombre:Tienda de Barrio}") String nombreTienda) {
        this.nombreTienda = nombreTienda;
    }

    public String nombreTienda() {
        return nombreTienda;
    }

    /** Fecha y hora como se muestran en los documentos, p. ej. "07/10/2026 03:39 p. m.". */
    public String fecha(LocalDateTime fechaHora) {
        return fechaHora.format(FECHA_HORA);
    }

    /** Productos de una entrega posterior de faltantes: "3 x Aceite 1 L, 2 x Arroz 500 g". */
    public String productosEntregados(EntregaFaltantes entrega) {
        return String.join(", ", entrega.getLineas().stream()
                .map(l -> l.getCantidad() + " x " + l.getProducto().getNombre()).toList());
    }

    /** "Notas crédito por devoluciones: -$ 15.600" con su salto de línea. */
    public String notasCredito(IngresoMercancia ingreso) {
        return "Notas crédito por devoluciones: -" + Dinero.formatear(ingreso.getTotalCreditoDevoluciones()) + '\n';
    }

    /** Total pagado, forma de pago y, si se pagó en efectivo indicando lo entregado, el cambio devuelto. */
    public void agregarPago(StringBuilder texto, IngresoMercancia ingreso) {
        texto.append("TOTAL PAGADO: ").append(Dinero.formatear(ingreso.getMontoPagado())).append('\n')
                .append("Forma de pago: ").append(ingreso.getFormaPagoPago().getNombre()).append('\n');
        if (ingreso.getMontoEntregado() != null) {
            texto.append("Efectivo entregado: ").append(Dinero.formatear(ingreso.getMontoEntregado())).append('\n')
                    .append("Cambio devuelto: ").append(Dinero.formatear(ingreso.getCambio())).append('\n');
        }
    }

    /** Cierre de ambos documentos. */
    public String despedida() {
        return "\nGracias por su atención.";
    }
}
