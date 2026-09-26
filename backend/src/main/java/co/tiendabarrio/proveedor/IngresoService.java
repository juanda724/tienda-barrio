package co.tiendabarrio.proveedor;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.comun.NegocioException;
import co.tiendabarrio.comun.NoEncontradoException;
import co.tiendabarrio.inventario.InventarioService;
import co.tiendabarrio.inventario.LineaProducto;
import co.tiendabarrio.inventario.LineaProductoRequest;
import co.tiendabarrio.inventario.OrigenMovimiento;
import co.tiendabarrio.producto.Producto;
import co.tiendabarrio.producto.ProductoRepository;

@Service
public class IngresoService {

    private final IngresoRepository ingresos;
    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;
    private final InventarioService inventario;

    public IngresoService(IngresoRepository ingresos, ProveedorRepository proveedores,
                          ProductoRepository productos, InventarioService inventario) {
        this.ingresos = ingresos;
        this.proveedores = proveedores;
        this.productos = productos;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public List<IngresoResponse> listar() {
        return ingresos.findAllByOrderByFechaHoraDescIdDesc().stream().map(IngresoResponse::de).toList();
    }

    /**
     * Registra la mercancía recibida y la suma al inventario (SWR-06).
     * Solo se aceptan productos que el proveedor tiene asociados (SWR-05).
     */
    @Transactional
    public IngresoResponse registrar(IngresoRequest datos) {
        Proveedor proveedor = proveedores.findById(datos.proveedorId())
                .orElseThrow(() -> new NoEncontradoException("Proveedor no encontrado"));
        String factura = datos.numeroFactura() == null || datos.numeroFactura().isBlank()
                ? null : datos.numeroFactura().trim();
        IngresoMercancia ingreso = ingresos.save(new IngresoMercancia(proveedor, factura));

        String referencia = "Ingreso #" + ingreso.getId() + " · " + proveedor.getNombre()
                + (factura == null ? "" : " · Factura " + factura);
        for (Map.Entry<Long, Integer> linea : LineaProductoRequest.agrupar(datos.lineas()).entrySet()) {
            Producto producto = productos.findById(linea.getKey())
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            if (!proveedor.suministra(producto)) {
                throw new NegocioException(producto.getNombre() + " no está asociado al proveedor "
                        + proveedor.getNombre());
            }
            inventario.registrarEntrada(producto, linea.getValue(), OrigenMovimiento.INGRESO_PROVEEDOR, referencia);
            ingreso.agregarLinea(new LineaProducto(producto, linea.getValue()));
        }
        return IngresoResponse.de(ingreso);
    }
}
