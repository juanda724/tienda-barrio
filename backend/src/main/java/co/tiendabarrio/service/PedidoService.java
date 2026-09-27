package co.tiendabarrio.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoRequest;
import co.tiendabarrio.dto.response.PedidoResponse;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.Pedido;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.repository.PedidoRepository;
import co.tiendabarrio.repository.ProductoRepository;

@Service
public class PedidoService {

    private final PedidoRepository pedidos;
    private final ProductoRepository productos;
    private final InventarioService inventario;

    public PedidoService(PedidoRepository pedidos, ProductoRepository productos, InventarioService inventario) {
        this.pedidos = pedidos;
        this.productos = productos;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public List<PedidoResponse> listar() {
        return pedidos.findAllByOrderByFechaHoraDescIdDesc().stream().map(PedidoResponse::de).toList();
    }

    /**
     * Registra el pedido y descarga cada producto del inventario (SWR-02).
     * Si algún producto no tiene stock suficiente, la transacción se revierte completa.
     */
    @Transactional
    public PedidoResponse registrar(PedidoRequest datos) {
        Pedido pedido = pedidos.save(new Pedido());
        String referencia = "Pedido #" + pedido.getId();
        for (Map.Entry<Long, Integer> linea : LineaProductoRequest.agrupar(datos.lineas()).entrySet()) {
            Producto producto = productos.findById(linea.getKey())
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            inventario.registrarSalida(producto, linea.getValue(), OrigenMovimiento.PEDIDO, referencia);
            pedido.agregarLinea(new LineaProducto(producto, linea.getValue()));
        }
        return PedidoResponse.de(pedido);
    }
}
