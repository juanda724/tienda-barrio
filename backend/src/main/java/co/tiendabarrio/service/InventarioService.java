package co.tiendabarrio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.MovimientoResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.MovimientoInventario;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.TipoMovimiento;
import co.tiendabarrio.repository.MovimientoRepository;

/**
 * Único punto donde cambia el stock. Cada entrada o salida actualiza el producto y
 * deja un movimiento con fecha, hora y cantidad (F-01: SWR-01, SWR-04).
 * Las entradas llegan por el ingreso de mercancía y las salidas por las ventas. Tras cada cambio
 * revisa si el producto quedó en su stock mínimo (F-03).
 */
@Service
public class InventarioService {

    private final MovimientoRepository movimientos;
    private final AlertaService alertas;

    public InventarioService(MovimientoRepository movimientos, AlertaService alertas) {
        this.movimientos = movimientos;
        this.alertas = alertas;
    }

    /**
     * numeroDocumento es el número de la venta o del ingreso (null en el inventario inicial);
     * referencia es un texto opcional con más detalle del origen.
     */
    @Transactional
    public MovimientoInventario registrarEntrada(Producto producto, int cantidad, OrigenMovimiento origen,
                                                 Long numeroDocumento, String referencia) {
        validarCantidad(cantidad);
        producto.aumentarStock(cantidad);
        alertas.revisar(producto);
        return movimientos.save(new MovimientoInventario(producto, TipoMovimiento.ENTRADA, origen, cantidad,
                numeroDocumento, referencia));
    }

    @Transactional
    public MovimientoInventario registrarSalida(Producto producto, int cantidad, OrigenMovimiento origen,
                                                Long numeroDocumento, String referencia) {
        validarCantidad(cantidad);
        if (producto.getStockActual() < cantidad) {
            throw new NegocioException("Stock insuficiente de " + producto.getNombre() + ": disponible "
                    + producto.getStockActual() + ", solicitado " + cantidad);
        }
        producto.disminuirStock(cantidad);
        alertas.revisar(producto);
        return movimientos.save(new MovimientoInventario(producto, TipoMovimiento.SALIDA, origen, cantidad,
                numeroDocumento, referencia));
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
