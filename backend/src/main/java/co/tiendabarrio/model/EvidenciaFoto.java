package co.tiendabarrio.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

/**
 * Foto que respalda una devolución (SWR-18). El archivo se guarda en la carpeta de evidencias
 * del backend; aquí solo queda su nombre y tipo.
 */
@Entity
public class EvidenciaFoto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Nombre del archivo dentro de la carpeta de evidencias (generado, único). */
    @Column(nullable = false, unique = true)
    private String archivo;

    @Column(nullable = false)
    private String tipoContenido;

    @Column(nullable = false)
    private LocalDateTime subida = LocalDateTime.now();

    protected EvidenciaFoto() {
    }

    public EvidenciaFoto(String archivo, String tipoContenido) {
        this.archivo = archivo;
        this.tipoContenido = tipoContenido;
    }

    public Long getId() {
        return id;
    }

    public String getArchivo() {
        return archivo;
    }

    public String getTipoContenido() {
        return tipoContenido;
    }

    public LocalDateTime getSubida() {
        return subida;
    }
}
