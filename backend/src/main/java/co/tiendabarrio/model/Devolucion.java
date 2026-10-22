package co.tiendabarrio.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.tiendabarrio.exception.NegocioException;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;

/**
 * Devolución al proveedor de productos no conformes de un ingreso (F-07). Guarda la evidencia
 * escrita y fotográfica (SWR-18), sirve de base a la nota de devolución (SWR-19) y se sigue por
 * estados hasta su resolución (SWR-20). Las listas se cargan bajo demanda dentro de los servicios.
 */
@Entity
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private IngresoMercancia ingreso;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    /** Evidencia escrita: qué se encontró al recibir la mercancía. */
    @Column(nullable = false, length = 1000)
    private String descripcion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDevolucion estado = EstadoDevolucion.PENDIENTE;

    /** Valor reconocido por el proveedor cuando se resuelve con nota crédito. */
    private Long montoCredito;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "devolucion_id")
    private List<LineaDevolucion> lineas = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "devolucion_id")
    @OrderBy("subida ASC, id ASC")
    private List<EvidenciaFoto> fotos = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "devolucion_id")
    @OrderBy("fechaHora ASC, id ASC")
    private List<CambioEstadoDevolucion> historial = new ArrayList<>();

    protected Devolucion() {
    }

    public Devolucion(IngresoMercancia ingreso, String descripcion) {
        this.ingreso = ingreso;
        this.descripcion = descripcion;
        historial.add(new CambioEstadoDevolucion(EstadoDevolucion.PENDIENTE, "Devolución registrada"));
    }

    public void agregarLinea(LineaDevolucion linea) {
        lineas.add(linea);
    }

    public void agregarFoto(EvidenciaFoto foto) {
        fotos.add(foto);
    }

    /** Solo se permiten los cambios definidos en EstadoDevolucion. */
    public void cambiarEstado(EstadoDevolucion nuevo, String nota) {
        if (!estado.siguientes().contains(nuevo)) {
            throw new NegocioException("No se puede pasar la devolución de \"" + estado.getNombre() + "\" a \""
                    + nuevo.getNombre() + "\"");
        }
        estado = nuevo;
        if (nuevo == EstadoDevolucion.CON_NOTA_CREDITO) {
            montoCredito = getValor();
        }
        historial.add(new CambioEstadoDevolucion(nuevo, nota));
    }

    /** Valor de lo devuelto a costo de factura. */
    public long getValor() {
        return lineas.stream().mapToLong(LineaDevolucion::getValor).sum();
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

    public String getDescripcion() {
        return descripcion;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }

    public Long getMontoCredito() {
        return montoCredito;
    }

    public List<LineaDevolucion> getLineas() {
        return lineas;
    }

    public List<EvidenciaFoto> getFotos() {
        return fotos;
    }

    public List<CambioEstadoDevolucion> getHistorial() {
        return historial;
    }
}
