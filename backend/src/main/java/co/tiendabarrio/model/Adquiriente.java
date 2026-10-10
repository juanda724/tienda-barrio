package co.tiendabarrio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Cliente que pide factura electrónica (el "adquiriente"). Se guarda por su documento para no volver a
 * escribir sus datos la próxima vez que compre; si cambian, se actualizan con la nueva venta.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"tipoDocumento", "numeroDocumento"}))
public class Adquiriente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoDocumento tipoDocumento;

    @Column(nullable = false)
    private String numeroDocumento;

    /** Nombre completo, o razón social si es una empresa. */
    @Column(nullable = false)
    private String nombre;

    /** Correo al que se le envía la factura electrónica. */
    @Column(nullable = false)
    private String correo;

    private String telefono;

    private String direccion;

    private String ciudad;

    protected Adquiriente() {
    }

    public Adquiriente(TipoDocumento tipoDocumento, String numeroDocumento) {
        this.tipoDocumento = tipoDocumento;
        this.numeroDocumento = numeroDocumento;
    }

    public void actualizar(String nombre, String correo, String telefono, String direccion, String ciudad) {
        this.nombre = nombre;
        this.correo = correo;
        this.telefono = telefono;
        this.direccion = direccion;
        this.ciudad = ciudad;
    }

    public Long getId() {
        return id;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public String getNombre() {
        return nombre;
    }

    public String getCorreo() {
        return correo;
    }

    public String getTelefono() {
        return telefono;
    }

    public String getDireccion() {
        return direccion;
    }

    public String getCiudad() {
        return ciudad;
    }
}
