package co.tiendabarrio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Producto devuelto: cuántas unidades, por qué y a qué costo se habían facturado. */
@Entity
public class LineaDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @Column(nullable = false)
    private int cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoDevolucion motivo;

    /** Costo unitario de la factura del ingreso, para valorar la nota crédito. */
    @Column(nullable = false)
    private long costoUnitario;

    protected LineaDevolucion() {
    }

    public LineaDevolucion(Producto producto, int cantidad, MotivoDevolucion motivo, long costoUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.costoUnitario = costoUnitario;
    }

    public long getValor() {
        return costoUnitario * cantidad;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public MotivoDevolucion getMotivo() {
        return motivo;
    }

    public long getCostoUnitario() {
        return costoUnitario;
    }
}
