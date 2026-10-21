package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Cliente;
import co.tiendabarrio.model.LineaVenta;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.TipoPago;
import co.tiendabarrio.model.Venta;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.VentaRepository;
import co.tiendabarrio.util.Dinero;

/** Ventas y facturación (F-05): registra la venta, calcula el total, descarga el inventario y genera el comprobante. */
@Service
public class VentaService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final InventarioService inventario;
    private final CreditoService credito;

    public VentaService(VentaRepository ventas, ProductoRepository productos, InventarioService inventario,
                        CreditoService credito) {
        this.ventas = ventas;
        this.productos = productos;
        this.inventario = inventario;
        this.credito = credito;
    }

    @Transactional(readOnly = true)
    public List<VentaResponse> listar() {
        return ventas.findAllByOrderByFechaHoraDescIdDesc().stream().map(VentaResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public VentaResponse obtener(Long id) {
        return VentaResponse.de(buscar(id));
    }

    /**
     * Registra la venta con fecha, hora, productos, cantidades y valor total (SWR-11, SWR-12) y descarga
     * cada producto del inventario. Si algo falla (sin stock, sin precio, falta el cliente) no se guarda nada.
     */
    @Transactional
    public VentaResponse registrar(VentaRequest datos) {
        Cliente cliente = null;
        if (datos.tipoPago() == TipoPago.CREDITO) {
            if (datos.clienteNombre() == null || datos.clienteNombre().isBlank()) {
                throw new NegocioException("La venta a crédito necesita el nombre del cliente");
            }
            cliente = credito.obtenerOCrearCliente(datos.clienteNombre(), datos.clienteTelefono());
        }

        Venta venta = ventas.save(new Venta(datos.tipoPago(), cliente));
        String referencia = venta.getNumero() + (cliente == null ? "" : " · Fiado a " + cliente.getNombre());
        for (Map.Entry<Long, Integer> linea : LineaProductoRequest.agrupar(datos.lineas()).entrySet()) {
            Producto producto = productos.findById(linea.getKey())
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            if (producto.getPrecioVenta().signum() <= 0) {
                throw new NegocioException(producto.getNombre()
                        + " no tiene precio de venta. Defínalo en la sección Inventario");
            }
            inventario.registrarSalida(producto, linea.getValue(), OrigenMovimiento.VENTA, referencia);
            venta.agregarLinea(new LineaVenta(producto, linea.getValue(), producto.getPrecioVenta(),
                    producto.getCosto()));
        }
        return VentaResponse.de(venta);
    }

    /** Comprobante o ticket digital en texto, listo para compartir por WhatsApp o imprimir (SWR-14). */
    @Transactional(readOnly = true)
    public String comprobante(Long id) {
        Venta venta = buscar(id);
        StringBuilder ticket = new StringBuilder();
        ticket.append("TIENDA DE BARRIO\n");
        ticket.append("Comprobante de venta ").append(venta.getNumero()).append('\n');
        ticket.append("Fecha: ").append(venta.getFechaHora().format(FORMATO_FECHA)).append('\n');
        if (venta.getTipoPago() == TipoPago.CREDITO) {
            ticket.append("Pago: A crédito (fiado) - Cliente: ").append(venta.getCliente().getNombre()).append('\n');
        } else {
            ticket.append("Pago: De contado\n");
        }
        ticket.append("--------------------------------\n");
        for (LineaVenta linea : venta.getLineas()) {
            ticket.append(linea.getCantidad()).append(" x ").append(linea.getProducto().getNombre())
                    .append(" a ").append(Dinero.formatear(linea.getPrecioUnitario()))
                    .append(" = ").append(Dinero.formatear(linea.getSubtotal())).append('\n');
        }
        ticket.append("--------------------------------\n");
        ticket.append("TOTAL: ").append(Dinero.formatear(venta.getTotal())).append('\n');
        ticket.append("¡Gracias por su compra!");
        return ticket.toString();
    }

    private Venta buscar(Long id) {
        return ventas.findById(id).orElseThrow(() -> new NoEncontradoException("Venta no encontrada"));
    }
}
