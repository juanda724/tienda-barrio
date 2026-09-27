package co.tiendabarrio.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/** Recepción de un pedido del proveedor. Sus productos entran al inventario (SWR-06). */
@Entity
public class IngresoMercancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    private String numeroFactura;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingreso_id")
    private List<LineaProducto> lineas = new ArrayList<>();

    protected IngresoMercancia() {
    }

    public IngresoMercancia(Proveedor proveedor, String numeroFactura) {
        this.proveedor = proveedor;
        this.numeroFactura = numeroFactura;
    }

    public void agregarLinea(LineaProducto linea) {
        lineas.add(linea);
    }

    public Long getId() {
        return id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public List<LineaProducto> getLineas() {
        return lineas;
    }
}
