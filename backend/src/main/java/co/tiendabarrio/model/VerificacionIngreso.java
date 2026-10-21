package co.tiendabarrio.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/** Resultado de una verificación de la factura contra lo recibido (F-06, SWR-15 y SWR-17). */
@Entity
public class VerificacionIngreso {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: el ingreso ya trae su propia lista de líneas y no se deben cargar dos listas a la vez.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IngresoMercancia ingreso;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoVerificacion resultado = EstadoVerificacion.PENDIENTE;

    private String observacion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "verificacion_id")
    private List<LineaVerificacion> lineas = new ArrayList<>();

    protected VerificacionIngreso() {
    }

    public VerificacionIngreso(IngresoMercancia ingreso, String observacion) {
        this.ingreso = ingreso;
        this.observacion = observacion;
    }

    public void agregarLinea(LineaVerificacion linea) {
        lineas.add(linea);
    }

    public void setResultado(EstadoVerificacion resultado) {
        this.resultado = resultado;
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

    public EstadoVerificacion getResultado() {
        return resultado;
    }

    public String getObservacion() {
        return observacion;
    }

    public List<LineaVerificacion> getLineas() {
        return lineas;
    }
}
