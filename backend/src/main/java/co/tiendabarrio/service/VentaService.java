package co.tiendabarrio.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.config.DatosEmisor;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ComprobanteResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Cliente;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Venta;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.VentaRepository;
import co.tiendabarrio.util.Dinero;

@Service
public class VentaService {

    private final VentaRepository ventas;
    private final ProductoRepository productos;
    private final InventarioService inventario;
    private final ComprobanteService comprobantes;
    private final ClienteService clientes;
    private final FacturaElectronicaService facturas;
    private final DatosEmisor emisor;

    public VentaService(VentaRepository ventas, ProductoRepository productos, InventarioService inventario,
                        ComprobanteService comprobantes, ClienteService clientes, FacturaElectronicaService facturas,
                        DatosEmisor emisor) {
        this.ventas = ventas;
        this.productos = productos;
        this.inventario = inventario;
        this.comprobantes = comprobantes;
        this.clientes = clientes;
        this.facturas = facturas;
        this.emisor = emisor;
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
     * Registra la venta con fecha, hora, productos y total (SWR-11), calcula el total con el precio
     * de venta de cada producto (SWR-12) y descarga el inventario (SWR-02). Una venta a crédito queda
     * a nombre del cliente y suma a su deuda (SWR-21). Si la tienda es responsable de IVA, cada línea guarda
     * el tratamiento de su producto (SWR-33). Si el cliente pide factura electrónica, la expide (SWR-32).
     * Primero valida todo (precios, efectivo recibido, cliente y numeración de la factura) para no
     * tocar el stock si la venta no puede hacerse.
     */
    @Transactional
    public VentaResponse registrar(VentaRequest datos) {
        List<LineaProducto> lineas = new ArrayList<>();
        for (Map.Entry<Long, Integer> linea : LineaProductoRequest.agrupar(datos.lineas()).entrySet()) {
            Producto producto = productos.findById(linea.getKey())
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            if (producto.getPrecioVenta() == null) {
                throw new NegocioException(producto.getNombre()
                        + " no tiene precio de venta. Defínalo en la pestaña Inventario");
            }
            lineas.add(LineaProducto.deVenta(producto, linea.getValue(), emisor.responsableIva()));
        }

        long total = lineas.stream().mapToLong(LineaProducto::getSubtotal).sum();
        Long recibido = datos.formaPago() == FormaPago.EFECTIVO ? datos.montoRecibido() : null;
        if (recibido != null && recibido < total) {
            throw new NegocioException("El efectivo recibido (" + Dinero.formatear(recibido)
                    + ") es menor que el total de la venta (" + Dinero.formatear(total) + ")");
        }

        Cliente cliente = null;
        if (datos.formaPago() == FormaPago.CREDITO) {
            if (datos.clienteId() == null) {
                throw new NegocioException("Seleccione el cliente al que se le fía");
            }
            cliente = clientes.buscar(datos.clienteId());
        }

        Venta venta = new Venta(datos.formaPago(), recibido, cliente);
        lineas.forEach(venta::agregarLinea);
        if (datos.facturaElectronica() != null) {
            facturas.expedir(venta, datos.facturaElectronica());
        }
        venta = ventas.save(venta);
        for (LineaProducto linea : lineas) {
            inventario.registrarSalida(linea.getProducto(), linea.getCantidad(), OrigenMovimiento.VENTA,
                    venta.getId(), null);
        }
        return VentaResponse.de(venta);
    }

    /**
     * Comprobante digital de la venta para compartir con el cliente (SWR-14). Si es fiada, incluye
     * el saldo actual del cliente y, si no se indica otro teléfono, va dirigido al del cliente.
     */
    @Transactional(readOnly = true)
    public ComprobanteResponse comprobante(Long id, String telefono) {
        Venta venta = buscar(id);
        Cliente cliente = venta.getCliente();
        Long saldo = cliente == null ? null : clientes.saldo(cliente.getId());
        String destino = telefono == null || telefono.isBlank()
                ? (cliente == null ? null : cliente.getTelefono())
                : telefono;
        return comprobantes.de(venta, destino, saldo);
    }

    private Venta buscar(Long id) {
        return ventas.findById(id).orElseThrow(() -> new NoEncontradoException("Venta no encontrada"));
    }
}
