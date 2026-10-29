package co.tiendabarrio.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.EntregaFaltantesRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.ResolverDiferenciasRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.EntregaFaltantes;
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

    @Transactional(readOnly = true)
    public IngresoResponse obtener(Long id) {
        return IngresoResponse.de(buscar(id));
    }

    /**
     * Registra la mercancía recibida, la suma al inventario (SWR-03, SWR-06) y confronta la factura
     * con lo recibido (SWR-15, SWR-17). Solo se aceptan productos que el proveedor tiene asociados
     * (SWR-05). El costo del producto se actualiza con el de la factura. Si se indica un pedido a
     * proveedor, debe ser del mismo proveedor y estar activo, y queda marcado como entregado.
     */
    @Transactional
    public IngresoResponse registrar(IngresoRequest datos) {
        Proveedor proveedor = buscarProveedor(datos.proveedorId());
        String factura = normalizarFactura(datos.numeroFactura());
        PedidoProveedor pedido = datos.pedidoId() == null ? null : buscarPedido(datos.pedidoId(), proveedor);
        List<LineaIngresoRequest> lineas = lineasConCantidades(datos.lineas());

        IngresoMercancia ingreso = ingresos.save(new IngresoMercancia(proveedor, factura, pedido));
        String referencia = referenciaDeMovimiento(proveedor, factura);
        for (LineaIngresoRequest linea : lineas) {
            recibirLinea(ingreso, proveedor, linea, referencia);
        }
        ingreso.verificar();
        cerrarPedido(pedido, ingreso);
        return IngresoResponse.de(ingreso);
    }

    /**
     * Recibe una línea: el producto debe ser del proveedor (SWR-05); lo que llegó entra al inventario
     * (SWR-03, SWR-06) y actualiza el costo del catálogo, y lo facturado queda para la verificación (SWR-15).
     */
    private void recibirLinea(IngresoMercancia ingreso, Proveedor proveedor, LineaIngresoRequest linea,
                              String referencia) {
        Producto producto = buscarProducto(linea.productoId());
        proveedor.validarQueSuministra(producto);
        if (linea.cantidadRecibida() > 0) {
            inventario.registrarEntrada(producto, linea.cantidadRecibida(), OrigenMovimiento.INGRESO_PROVEEDOR,
                    ingreso.getId(), referencia);
            actualizarCostoDelCatalogo(producto, linea.costoUnitario());
        }
        ingreso.agregarLinea(LineaProducto.deIngreso(producto, linea.cantidadRecibida(),
                linea.cantidadFacturada(), linea.costoUnitario()));
    }

    /**
     * El costo del producto en el catálogo pasa a ser el de la última factura con la que llegó mercancía.
     * Una línea solo facturada, sin unidades recibidas, no lo cambia.
     */
    private static void actualizarCostoDelCatalogo(Producto producto, Long costoUnitario) {
        producto.setCosto(costoUnitario);
    }

    /** El pedido que llegó con este ingreso queda entregado. */
    private static void cerrarPedido(PedidoProveedor pedido, IngresoMercancia ingreso) {
        if (pedido != null) {
            pedido.marcarEntregado("Recibido con el ingreso #" + ingreso.getId());
        }
    }

    /**
     * Une las líneas repetidas, exige que al menos una tenga cantidades y descarta las que no traen nada
     * (ni recibido ni facturado). Conserva el orden en que llegaron.
     */
    private static List<LineaIngresoRequest> lineasConCantidades(List<LineaIngresoRequest> lineas) {
        List<LineaIngresoRequest> conCantidades = agrupar(lineas).values().stream()
                .filter(l -> !(l.cantidadRecibida() == 0 && l.cantidadFacturada() == 0))
                .toList();
        if (conCantidades.isEmpty()) {
            throw new NegocioException("Indique lo recibido o lo facturado de al menos un producto");
        }
        return conCantidades;
    }

    /** Número de factura sin espacios sobrantes; vacío si no se indicó. */
    private static String normalizarFactura(String numeroFactura) {
        return numeroFactura == null || numeroFactura.isBlank() ? null : numeroFactura.trim();
    }

    /** Detalle con que los movimientos de inventario del ingreso identifican su origen. */
    private static String referenciaDeMovimiento(Proveedor proveedor, String factura) {
        return proveedor.getNombre() + (factura == null ? "" : " · Factura " + factura);
    }

    private Proveedor buscarProveedor(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new NoEncontradoException("Proveedor no encontrado"));
    }

    private Producto buscarProducto(Long id) {
        return productos.findById(id).orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
    }

    /** El proveedor ajustó la factura a lo recibido; se desbloquea el pago por el valor recibido. */
    @Transactional
    public IngresoResponse resolverDiferencias(Long id, ResolverDiferenciasRequest datos) {
        IngresoMercancia ingreso = buscar(id);
        ingreso.resolverDiferencias(datos.nota().trim());
        return IngresoResponse.de(ingreso);
    }

    /**
     * El proveedor entregó faltantes del ingreso (todo o parte): entran al inventario como entrada del
     * mismo ingreso. Cuando ya no falta nada, el ingreso queda resuelto y se paga lo facturado.
     */
    @Transactional
    public IngresoResponse registrarEntregaFaltantes(Long id, EntregaFaltantesRequest datos) {
        IngresoMercancia ingreso = buscar(id);
        Map<Long, Integer> cantidades = new LinkedHashMap<>();
        for (EntregaFaltantesRequest.Linea l : datos.lineas()) {
            cantidades.merge(l.productoId(), l.cantidad(), Integer::sum);
        }
        String nota = datos.nota() == null || datos.nota().isBlank() ? null : datos.nota().trim();
        EntregaFaltantes entrega = ingreso.registrarEntregaFaltantes(cantidades, nota);
        String referencia = ingreso.getProveedor().getNombre() + " · Entrega de faltantes del ingreso #" + ingreso.getId();
        for (LineaProducto linea : entrega.getLineas()) {
            inventario.registrarEntrada(linea.getProducto(), linea.getCantidad(), OrigenMovimiento.INGRESO_PROVEEDOR,
                    ingreso.getId(), referencia);
        }
        ingresos.flush();
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
            ingreso.pagar(datos.formaPago(), datos.montoEntregado());
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
