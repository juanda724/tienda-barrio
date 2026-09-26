package co.tiendabarrio.producto;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.comun.NegocioException;
import co.tiendabarrio.comun.NoEncontradoException;
import co.tiendabarrio.inventario.InventarioService;
import co.tiendabarrio.inventario.OrigenMovimiento;

@Service
public class ProductoService {

    private final ProductoRepository productos;
    private final InventarioService inventario;

    public ProductoService(ProductoRepository productos, InventarioService inventario) {
        this.productos = productos;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public List<Producto> listar() {
        return productos.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Producto obtener(Long id) {
        return productos.findById(id).orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
    }

    /** Crea el producto; el stock inicial entra como movimiento para que quede en el historial. */
    @Transactional
    public Producto crear(ProductoRequest datos) {
        String nombre = datos.nombre().trim();
        if (productos.existsByNombreIgnoreCase(nombre)) {
            throw new NegocioException("Ya existe un producto llamado " + nombre);
        }
        Producto producto = productos.save(new Producto(nombre, limpiar(datos.categoria()), datos.stockMinimo()));
        if (datos.stockInicial() != null && datos.stockInicial() > 0) {
            inventario.registrarEntrada(producto, datos.stockInicial(), OrigenMovimiento.INVENTARIO_INICIAL,
                    "Inventario inicial");
        }
        return producto;
    }

    /** Edita los datos descriptivos. El stock no se edita aquí: solo cambia con movimientos. */
    @Transactional
    public Producto actualizar(Long id, ProductoRequest datos) {
        Producto producto = obtener(id);
        String nombre = datos.nombre().trim();
        if (productos.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NegocioException("Ya existe un producto llamado " + nombre);
        }
        producto.setNombre(nombre);
        producto.setCategoria(limpiar(datos.categoria()));
        producto.setStockMinimo(datos.stockMinimo());
        return productos.save(producto);
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
