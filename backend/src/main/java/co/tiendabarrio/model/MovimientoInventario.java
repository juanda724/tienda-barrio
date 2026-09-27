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

/** Registro de una entrada o salida de producto con fecha, hora y cantidad (SWR-01, SWR-04). */
@Entity
public class MovimientoInventario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoMovimiento tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrigenMovimiento origen;

    @Column(nullable = false)
    private int cantidad;

    /** Stock del producto después de aplicar el movimiento. */
    @Column(nullable = false)
    private int stockResultante;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    /** Texto libre que identifica el documento de origen, p. ej. "Pedido #3". */
    private String referencia;

    protected MovimientoInventario() {
    }

    public MovimientoInventario(Producto producto, TipoMovimiento tipo, OrigenMovimiento origen,
                                int cantidad, String referencia) {
        this.producto = producto;
        this.tipo = tipo;
        this.origen = origen;
        this.cantidad = cantidad;
        this.stockResultante = producto.getStockActual();
        this.fechaHora = LocalDateTime.now();
        this.referencia = referencia;
    }

    public Long getId() {
        return id;
    }

    public Producto getProducto() {
        return producto;
    }

    public TipoMovimiento getTipo() {
        return tipo;
    }

    public OrigenMovimiento getOrigen() {
        return origen;
    }

    public int getCantidad() {
        return cantidad;
    }

    public int getStockResultante() {
        return stockResultante;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getReferencia() {
        return referencia;
    }
}
