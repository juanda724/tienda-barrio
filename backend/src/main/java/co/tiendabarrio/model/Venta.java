package co.tiendabarrio.model;

import java.math.BigDecimal;
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
 * Venta de la tienda (F-05, SWR-11). Puede ser de contado o a crédito (F-08): en ese caso
 * queda asociada a un cliente y suma a su saldo pendiente.
 */
@Entity
public class Venta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoPago tipoPago;

    @ManyToOne
    private Cliente cliente;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal total = BigDecimal.ZERO;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "venta_id")
    private List<LineaVenta> lineas = new ArrayList<>();

    protected Venta() {
    }

    public Venta(TipoPago tipoPago, Cliente cliente) {
        this.tipoPago = tipoPago;
        this.cliente = cliente;
    }

    /** Agrega la línea y recalcula el valor total de la venta (SWR-12). */
    public void agregarLinea(LineaVenta linea) {
        lineas.add(linea);
        total = total.add(linea.getSubtotal());
    }

    public BigDecimal getGanancia() {
        BigDecimal costo = lineas.stream().map(LineaVenta::getCostoTotal).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.subtract(costo);
    }

    public String getNumero() {
        return String.format("V-%06d", id);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public TipoPago getTipoPago() {
        return tipoPago;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public List<LineaVenta> getLineas() {
        return lineas;
    }
}
