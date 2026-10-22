package co.tiendabarrio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Pago que un cliente hace a su deuda de ventas a crédito (SWR-23). Valores en pesos enteros. */
@Entity
public class Abono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Cliente cliente;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    @Column(nullable = false)
    private long monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FormaPago formaPago;

    private String nota;

    /** Efectivo que entregó el cliente; con él se calcula el cambio que le devuelve el dueño. */
    private Long montoEntregado;

    protected Abono() {
    }

    public Abono(Cliente cliente, long monto, FormaPago formaPago, String nota, Long montoEntregado) {
        this.cliente = cliente;
        this.monto = monto;
        this.formaPago = formaPago;
        this.nota = nota;
        this.montoEntregado = montoEntregado;
    }

    /** Cambio devuelto al cliente; null si no pagó en efectivo o no se indicó lo entregado. */
    public Long getCambio() {
        return montoEntregado == null ? null : montoEntregado - monto;
    }

    public Long getMontoEntregado() {
        return montoEntregado;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public long getMonto() {
        return monto;
    }

    public FormaPago getFormaPago() {
        return formaPago;
    }

    public String getNota() {
        return nota;
    }
}
