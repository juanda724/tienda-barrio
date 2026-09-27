package co.tiendabarrio.model;

import java.util.LinkedHashSet;
import java.util.Set;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderBy;

/** Proveedor con sus datos de contacto y los productos que suministra (SWR-05). */
@Entity
public class Proveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String nombreContacto;

    private String telefono;

    private String correo;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "proveedor_producto")
    @OrderBy("nombre")
    private Set<Producto> productos = new LinkedHashSet<>();

    protected Proveedor() {
    }

    public Proveedor(String nombre) {
        this.nombre = nombre;
    }

    public boolean suministra(Producto producto) {
        return productos.stream().anyMatch(p -> p.getId().equals(producto.getId()));
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public String getNombreContacto() {
        return nombreContacto;
    }

    public void setNombreContacto(String nombreContacto) {
        this.nombreContacto = nombreContacto;
    }

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public Set<Producto> getProductos() {
        return productos;
    }

    public void setProductos(Set<Producto> productos) {
        this.productos = productos;
    }
}
