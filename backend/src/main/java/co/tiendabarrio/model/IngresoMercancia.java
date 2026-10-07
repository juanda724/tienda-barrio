package co.tiendabarrio.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.tiendabarrio.exception.NegocioException;
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
import jakarta.persistence.OrderBy;

/**
 * Recepción de mercancía de un proveedor. Es la única entrada de stock: lo recibido se suma al
 * inventario (SWR-03, SWR-06). Puede venir de un pedido a proveedor, que queda entregado.
 *
 * <p>F-06: cada línea confronta lo facturado con lo recibido (SWR-15) y el ingreso guarda el resultado
 * (SWR-17). Mientras haya diferencias sin resolver no se puede pagar ni acordar crédito (SWR-16, RN-01).
 * F-07: tampoco mientras haya una devolución en curso; las notas crédito se descuentan del pago.
 * Valores en pesos enteros.
 */
@Entity
public class IngresoMercancia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Proveedor proveedor;

    @Column(nullable = false)
    private LocalDateTime fechaHora = LocalDateTime.now();

    private String numeroFactura;

    /** Pedido que se recibe con este ingreso; vacío si la mercancía llegó sin pedido previo. */
    @ManyToOne(fetch = FetchType.LAZY)
    private PedidoProveedor pedido;

    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @JoinColumn(name = "ingreso_id")
    private List<LineaProducto> lineas = new ArrayList<>();

    // Los campos siguientes pueden faltar en ingresos registrados antes de F-06

    @Enumerated(EnumType.STRING)
    private ResultadoVerificacion resultadoVerificacion;

    /** Cómo se resolvieron las diferencias, p. ej. "El proveedor envió nota crédito". */
    private String notaVerificacion;

    @Enumerated(EnumType.STRING)
    private EstadoPago estadoPago;

    /** Fecha límite para pagar un ingreso a crédito. */
    private LocalDate fechaVencimiento;

    private LocalDateTime fechaPago;

    @Enumerated(EnumType.STRING)
    private FormaPago formaPagoPago;

    private Long montoPagado;

    @OneToMany(mappedBy = "ingreso")
    @OrderBy("fechaHora ASC, id ASC")
    private List<Devolucion> devoluciones = new ArrayList<>();

    protected IngresoMercancia() {
    }

    public IngresoMercancia(Proveedor proveedor, String numeroFactura, PedidoProveedor pedido) {
        this.proveedor = proveedor;
        this.numeroFactura = numeroFactura;
        this.pedido = pedido;
        this.estadoPago = EstadoPago.PENDIENTE;
    }

    public void agregarLinea(LineaProducto linea) {
        lineas.add(linea);
    }

    /** Mantiene la lista en memoria al registrar una devolución de este ingreso. */
    public void agregarDevolucion(Devolucion devolucion) {
        devoluciones.add(devolucion);
    }

    /** Confronta factura y recibido en todas las líneas y guarda el resultado (SWR-15, SWR-17). */
    public void verificar() {
        boolean coincide = lineas.stream().allMatch(l -> l.getDiferencia() == 0);
        resultadoVerificacion = coincide ? ResultadoVerificacion.APROBADO : ResultadoVerificacion.CON_DIFERENCIAS;
    }

    /** El proveedor ajustó la factura a lo recibido: se pagará solo lo que llegó. */
    public void resolverDiferencias(String nota) {
        if (resultadoVerificacion != ResultadoVerificacion.CON_DIFERENCIAS) {
            throw new NegocioException("El ingreso #" + id + " no tiene diferencias por resolver");
        }
        resultadoVerificacion = ResultadoVerificacion.DIFERENCIAS_RESUELTAS;
        notaVerificacion = nota;
    }

    /** Paga el total al proveedor, de contado o saldando un crédito. */
    public void pagar(FormaPago formaPago) {
        if (formaPago == FormaPago.CREDITO) {
            throw new NegocioException("Para pagar a crédito use \"Acordar crédito\"");
        }
        validarQueSePuedePagar();
        estadoPago = EstadoPago.PAGADO;
        fechaPago = LocalDateTime.now();
        formaPagoPago = formaPago;
        montoPagado = getTotalAPagar();
    }

    /** Acuerdo de pago a crédito con el proveedor: queda por pagar hasta la fecha de vencimiento. */
    public void acordarCredito(LocalDate vencimiento) {
        validarQueSePuedePagar();
        if (getEstadoPago() == EstadoPago.CREDITO) {
            throw new NegocioException("El ingreso #" + id + " ya tiene un crédito acordado");
        }
        if (vencimiento.isBefore(LocalDate.now())) {
            throw new NegocioException("La fecha de vencimiento no puede estar en el pasado");
        }
        estadoPago = EstadoPago.CREDITO;
        fechaVencimiento = vencimiento;
    }

    /** SWR-16 y RN-01: no se paga lo que no se ha verificado sin diferencias ni lo que está en devolución. */
    private void validarQueSePuedePagar() {
        Devolucion enCurso = getDevolucionEnCurso();
        if (enCurso != null && getEstadoPago() != EstadoPago.PAGADO) {
            throw new NegocioException("El ingreso #" + id + " tiene la devolución #" + enCurso.getId()
                    + " en curso. Resuélvala antes de pagar (RN-01)");
        }
        if (resultadoVerificacion == ResultadoVerificacion.CON_DIFERENCIAS) {
            throw new NegocioException("El ingreso #" + id + " tiene diferencias entre la factura y lo recibido. "
                    + "Resuélvalas antes de pagar (RN-01)");
        }
        if (getEstadoPago() == EstadoPago.PAGADO) {
            throw new NegocioException("El ingreso #" + id + " ya está pagado");
        }
    }

    public boolean isConDiferencias() {
        return resultadoVerificacion == ResultadoVerificacion.CON_DIFERENCIAS;
    }

    public boolean isPagable() {
        return !isConDiferencias() && getDevolucionEnCurso() == null && getEstadoPago() != EstadoPago.PAGADO;
    }

    /** La primera devolución de este ingreso que aún no se resuelve, o null. */
    public Devolucion getDevolucionEnCurso() {
        return devoluciones.stream().filter(d -> d.getEstado().enCurso()).findFirst().orElse(null);
    }

    /** Suma de las notas crédito que el proveedor reconoció por devoluciones de este ingreso. */
    public long getTotalCreditoDevoluciones() {
        return devoluciones.stream().mapToLong(d -> d.getMontoCredito() == null ? 0 : d.getMontoCredito()).sum();
    }

    /** Unidades de un producto ya devueltas en devoluciones de este ingreso. */
    public int unidadesDevueltas(Long productoId) {
        return devoluciones.stream().flatMap(d -> d.getLineas().stream())
                .filter(l -> l.getProducto().getId().equals(productoId)).mapToInt(LineaDevolucion::getCantidad).sum();
    }

    /** Un crédito cuya fecha de vencimiento ya pasó sin pagarse. */
    public boolean isVencido() {
        return getEstadoPago() == EstadoPago.CREDITO && fechaVencimiento != null
                && fechaVencimiento.isBefore(LocalDate.now());
    }

    public long getTotalFacturado() {
        return lineas.stream().mapToLong(LineaProducto::getSubtotalFacturado).sum();
    }

    /** Valor de lo que realmente llegó: cantidad recibida × costo unitario. */
    public long getTotalRecibido() {
        return lineas.stream().mapToLong(LineaProducto::getSubtotal).sum();
    }

    /**
     * Lo facturado (o solo lo recibido si se resolvieron diferencias ajustando la factura), menos las
     * notas crédito de devoluciones.
     */
    public long getTotalAPagar() {
        long base = resultadoVerificacion == ResultadoVerificacion.DIFERENCIAS_RESUELTAS
                ? getTotalRecibido() : getTotalFacturado();
        return Math.max(base - getTotalCreditoDevoluciones(), 0);
    }

    public Long getId() {
        return id;
    }

    public Proveedor getProveedor() {
        return proveedor;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    public String getNumeroFactura() {
        return numeroFactura;
    }

    public PedidoProveedor getPedido() {
        return pedido;
    }

    public List<LineaProducto> getLineas() {
        return lineas;
    }

    public ResultadoVerificacion getResultadoVerificacion() {
        return resultadoVerificacion;
    }

    public String getNotaVerificacion() {
        return notaVerificacion;
    }

    public EstadoPago getEstadoPago() {
        return estadoPago == null ? EstadoPago.PENDIENTE : estadoPago;
    }

    public LocalDate getFechaVencimiento() {
        return fechaVencimiento;
    }

    public LocalDateTime getFechaPago() {
        return fechaPago;
    }

    public FormaPago getFormaPagoPago() {
        return formaPagoPago;
    }

    public Long getMontoPagado() {
        return montoPagado;
    }

    public List<Devolucion> getDevoluciones() {
        return devoluciones;
    }
}
