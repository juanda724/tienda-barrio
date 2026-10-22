package co.tiendabarrio.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CambioEstadoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoProveedorRequest;
import co.tiendabarrio.dto.response.PedidoProveedorResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.EstadoPedido;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.PedidoProveedor;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.repository.PedidoProveedorRepository;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.ProveedorRepository;

/**
 * Pedidos de mercancía a proveedores: se crean en estado "En espera", se envían por WhatsApp
 * o correo y el dueño actualiza su estado hasta recibirlos con un ingreso de mercancía.
 */
@Service
public class PedidoProveedorService {

    private final PedidoProveedorRepository pedidos;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final MensajePedidoService mensajes;
    private final IngresoRepository ingresos;

    public PedidoProveedorService(PedidoProveedorRepository pedidos, ProveedorRepository proveedores,
                                  ProductoRepository productos, MensajePedidoService mensajes,
                                  IngresoRepository ingresos) {
        this.pedidos = pedidos;
        this.proveedores = proveedores;
        this.productos = productos;
        this.mensajes = mensajes;
        this.ingresos = ingresos;
    }

    @Transactional(readOnly = true)
    public List<PedidoProveedorResponse> listar(EstadoPedido estado) {
        List<PedidoProveedor> lista = estado == null
                ? pedidos.findAllByOrderByFechaCreacionDescIdDesc()
                : pedidos.findByEstadoOrderByFechaCreacionDescIdDesc(estado);
        return lista.stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public PedidoProveedorResponse obtener(Long id) {
        return respuesta(buscar(id));
    }

    /**
     * Crea el pedido con productos que el proveedor suministra (SWR-05). Queda "En espera".
     * Regla RN-02: solo se piden productos que estén en o por debajo de su stock mínimo.
     */
    @Transactional
    public PedidoProveedorResponse crear(PedidoProveedorRequest datos) {
        Proveedor proveedor = proveedores.findById(datos.proveedorId())
                .orElseThrow(() -> new NoEncontradoException("Proveedor no encontrado"));
        String observaciones = datos.observaciones() == null || datos.observaciones().isBlank()
                ? null : datos.observaciones().trim();
        PedidoProveedor pedido = new PedidoProveedor(proveedor, datos.fechaEstimadaEntrega(), observaciones);
        for (Map.Entry<Long, Integer> linea : LineaProductoRequest.agrupar(datos.lineas()).entrySet()) {
            Producto producto = productos.findById(linea.getKey())
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            if (!proveedor.suministra(producto)) {
                throw new NegocioException(producto.getNombre() + " no está asociado al proveedor "
                        + proveedor.getNombre());
            }
            if (!producto.isBajoMinimo()) {
                throw new NegocioException(producto.getNombre() + " tiene stock suficiente (" + producto.getStockActual()
                        + ", mínimo " + producto.getStockMinimo() + "). Solo se piden productos en o por debajo "
                        + "del stock mínimo (RN-02)");
            }
            pedido.agregarLinea(new LineaProducto(producto, linea.getValue()));
        }
        return respuesta(pedidos.save(pedido));
    }

    /** Cambio manual de estado hecho por el dueño según lo que le informe el proveedor. */
    @Transactional
    public PedidoProveedorResponse cambiarEstado(Long id, CambioEstadoRequest datos) {
        PedidoProveedor pedido = buscar(id);
        String nota = datos.nota() == null || datos.nota().isBlank() ? null : datos.nota().trim();
        pedido.cambiarEstado(datos.estado(), nota);
        if (datos.fechaEstimadaEntrega() != null) {
            pedido.setFechaEstimadaEntrega(datos.fechaEstimadaEntrega());
        }
        return respuesta(pedido);
    }

    private PedidoProveedor buscar(Long id) {
        return pedidos.findById(id).orElseThrow(() -> new NoEncontradoException("Pedido a proveedor no encontrado"));
    }

    private PedidoProveedorResponse respuesta(PedidoProveedor pedido) {
        String mensaje = mensajes.mensaje(pedido);
        return PedidoProveedorResponse.de(pedido, mensaje, mensajes.whatsappUrl(pedido, mensaje),
                mensajes.correoUrl(pedido, mensaje), ingresoDe(pedido));
    }

    /** Ingreso con el que llegó el pedido; null si aún no se ha recibido. */
    private IngresoMercancia ingresoDe(PedidoProveedor pedido) {
        return pedido.getEstado() == EstadoPedido.ENTREGADO
                ? ingresos.findFirstByPedidoIdOrderByIdDesc(pedido.getId()).orElse(null) : null;
    }
}
