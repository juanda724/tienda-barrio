package co.tiendabarrio.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.ResolverDiferenciasRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.PedidoProveedor;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.repository.PedidoProveedorRepository;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.ProveedorRepository;

@Service
public class IngresoService {

    private final IngresoRepository ingresos;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final PedidoProveedorRepository pedidosProveedor;
    private final InventarioService inventario;

    public IngresoService(IngresoRepository ingresos, ProveedorRepository proveedores,
                          ProductoRepository productos, PedidoProveedorRepository pedidosProveedor,
                          InventarioService inventario) {
        this.ingresos = ingresos;
        this.proveedores = proveedores;
        this.productos = productos;
        this.pedidosProveedor = pedidosProveedor;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public List<IngresoResponse> listar() {
        return ingresos.findAllByOrderByFechaHoraDescIdDesc().stream().map(IngresoResponse::de).toList();
    }

    /**
     * Registra la mercancía recibida, la suma al inventario (SWR-03, SWR-06) y confronta la factura
     * con lo recibido (SWR-15, SWR-17). Solo se aceptan productos que el proveedor tiene asociados
     * (SWR-05). El costo del producto se actualiza con el de la factura. Si se indica un pedido a
     * proveedor, debe ser del mismo proveedor y estar activo, y queda marcado como entregado.
     */
    @Transactional
    public IngresoResponse registrar(IngresoRequest datos) {
        Proveedor proveedor = proveedores.findById(datos.proveedorId())
                .orElseThrow(() -> new NoEncontradoException("Proveedor no encontrado"));
        String factura = datos.numeroFactura() == null || datos.numeroFactura().isBlank()
                ? null : datos.numeroFactura().trim();
        PedidoProveedor pedido = datos.pedidoId() == null ? null : buscarPedido(datos.pedidoId(), proveedor);
        Map<Long, LineaIngresoRequest> lineas = agrupar(datos.lineas());
        if (lineas.values().stream().allMatch(l -> l.cantidadRecibida() == 0 && l.cantidadFacturada() == 0)) {
            throw new NegocioException("Indique lo recibido o lo facturado de al menos un producto");
        }
        IngresoMercancia ingreso = ingresos.save(new IngresoMercancia(proveedor, factura, pedido));

        String referencia = proveedor.getNombre() + (factura == null ? "" : " · Factura " + factura);
        for (LineaIngresoRequest linea : lineas.values()) {
            if (linea.cantidadRecibida() == 0 && linea.cantidadFacturada() == 0) {
                continue;
            }
            Producto producto = productos.findById(linea.productoId())
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            if (!proveedor.suministra(producto)) {
                throw new NegocioException(producto.getNombre() + " no está asociado al proveedor "
                        + proveedor.getNombre());
            }
            if (linea.cantidadRecibida() > 0) {
                inventario.registrarEntrada(producto, linea.cantidadRecibida(), OrigenMovimiento.INGRESO_PROVEEDOR,
                        ingreso.getId(), referencia);
                producto.setCosto(linea.costoUnitario());
            }
            ingreso.agregarLinea(LineaProducto.deIngreso(producto, linea.cantidadRecibida(),
                    linea.cantidadFacturada(), linea.costoUnitario()));
        }
        ingreso.verificar();
        if (pedido != null) {
            pedido.marcarEntregado("Recibido con el ingreso #" + ingreso.getId());
        }
        return IngresoResponse.de(ingreso);
    }

    /** El proveedor ajustó la factura a lo recibido; se desbloquea el pago por el valor recibido. */
    @Transactional
    public IngresoResponse resolverDiferencias(Long id, ResolverDiferenciasRequest datos) {
        IngresoMercancia ingreso = buscar(id);
        ingreso.resolverDiferencias(datos.nota().trim());
        return IngresoResponse.de(ingreso);
    }

    /**
     * Paga el ingreso (efectivo, transferencia o tarjeta) o acuerda un crédito con fecha de vencimiento.
     * Ambas opciones se rechazan si hay diferencias sin resolver (SWR-16, RN-01).
     */
    @Transactional
    public IngresoResponse pagar(Long id, PagoProveedorRequest datos) {
        IngresoMercancia ingreso = buscar(id);
        if (datos.formaPago() == null) {
            throw new NegocioException("Indique la forma de pago");
        }
        if (datos.formaPago() == FormaPago.CREDITO) {
            if (datos.fechaVencimiento() == null) {
                throw new NegocioException("Indique la fecha de vencimiento del crédito");
            }
            ingreso.acordarCredito(datos.fechaVencimiento());
        } else {
            ingreso.pagar(datos.formaPago());
        }
        return IngresoResponse.de(ingreso);
    }

    private IngresoMercancia buscar(Long id) {
        return ingresos.findById(id).orElseThrow(() -> new NoEncontradoException("Ingreso no encontrado"));
    }

    /** Une las líneas repetidas del mismo producto sumando cantidades; conserva el último costo. */
    private static Map<Long, LineaIngresoRequest> agrupar(List<LineaIngresoRequest> lineas) {
        Map<Long, LineaIngresoRequest> porProducto = new LinkedHashMap<>();
        for (LineaIngresoRequest l : lineas) {
            porProducto.merge(l.productoId(), l, (a, b) -> new LineaIngresoRequest(a.productoId(),
                    a.cantidadRecibida() + b.cantidadRecibida(), a.cantidadFacturada() + b.cantidadFacturada(),
                    b.costoUnitario()));
        }
        return porProducto;
    }

    private PedidoProveedor buscarPedido(Long pedidoId, Proveedor proveedor) {
        PedidoProveedor pedido = pedidosProveedor.findById(pedidoId)
                .orElseThrow(() -> new NoEncontradoException("Pedido a proveedor no encontrado"));
        if (!pedido.getProveedor().getId().equals(proveedor.getId())) {
            throw new NegocioException("El pedido #" + pedidoId + " es de " + pedido.getProveedor().getNombre()
                    + ", no de " + proveedor.getNombre());
        }
        pedido.validarQuePuedeRecibirse();
        return pedido;
    }
}
