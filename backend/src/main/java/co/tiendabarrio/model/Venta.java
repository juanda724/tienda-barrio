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

/**
 * Venta a un cliente con fecha, hora, productos y valor total (SWR-11). Al registrarse, sus
 * productos salen del inventario (SWR-02). Los valores en dinero son pesos enteros.
 */
@Entity
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    // Pueden faltar en ventas registradas antes de F-05
    @Enumerated(EnumType.STRING)
    private FormaPago formaPago;

    /** Suma de los subtotales de las líneas, calculada por el sistema (SWR-12). */
    private Long total;

    /** Efectivo entregado por el cliente, para calcular el cambio; solo en pagos en efectivo. */
    private Long montoRecibido;

    /** Cliente al que se le fió; solo en ventas a crédito (SWR-21). */
    @ManyToOne
    private Cliente cliente;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "venta_id")
    private List<LineaProducto> lineas = new ArrayList<>();

    protected Venta() {
    }

    public Venta(FormaPago formaPago, Long montoRecibido, Cliente cliente) {
        this.formaPago = formaPago;
        this.montoRecibido = montoRecibido;
        this.cliente = cliente;
        this.total = 0L;
    }

    public void agregarLinea(LineaProducto linea) {
        lineas.add(linea);
        total = lineas.stream().mapToLong(LineaProducto::getSubtotal).sum();
    }

    /** Cambio a devolver al cliente; null si no pagó en efectivo o no se indicó lo recibido. */
    public Long getCambio() {
        return montoRecibido == null || total == null ? null : montoRecibido - total;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public Long getTotal() {
        return total;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public Long getMontoRecibido() {
        return montoRecibido;
    }

    public List<LineaProducto> getLineas() {
        return lineas;
    }
}
