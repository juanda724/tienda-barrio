package co.tiendabarrio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.repository.ProductoRepository;

@Service
public class ProductoService {

    private final ProductoRepository productos;
    private final InventarioService inventario;

    public ProductoService(ProductoRepository productos, InventarioService inventario) {
        this.productos = productos;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public List<ProductoResponse> listar() {
        return productos.findAllByOrderByNombreAsc().stream().map(ProductoResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProductoResponse obtener(Long id) {
        return ProductoResponse.de(buscar(id));
    }

    /** Crea el producto; el stock inicial entra como movimiento para que quede en el historial. */
    @Transactional
    public ProductoResponse crear(ProductoRequest datos) {
        String nombre = datos.nombre().trim();
        if (productos.existsByNombreIgnoreCase(nombre)) {
            throw new NegocioException("Ya existe un producto llamado " + nombre);
        }
        Producto producto = productos.save(new Producto(nombre, limpiar(datos.categoria()), datos.stockMinimo()));
        if (datos.stockInicial() != null && datos.stockInicial() > 0) {
            inventario.registrarEntrada(producto, datos.stockInicial(), OrigenMovimiento.INVENTARIO_INICIAL,
                    "Inventario inicial");
        }
        return ProductoResponse.de(producto);
    }

    /** Edita los datos descriptivos. El stock no se edita aquí: solo cambia con movimientos. */
    @Transactional
    public ProductoResponse actualizar(Long id, ProductoRequest datos) {
        Producto producto = buscar(id);
        String nombre = datos.nombre().trim();
        if (productos.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NegocioException("Ya existe un producto llamado " + nombre);
        }
        producto.setNombre(nombre);
        producto.setCategoria(limpiar(datos.categoria()));
        producto.setStockMinimo(datos.stockMinimo());
        return ProductoResponse.de(productos.save(producto));
    }

    private Producto buscar(Long id) {
        return productos.findById(id).orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
