package co.tiendabarrio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/**
 * Aviso al dueño de que un producto llegó a su stock mínimo (SWR-07). Sigue activa hasta que el
 * producto se reabastece por encima del mínimo; entonces se marca como resuelta.
 */
@Entity
public class AlertaStock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @Column(nullable = false)
    private LocalDateTime creada = LocalDateTime.now();

    /** Stock del producto cuando se generó la alerta. */
    @Column(nullable = false)
    private int stockAlCrear;

    /** Cuándo el stock volvió a quedar por encima del mínimo; vacío mientras la alerta está activa. */
    private LocalDateTime resuelta;

    /** Si el dueño ya la vio en la campana de alertas. */
    @Column(nullable = false)
    private boolean vista;

    protected AlertaStock() {
    }

    public AlertaStock(Producto producto) {
        this.producto = producto;
        this.stockAlCrear = producto.getStockActual();
    }

    public void resolver() {
        resuelta = LocalDateTime.now();
    }

    public void marcarVista() {
        vista = true;
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public LocalDateTime getCreada() {
        return creada;
    }

    public int getStockAlCrear() {
        return stockAlCrear;
    }

    public LocalDateTime getResuelta() {
        return resuelta;
    }

    public boolean isVista() {
        return vista;
    }
}
