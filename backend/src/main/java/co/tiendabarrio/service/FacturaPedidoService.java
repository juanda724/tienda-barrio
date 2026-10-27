package co.tiendabarrio.service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    private final PedidoProveedorRepository pedidos;
    private final IngresoRepository ingresos;
    private final RedaccionPagoProveedor redaccion;

    public FacturaPedidoService(PedidoProveedorRepository pedidos, IngresoRepository ingresos,
                                RedaccionPagoProveedor redaccion) {
        this.pedidos = pedidos;
        this.ingresos = ingresos;
        this.redaccion = redaccion;
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
        // Cuánto se cobra de cada línea lo decide el ingreso: es la misma regla con la que se pagó
        List<FacturaPedidoResponse.Linea> lineas = ingreso.getLineas().stream()
                .map(l -> new FacturaPedidoResponse.Linea(l.getProducto().getId(), l.getProducto().getNombre(),
                        pedidas.get(l.getProducto().getId()), l.getCantidadFacturada(), l.getCantidadRecibidaTotal(),
                        ingreso.getCantidadCobrada(l), l.getCostoUnitario(), ingreso.getSubtotalCobrado(l)))
                .toList();
        long subtotal = ingreso.getTotalCobrado();

        String texto = texto(pedido, ingreso, lineas, subtotal);
        Proveedor proveedor = ingreso.getProveedor();
        String whatsapp = proveedor.getTelefono() == null ? null : Enlaces.whatsapp(proveedor.getTelefono(), texto);
        String correo = Enlaces.correo(proveedor.getCorreo(),
                "Factura del pedido #" + pedidoId + " - " + redaccion.nombreTienda(), texto);
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
                .append(redaccion.nombreTienda()).append('\n')
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
        t.append("Pedido: ").append(redaccion.fecha(pedido.getFechaCreacion())).append('\n')
                .append("Recibido: ").append(redaccion.fecha(i.getFechaHora())).append(" (ingreso #").append(i.getId())
                .append(")\n")
                .append("Pagado: ").append(redaccion.fecha(i.getFechaPago())).append("\n\n");
        for (FacturaPedidoResponse.Linea l : lineas) {
            t.append("- ").append(l.cantidadCobrada()).append(" x ").append(l.productoNombre())
                    .append(" a ").append(Dinero.formatear(l.costoUnitario()))
                    .append(" = ").append(Dinero.formatear(l.subtotal())).append('\n');
        }
        t.append("\nSubtotal: ").append(Dinero.formatear(subtotal)).append('\n');
        if (i.getTotalCreditoDevoluciones() > 0) {
            t.append(redaccion.notasCredito(i));
        }
        if (!i.getEntregasFaltantes().isEmpty()) {
            t.append("Entregas posteriores de faltantes:\n");
            for (EntregaFaltantes e : i.getEntregasFaltantes()) {
                t.append("  ").append(redaccion.fecha(e.getFechaHora())).append(": ")
                        .append(redaccion.productosEntregados(e)).append('\n');
            }
        }
        redaccion.agregarPago(t, i);
        return t.append(redaccion.despedida()).toString();
    }
}
