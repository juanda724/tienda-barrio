package co.tiendabarrio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/** Un paso en la vida de una devolución (SWR-20). */
@Entity
public class SeguimientoDevolucion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoDevolucion estado;

    private String nota;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    protected SeguimientoDevolucion() {
    }

    public SeguimientoDevolucion(EstadoDevolucion estado, String nota) {
        this.estado = estado;
        this.nota = nota;
    }

    public EstadoDevolucion getEstado() {
        return estado;
    }

    public String getNota() {
        return nota;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }
}
