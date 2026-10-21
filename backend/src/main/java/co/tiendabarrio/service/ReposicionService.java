package co.tiendabarrio.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.ListaPedidoResponse;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.ProveedorRepository;

/** Arma la lista de pedido con los productos que están en su stock mínimo o por debajo (F-03, SWR-08). */
@Service
public class ReposicionService {

    private final ProductoRepository productos;
    private final ProveedorRepository proveedores;

    public ReposicionService(ProductoRepository productos, ProveedorRepository proveedores) {
        this.productos = productos;
        this.proveedores = proveedores;
    }

    /**
     * Agrupa los productos faltantes por proveedor (el primero, por nombre, que lo suministre)
     * para que el dueño sepa a quién pedirle cada cosa. Los productos sin proveedor van aparte.
     */
    @Transactional(readOnly = true)
    public ListaPedidoResponse listaPedido() {
        List<Producto> faltantes = productos.findAllByOrderByNombreAsc().stream()
                .filter(Producto::isBajoMinimo)
                .toList();
        List<Proveedor> todos = proveedores.findAllByOrderByNombreAsc();

        Map<Long, List<ListaPedidoResponse.Item>> porProveedor = new LinkedHashMap<>();
        List<ListaPedidoResponse.Item> sinProveedor = new ArrayList<>();
        for (Producto producto : faltantes) {
            ListaPedidoResponse.Item item = ListaPedidoResponse.Item.de(producto);
            Proveedor elegido = null;
            for (Proveedor proveedor : todos) {
                if (proveedor.suministra(producto)) {
                    elegido = proveedor;
                    break;
                }
            }
            if (elegido == null) {
                sinProveedor.add(item);
            } else {
                porProveedor.computeIfAbsent(elegido.getId(), id -> new ArrayList<>()).add(item);
            }
        }

        List<ListaPedidoResponse.Grupo> grupos = new ArrayList<>();
        for (Proveedor proveedor : todos) {
            List<ListaPedidoResponse.Item> items = porProveedor.get(proveedor.getId());
            if (items != null) {
                grupos.add(new ListaPedidoResponse.Grupo(proveedor.getId(), proveedor.getNombre(),
                        proveedor.getNombreContacto(), proveedor.getTelefono(), proveedor.getCorreo(), items));
            }
        }
        return new ListaPedidoResponse(LocalDateTime.now(), faltantes.size(), grupos, sinProveedor);
    }
}
