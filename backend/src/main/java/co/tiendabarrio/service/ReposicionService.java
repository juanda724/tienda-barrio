package co.tiendabarrio.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.ReposicionResponse;
import co.tiendabarrio.dto.response.ReposicionResponse.Grupo;
import co.tiendabarrio.model.EstadoPedido;
import co.tiendabarrio.model.PedidoProveedor;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.repository.PedidoProveedorRepository;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.ProveedorRepository;

/**
 * Genera la lista de pedido con los productos en o por debajo del stock mínimo (SWR-08). Cada
 * producto se asigna a su proveedor habitual: el último que se lo entregó o, si nunca se ha
 * recibido, el primero (por nombre) que lo suministra.
 */
@Service
public class ReposicionService {

    private static final EnumSet<EstadoPedido> ACTIVOS =
            EnumSet.of(EstadoPedido.EN_ESPERA, EstadoPedido.ACEPTADO, EstadoPedido.EN_PROCESO, EstadoPedido.EN_CAMINO);

    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;
    private final IngresoRepository ingresos;
    private final PedidoProveedorRepository pedidos;

    public ReposicionService(ProductoRepository productos, ProveedorRepository proveedores,
                             IngresoRepository ingresos, PedidoProveedorRepository pedidos) {
        this.productos = productos;
        this.proveedores = proveedores;
        this.ingresos = ingresos;
        this.pedidos = pedidos;
    }

    @Transactional(readOnly = true)
    public ReposicionResponse lista() {
        List<Proveedor> todos = proveedores.findAllByOrderByNombreAsc();
        Map<Proveedor, List<ReposicionResponse.Producto>> porProveedor = new LinkedHashMap<>();
        List<ReposicionResponse.Producto> sinProveedor = new ArrayList<>();

        for (Producto producto : productos.findAllByOrderByNombreAsc()) {
            if (!producto.isBajoMinimo()) {
                continue;
            }
            ReposicionResponse.Producto item = item(producto);
            proveedorHabitual(producto, todos).ifPresentOrElse(
                    p -> porProveedor.computeIfAbsent(p, k -> new ArrayList<>()).add(item),
                    () -> sinProveedor.add(item));
        }

        List<Grupo> grupos = porProveedor.entrySet().stream()
                .map(e -> new Grupo(e.getKey().getId(), e.getKey().getNombre(), e.getValue()))
                .sorted(Comparator.comparing(Grupo::proveedorNombre))
                .toList();
        return new ReposicionResponse(grupos, sinProveedor);
    }

    /** Lleva el stock al doble del mínimo; al menos una unidad. */
    static int cantidadSugerida(Producto producto) {
        return Math.max(2 * producto.getStockMinimo() - producto.getStockActual(), 1);
    }

    private ReposicionResponse.Producto item(Producto producto) {
        Long pendiente = pedidos.conProductoEnEstados(producto.getId(), ACTIVOS).stream()
                .findFirst().map(PedidoProveedor::getId).orElse(null);
        return new ReposicionResponse.Producto(producto.getId(), producto.getNombre(), producto.getStockActual(),
                producto.getStockMinimo(), cantidadSugerida(producto), pendiente);
    }

    private Optional<Proveedor> proveedorHabitual(Producto producto, List<Proveedor> todos) {
        Optional<Proveedor> ultimo = ingresos.proveedoresQueEntregaron(producto.getId()).stream()
                .filter(p -> p.suministra(producto))
                .findFirst();
        return ultimo.isPresent() ? ultimo : todos.stream().filter(p -> p.suministra(producto)).findFirst();
    }
}
