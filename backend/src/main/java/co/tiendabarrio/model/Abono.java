package co.tiendabarrio.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Pago parcial o total de un cliente sobre su deuda (F-08, SWR-23). */
@Entity
public class Abono {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Cliente cliente;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    private String nota;

    protected Abono() {
    }

    public Abono(Cliente cliente, BigDecimal monto, String nota) {
        this.cliente = cliente;
        this.monto = monto;
        this.nota = nota;
    }

    public Long getId() {
        return id;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getNota() {
        return nota;
    }
}
