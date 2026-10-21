package co.tiendabarrio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Aviso al dueño de que un producto llegó a su stock mínimo (F-03, SWR-07). */
@Entity
public class Alerta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(nullable = false)
    private int stockAlGenerar;

    @Column(nullable = false)
    private int stockMinimo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoAlerta estado = EstadoAlerta.ACTIVA;

    private LocalDateTime fechaResolucion;

    protected Alerta() {
    }

    public Alerta(Producto producto) {
        this.producto = producto;
        this.stockAlGenerar = producto.getStockActual();
        this.stockMinimo = producto.getStockMinimo();
    }

    public void resolver() {
        this.estado = EstadoAlerta.RESUELTA;
        this.fechaResolucion = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public int getStockAlGenerar() {
        return stockAlGenerar;
    }

    public int getStockMinimo() {
        return stockMinimo;
    }

    public EstadoAlerta getEstado() {
        return estado;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }
}
