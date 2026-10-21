package co.tiendabarrio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Comparación de un producto: cantidad facturada por el proveedor contra cantidad recibida (SWR-15). */
@Entity
public class LineaVerificacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @Column(nullable = false)
    private int cantidadFacturada;

    @Column(nullable = false)
    private int cantidadRecibida;

    protected LineaVerificacion() {
    }

    public LineaVerificacion(Producto producto, int cantidadFacturada, int cantidadRecibida) {
        this.producto = producto;
        this.cantidadFacturada = cantidadFacturada;
        this.cantidadRecibida = cantidadRecibida;
    }

    /** Positiva si el proveedor facturó más de lo que entregó. */
    public int getDiferencia() {
        return cantidadFacturada - cantidadRecibida;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidadFacturada() {
        return cantidadFacturada;
    }

    public int getCantidadRecibida() {
        return cantidadRecibida;
    }
}
