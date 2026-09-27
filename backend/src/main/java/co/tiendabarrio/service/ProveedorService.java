package co.tiendabarrio.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.ProveedorRepository;

@Service
public class ProveedorService {

    private final ProveedorRepository proveedores;
    private final ProductoRepository productos;

    public ProveedorService(ProveedorRepository proveedores, ProductoRepository productos) {
        this.proveedores = proveedores;
        this.productos = productos;
    }

    @Transactional(readOnly = true)
    public List<ProveedorResponse> listar() {
        return proveedores.findAllByOrderByNombreAsc().stream().map(ProveedorResponse::de).toList();
    }

    @Transactional(readOnly = true)
    public ProveedorResponse obtener(Long id) {
        return ProveedorResponse.de(buscar(id));
    }

    @Transactional
    public ProveedorResponse crear(ProveedorRequest datos) {
        String nombre = datos.nombre().trim();
        if (proveedores.existsByNombreIgnoreCase(nombre)) {
            throw new NegocioException("Ya existe un proveedor llamado " + nombre);
        }
        return ProveedorResponse.de(proveedores.save(aplicar(new Proveedor(nombre), datos)));
    }

    @Transactional
    public ProveedorResponse actualizar(Long id, ProveedorRequest datos) {
        Proveedor proveedor = buscar(id);
        String nombre = datos.nombre().trim();
        if (proveedores.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new NegocioException("Ya existe un proveedor llamado " + nombre);
        }
        proveedor.setNombre(nombre);
        return ProveedorResponse.de(proveedores.save(aplicar(proveedor, datos)));
    }

    private Proveedor buscar(Long id) {
        return proveedores.findById(id).orElseThrow(() -> new NoEncontradoException("Proveedor no encontrado"));
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
