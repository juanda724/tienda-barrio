package co.tiendabarrio.model;

import java.time.LocalDate;
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
 * Pedido de mercancía que la tienda hace a un proveedor. Se envía por WhatsApp o correo
 * y su estado avanza hasta que la mercancía se recibe con un ingreso.
 * Las listas se cargan bajo demanda: los servicios las leen dentro de su transacción.
 */
@Entity
public class PedidoProveedor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDateTime fechaCreacion = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado = EstadoPedido.EN_ESPERA;

    private LocalDate fechaEstimadaEntrega;

    private String observaciones;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_proveedor_id")
    private List<LineaProducto> lineas = new ArrayList<>();

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "pedido_proveedor_id")
    @OrderBy("fechaHora ASC, id ASC")
    private List<CambioEstadoPedido> historial = new ArrayList<>();

    protected PedidoProveedor() {
    }

    public PedidoProveedor(Proveedor proveedor, LocalDate fechaEstimadaEntrega, String observaciones) {
        this.proveedor = proveedor;
        this.fechaEstimadaEntrega = fechaEstimadaEntrega;
        this.observaciones = observaciones;
        historial.add(new CambioEstadoPedido(EstadoPedido.EN_ESPERA, "Pedido creado"));
    }

    public void agregarLinea(LineaProducto linea) {
        lineas.add(linea);
    }

    /** Cambio manual de estado; solo se permiten los avances definidos en EstadoPedido. */
    public void cambiarEstado(EstadoPedido nuevo, String nota) {
        if (nuevo == EstadoPedido.ENTREGADO) {
            throw new NegocioException("Para marcar el pedido como entregado, registre el ingreso de mercancía");
        }
        if (!estado.siguientesManuales().contains(nuevo)) {
            throw new NegocioException("No se puede pasar el pedido de \"" + estado.getNombre() + "\" a \""
                    + nuevo.getNombre() + "\"");
        }
        registrar(nuevo, nota);
    }

    /** Lo usa el ingreso de mercancía: un pedido activo pasa directamente a ENTREGADO. */
    public void marcarEntregado(String nota) {
        validarQuePuedeRecibirse();
        registrar(EstadoPedido.ENTREGADO, nota);
    }

    /** Solo un pedido activo (no entregado, rechazado ni cancelado) puede recibirse. */
    public void validarQuePuedeRecibirse() {
        if (!estado.esActivo()) {
            throw new NegocioException("El pedido #" + id + " está " + estado.getNombre().toLowerCase()
                    + " y no puede recibirse");
        }
    }

    private void registrar(EstadoPedido nuevo, String nota) {
        estado = nuevo;
        historial.add(new CambioEstadoPedido(nuevo, nota));
    }

    public Long getId() {
        return id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public LocalDate getFechaEstimadaEntrega() {
        return fechaEstimadaEntrega;
    }

    public void setFechaEstimadaEntrega(LocalDate fechaEstimadaEntrega) {
        this.fechaEstimadaEntrega = fechaEstimadaEntrega;
    }

    public String getObservaciones() {
        return observaciones;
    }

    public List<LineaProducto> getLineas() {
        return lineas;
    }

    public List<CambioEstadoPedido> getHistorial() {
        return historial;
    }
}
