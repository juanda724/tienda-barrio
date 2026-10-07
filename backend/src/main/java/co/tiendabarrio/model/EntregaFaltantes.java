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

/**
 * Entrega posterior de productos que faltaron en un ingreso ("mañana le traigo lo que faltó"). Puede
 * ser parcial: un ingreso admite varias entregas hasta completar lo facturado.
 */
@Entity
public class EntregaFaltantes {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IngresoMercancia ingreso;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    private String nota;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "entrega_faltantes_id")
    private List<LineaProducto> lineas = new ArrayList<>();

    protected EntregaFaltantes() {
    }

    public EntregaFaltantes(IngresoMercancia ingreso, String nota) {
        this.ingreso = ingreso;
        this.nota = nota;
    }

    public void agregarLinea(Producto producto, int cantidad) {
        lineas.add(new LineaProducto(producto, cantidad));
    }

    public Long getId() {
        return id;
    }

    public IngresoMercancia getIngreso() {
        return ingreso;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getNota() {
        return nota;
    }

    public List<LineaProducto> getLineas() {
        return lineas;
    }
}
