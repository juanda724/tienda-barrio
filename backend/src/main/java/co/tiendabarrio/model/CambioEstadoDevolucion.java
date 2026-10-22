package co.tiendabarrio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Registro de cuándo una devolución pasó a un estado (SWR-20). */
@Entity
public class CambioEstadoDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDevolucion estado;

    @Column(nullable = false)
    private LocalDateTime fechaHora;

    private String nota;

    protected CambioEstadoDevolucion() {
    }

    public CambioEstadoDevolucion(EstadoDevolucion estado, String nota) {
        this.estado = estado;
        this.fechaHora = LocalDateTime.now();
        this.nota = nota;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getNota() {
        return nota;
    }
}
