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

    protected Abono() {
    }

    public Abono(Cliente cliente, long monto, FormaPago formaPago, String nota) {
        this.cliente = cliente;
        this.monto = monto;
        this.formaPago = formaPago;
        this.nota = nota;
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
