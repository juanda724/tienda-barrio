package co.tiendabarrio.model;

import java.math.BigDecimal;

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

    /** Precio al que se vende el producto (SWR-13). Nulo en bases de datos anteriores: se toma como 0. */
    @Column(precision = 14, scale = 2)
    private BigDecimal precioVenta;

    /** Lo que cuesta el producto a la tienda (SWR-13). */
    @Column(precision = 14, scale = 2)
    private BigDecimal costo;

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

    public BigDecimal getPrecioVenta() {
        return precioVenta == null ? BigDecimal.ZERO : precioVenta;
    }

    public void setPrecioVenta(BigDecimal precioVenta) {
        this.precioVenta = precioVenta;
    }

    public BigDecimal getCosto() {
        return costo == null ? BigDecimal.ZERO : costo;
    }

    public void setCosto(BigDecimal costo) {
        this.costo = costo;
    }
}
