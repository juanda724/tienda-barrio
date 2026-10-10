package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.FacturaPedidoResponse;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.EntregaFaltantes;
import co.tiendabarrio.model.EstadoPago;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.PedidoProveedor;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.model.ResultadoVerificacion;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.repository.PedidoProveedorRepository;
import co.tiendabarrio.util.Dinero;
import co.tiendabarrio.util.Enlaces;

/**
 * Factura de compra de un pedido a proveedor, para archivarla o enviarla una vez que el pedido se recibió
 * con un ingreso de mercancía y ese ingreso quedó pagado.
 */
@Service
public class FacturaPedidoService {

    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a");

    private final PedidoProveedorRepository pedidos;
    private final IngresoRepository ingresos;
    private final String nombreTienda;

    public FacturaPedidoService(PedidoProveedorRepository pedidos, IngresoRepository ingresos,
                                @Value("${tienda.nombre:Tienda de Barrio}") String nombreTienda) {
        this.pedidos = pedidos;
        this.ingresos = ingresos;
        this.nombreTienda = nombreTienda;
    }

    @Transactional(readOnly = true)
    public FacturaPedidoResponse generar(Long pedidoId) {
        PedidoProveedor pedido = pedidos.findById(pedidoId)
                .orElseThrow(() -> new NoEncontradoException("Pedido a proveedor no encontrado"));
        IngresoMercancia ingreso = ingresos.findFirstByPedidoIdOrderByIdDesc(pedidoId)
                .orElseThrow(() -> new NegocioException("El pedido #" + pedidoId + " todavía no se ha recibido"));
        if (ingreso.getEstadoPago() != EstadoPago.PAGADO) {
            throw new NegocioException("El pedido #" + pedidoId + " todavía no está pagado (ingreso #"
                    + ingreso.getId() + ": " + ingreso.getEstadoPago().getNombre().toLowerCase() + ")");
        }

        Map<Long, Integer> pedidas = pedido.getLineas().stream()
                .collect(Collectors.toMap(l -> l.getProducto().getId(), LineaProducto::getCantidad, Integer::sum));
        // Si la factura se ajustó a lo recibido se cobra lo recibido; si no, lo facturado (igual que el pago)
        boolean porRecibido = ingreso.getResultadoVerificacion() == ResultadoVerificacion.DIFERENCIAS_RESUELTAS;
        List<FacturaPedidoResponse.Linea> lineas = ingreso.getLineas().stream().map(l -> {
            int cobrada = porRecibido ? l.getCantidadRecibidaTotal() : l.getCantidadFacturada();
            long costo = l.getPrecioUnitario() == null ? 0 : l.getPrecioUnitario();
            return new FacturaPedidoResponse.Linea(l.getProducto().getId(), l.getProducto().getNombre(),
                    pedidas.get(l.getProducto().getId()), l.getCantidadFacturada(), l.getCantidadRecibidaTotal(),
                    cobrada, costo, costo * cobrada);
        }).toList();
        long subtotal = lineas.stream().mapToLong(FacturaPedidoResponse.Linea::subtotal).sum();

        String texto = texto(pedido, ingreso, lineas, subtotal);
        Proveedor proveedor = ingreso.getProveedor();
        String whatsapp = proveedor.getTelefono() == null ? null : Enlaces.whatsapp(proveedor.getTelefono(), texto);
        String correo = Enlaces.correo(proveedor.getCorreo(), "Factura del pedido #" + pedidoId + " - " + nombreTienda, texto);
        return new FacturaPedidoResponse(pedidoId, ingreso.getId(), proveedor.getNombre(), proveedor.getNombreContacto(),
                proveedor.getTelefono(), proveedor.getCorreo(), ingreso.getNumeroFactura(),
                pedido.getFechaCreacion(), ingreso.getFechaHora(), ingreso.getFechaPago(), lineas, subtotal,
                ingreso.getTotalCreditoDevoluciones(), ingreso.getMontoPagado(), ingreso.getFormaPagoPago().getNombre(),
                ingreso.getMontoEntregado(), ingreso.getCambio(),
                ingreso.getEntregasFaltantes().stream().map(IngresoResponse.Entrega::de).toList(),
                texto, whatsapp, correo);
    }

    private String texto(PedidoProveedor pedido, IngresoMercancia i, List<FacturaPedidoResponse.Linea> lineas,
                         long subtotal) {
        StringBuilder t = new StringBuilder()
                .append(nombreTienda).append('\n')
                .append("Factura del pedido #").append(pedido.getId()).append('\n')
                .append("Proveedor: ").append(i.getProveedor().getNombre()).append('\n');
        Proveedor proveedor = i.getProveedor();
        if (proveedor.getNombreContacto() != null) {
            t.append("Contacto: ").append(proveedor.getNombreContacto()).append('\n');
        }
        if (proveedor.getTelefono() != null) {
            t.append("Teléfono: ").append(proveedor.getTelefono()).append('\n');
        }
        if (proveedor.getCorreo() != null) {
            t.append("Correo: ").append(proveedor.getCorreo()).append('\n');
        }
        if (i.getNumeroFactura() != null) {
            t.append("Factura del proveedor: ").append(i.getNumeroFactura()).append('\n');
        }
        t.append("Pedido: ").append(pedido.getFechaCreacion().format(FECHA_HORA)).append('\n')
                .append("Recibido: ").append(i.getFechaHora().format(FECHA_HORA)).append(" (ingreso #").append(i.getId())
                .append(")\n")
                .append("Pagado: ").append(i.getFechaPago().format(FECHA_HORA)).append("\n\n");
        for (FacturaPedidoResponse.Linea l : lineas) {
            t.append("- ").append(l.cantidadCobrada()).append(" x ").append(l.productoNombre())
                    .append(" a ").append(Dinero.formatear(l.costoUnitario()))
                    .append(" = ").append(Dinero.formatear(l.subtotal())).append('\n');
        }
        t.append("\nSubtotal: ").append(Dinero.formatear(subtotal)).append('\n');
        if (i.getTotalCreditoDevoluciones() > 0) {
            t.append("Notas crédito por devoluciones: -").append(Dinero.formatear(i.getTotalCreditoDevoluciones()))
                    .append('\n');
        }
        if (!i.getEntregasFaltantes().isEmpty()) {
            t.append("Entregas posteriores de faltantes:\n");
            for (EntregaFaltantes e : i.getEntregasFaltantes()) {
                t.append("  ").append(e.getFechaHora().format(FECHA_HORA)).append(": ")
                        .append(String.join(", ", e.getLineas().stream()
                                .map(l -> l.getCantidad() + " x " + l.getProducto().getNombre()).toList()))
                        .append('\n');
            }
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
