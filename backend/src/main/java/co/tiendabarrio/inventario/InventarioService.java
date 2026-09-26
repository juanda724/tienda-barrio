package co.tiendabarrio.inventario;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.comun.NegocioException;
import co.tiendabarrio.comun.NoEncontradoException;
import co.tiendabarrio.producto.Producto;
import co.tiendabarrio.producto.ProductoRepository;

/**
 * Único punto donde cambia el stock. Cada entrada o salida actualiza el producto y
 * deja un movimiento con fecha, hora y cantidad (F-01: SWR-01, SWR-03, SWR-04).
 */
@Service
public class InventarioService {

    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;

    public InventarioService(ProductoRepository productos, MovimientoRepository movimientos) {
        this.productos = productos;
        this.movimientos = movimientos;
    }

    @Transactional
    public MovimientoInventario registrarEntrada(Producto producto, int cantidad, OrigenMovimiento origen,
                                                 String referencia) {
        validarCantidad(cantidad);
        producto.aumentarStock(cantidad);
        return movimientos.save(new MovimientoInventario(producto, TipoMovimiento.ENTRADA, origen, cantidad, referencia));
    }

    @Transactional
    public MovimientoInventario registrarSalida(Producto producto, int cantidad, OrigenMovimiento origen,
                                                String referencia) {
        validarCantidad(cantidad);
        if (producto.getStockActual() < cantidad) {
            throw new NegocioException("Stock insuficiente de " + producto.getNombre() + ": disponible "
                    + producto.getStockActual() + ", solicitado " + cantidad);
        }
        producto.disminuirStock(cantidad);
        return movimientos.save(new MovimientoInventario(producto, TipoMovimiento.SALIDA, origen, cantidad, referencia));
    }

    /** Compra directa de un producto: incrementa su stock (SWR-03). */
    @Transactional
    public MovimientoInventario registrarCompra(CompraRequest compra) {
        Producto producto = productos.findById(compra.productoId())
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
        return registrarEntrada(producto, compra.cantidad(), OrigenMovimiento.COMPRA, compra.referencia());
    }

    @Transactional(readOnly = true)
    public List<MovimientoResponse> historial(Long productoId) {
        List<MovimientoInventario> lista = productoId == null
                ? movimientos.findAllByOrderByFechaHoraDescIdDesc()
                : movimientos.findByProductoIdOrderByFechaHoraDescIdDesc(productoId);
        return lista.stream().map(MovimientoResponse::de).toList();
    }

    private void validarCantidad(int cantidad) {
        if (cantidad <= 0) {
            throw new NegocioException("La cantidad debe ser mayor que cero");
        }
    }
}
