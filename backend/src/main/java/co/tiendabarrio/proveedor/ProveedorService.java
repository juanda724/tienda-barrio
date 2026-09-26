package co.tiendabarrio.proveedor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.comun.NegocioException;
import co.tiendabarrio.comun.NoEncontradoException;
import co.tiendabarrio.producto.Producto;
import co.tiendabarrio.producto.ProductoRepository;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;

    public ProveedorService(ProveedorRepository proveedores, ProductoRepository productos) {
        this.proveedores = proveedores;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<Proveedor> listar() {
        return proveedores.findAllByOrderByNombreAsc();
    }

    @Transactional(readOnly = true)
    public Proveedor obtener(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new NoEncontradoException("Proveedor no encontrado"));
    }

    @Transactional
    public Proveedor crear(ProveedorRequest datos) {
        String nombre = datos.nombre().trim();
        if (proveedores.existsByNombreIgnoreCase(nombre)) {
            throw new NegocioException("Ya existe un proveedor llamado " + nombre);
        }
        return proveedores.save(aplicar(new Proveedor(nombre), datos));
    }

    @Transactional
    public Proveedor actualizar(Long id, ProveedorRequest datos) {
        Proveedor proveedor = obtener(id);
        String nombre = datos.nombre().trim();
        if (proveedores.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NegocioException("Ya existe un proveedor llamado " + nombre);
        }
        proveedor.setNombre(nombre);
        return proveedores.save(aplicar(proveedor, datos));
    }

    private Proveedor aplicar(Proveedor proveedor, ProveedorRequest datos) {
        proveedor.setNombreContacto(limpiar(datos.nombreContacto()));
        proveedor.setTelefono(datos.telefono().trim());
        proveedor.setCorreo(limpiar(datos.correo()));
        proveedor.setProductos(buscarProductos(datos.productoIds()));
        return proveedor;
    }

    private Set<Producto> buscarProductos(List<Long> ids) {
        Set<Producto> encontrados = new LinkedHashSet<>();
        if (ids == null) {
            return encontrados;
        }
        for (Long id : new LinkedHashSet<>(ids)) {
            encontrados.add(productos.findById(id)
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado: " + id)));
        }
        return encontrados;
    }

    private static String limpiar(String texto) {
        return texto == null || texto.isBlank() ? null : texto.trim();
    }
}
