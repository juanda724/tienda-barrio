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

    /** Nulo en ingresos de bases de datos anteriores: se toma como PENDIENTE. */
    @Enumerated(EnumType.STRING)
    private EstadoVerificacion estadoVerificacion = EstadoVerificacion.PENDIENTE;

    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago = EstadoPago.PENDIENTE;

    private LocalDateTime fechaPago;

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

    public EstadoVerificacion getEstadoVerificacion() {
        return estadoVerificacion == null ? EstadoVerificacion.PENDIENTE : estadoVerificacion;
    }

    public void setEstadoVerificacion(EstadoVerificacion estadoVerificacion) {
        this.estadoVerificacion = estadoVerificacion;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago == null ? EstadoPago.PENDIENTE : estadoPago;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    /** Marca el ingreso como pagado. Quien lo llame debe haber validado antes la verificación (SWR-16). */
    public void marcarPagado() {
        this.estadoPago = EstadoPago.PAGADO;
        this.fechaPago = LocalDateTime.now();
    }
}
