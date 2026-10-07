package co.tiendabarrio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Registro de cuándo un pedido a proveedor pasó a un estado. */
@Entity
public class CambioEstadoPedido {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoPedido estado;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    private String nota;

    protected CambioEstadoPedido() {
    }

    public CambioEstadoPedido(EstadoPedido estado, String nota) {
        this.estado = estado;
        this.fechaHora = LocalDateTime.now();
        this.nota = nota;
    }

    public EstadoPedido getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getNota() {
        return nota;
    }
}
