package co.tiendabarrio.producto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Producto de la tienda. El stock solo cambia a través de InventarioService,
 * para que toda variación quede registrada como movimiento (SWR-01, SWR-04).
 */
@Entity
public class Producto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String nombre;

    private String categoria;

    @Column(nullable = false)
    private int stockActual;

    @Column(nullable = false)
    private int stockMinimo;

    @Version
    private Long version;

    protected Producto() {
    }

    public Producto(String nombre, String categoria, int stockMinimo) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.stockMinimo = stockMinimo;
    }

    public void aumentarStock(int cantidad) {
        stockActual += cantidad;
    }

    public void disminuirStock(int cantidad) {
        stockActual -= cantidad;
    }

    @JsonProperty("bajoMinimo")
    public boolean isBajoMinimo() {
        return stockActual <= stockMinimo;
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

    public String getCategoria() {
        return categoria;
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public int getStockActual() {
        return stockActual;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public void setStockMinimo(int stockMinimo) {
        this.stockMinimo = stockMinimo;
    }
}
