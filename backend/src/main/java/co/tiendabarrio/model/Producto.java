package co.tiendabarrio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Version;

/**
 * Producto de la tienda. El stock solo cambia a través de InventarioService,
 * para que toda variación quede registrada como movimiento (SWR-01, SWR-04).
 * Los valores en dinero son pesos colombianos enteros (sin decimales).
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

    /** Precio al que se vende al cliente (SWR-13). Puede faltar en productos creados antes de F-05. */
    private Long precioVenta;

    /** Lo que le cuesta a la tienda comprarlo (SWR-13). */
    private Long costo;

    @Version
    private Long version;

    protected Producto() {
    }

    public Producto(String nombre, String categoria, int stockMinimo, Long precioVenta, Long costo) {
        this.nombre = nombre;
        this.categoria = categoria;
        this.stockMinimo = stockMinimo;
        this.precioVenta = precioVenta;
        this.costo = costo;
    }

    public void aumentarStock(int cantidad) {
        stockActual += cantidad;
    }

    public void disminuirStock(int cantidad) {
        stockActual -= cantidad;
    }

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

    public Long getPrecioVenta() {
        return precioVenta;
    }

    public void setPrecioVenta(Long precioVenta) {
        this.precioVenta = precioVenta;
    }

    public Long getCosto() {
        return costo;
    }

    public void setCosto(Long costo) {
        this.costo = costo;
    }
}
