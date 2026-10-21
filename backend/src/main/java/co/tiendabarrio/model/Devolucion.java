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
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;

/** Producto en mal estado o no conforme que se devuelve al proveedor (F-07). */
@Entity
public class Devolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // LAZY: el ingreso ya trae su propia lista de líneas y no se deben cargar dos listas a la vez.
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    private IngresoMercancia ingreso;

    @ManyToOne(optional = false)
    private Producto producto;

    @Column(nullable = false)
    private int cantidad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private MotivoDevolucion motivo;

    /** Evidencia escrita (SWR-18). */
    @Column(nullable = false, length = 1000)
    private String descripcion;

    /** Evidencia fotográfica opcional (SWR-18). */
    @Lob
    private byte[] foto;

    private String fotoTipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDevolucion estado = EstadoDevolucion.REGISTRADA;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    private LocalDateTime fechaResolucion;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "devolucion_id")
    private List<SeguimientoDevolucion> seguimiento = new ArrayList<>();

    protected Devolucion() {
    }

    public Devolucion(IngresoMercancia ingreso, Producto producto, int cantidad, MotivoDevolucion motivo,
                      String descripcion, byte[] foto, String fotoTipo) {
        this.ingreso = ingreso;
        this.producto = producto;
        this.cantidad = cantidad;
        this.motivo = motivo;
        this.descripcion = descripcion;
        this.foto = foto;
        this.fotoTipo = foto == null ? null : fotoTipo;
        this.seguimiento.add(new SeguimientoDevolucion(EstadoDevolucion.REGISTRADA, "Devolución registrada"));
    }

    /** Pasa a un nuevo estado y lo deja anotado en el seguimiento (SWR-20). */
    public void cambiarEstado(EstadoDevolucion nuevo, String nota) {
        this.estado = nuevo;
        if (nuevo.esFinal()) {
            this.fechaResolucion = LocalDateTime.now();
        }
        this.seguimiento.add(new SeguimientoDevolucion(nuevo, nota));
    }

    public String getNumero() {
        return String.format("ND-%06d", id);
    }

    public boolean tieneFoto() {
        return foto != null && foto.length > 0;
    }

    public Long getId() {
        return id;
    }

    public IngresoMercancia getIngreso() {
        return ingreso;
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

    public String getDescripcion() {
        return descripcion;
    }

    public byte[] getFoto() {
        return foto;
    }

    public String getFotoTipo() {
        return fotoTipo;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public LocalDateTime getFechaResolucion() {
        return fechaResolucion;
    }

    public List<SeguimientoDevolucion> getSeguimiento() {
        return seguimiento;
    }
}
