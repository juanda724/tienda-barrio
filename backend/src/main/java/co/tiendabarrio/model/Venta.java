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

    /** Cliente a cuyo nombre va la factura electrónica; vacío si no se pidió o si va a "Consumidor final". */
    @ManyToOne
    private Adquiriente adquiriente;

    /** Posición de la factura en la numeración autorizada; vacío si la venta no tiene factura electrónica. */
    private Long consecutivoFactura;

    /** Número con que se expidió la factura (FE-12); no se repite. */
    @Column(unique = true)
    private String numeroFactura;

    /** Código único de la factura, calculado al expedirla (SWR-32). */
    @Column(length = 96)
    private String cufe;

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

    /**
     * Expide la factura electrónica de la venta con el número que le corresponde en la resolución.
     * adquiriente es null si se factura a "Consumidor final".
     */
    public void expedirFacturaElectronica(Adquiriente adquiriente, long consecutivo, String numero, String cufe) {
        this.adquiriente = adquiriente;
        this.consecutivoFactura = consecutivo;
        this.numeroFactura = numero;
        this.cufe = cufe;
    }

    /**
     * Número de la factura electrónica; null si no se pidió. Las facturas anteriores a la numeración por
     * resolución no lo tienen guardado y conservan el que mostraban: FE-000012 para la venta 12.
     */
    public String getNumeroFacturaElectronica() {
        if (numeroFactura != null) {
            return numeroFactura;
        }
        return adquiriente == null || id == null ? null : String.format("FE-%06d", id);
    }

    public Adquiriente getAdquiriente() {
        return adquiriente;
    }

    /** CUFE de la factura; null si la venta no tiene factura o es anterior a la numeración por resolución. */
    public String getCufe() {
        return cufe;
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
