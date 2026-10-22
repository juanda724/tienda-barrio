package co.tiendabarrio.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;

/** Línea persistida de una venta, un pedido a proveedor o un ingreso de mercancía. */
@Entity
public class LineaProducto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Producto producto;

    @Column(nullable = false)
    private int cantidad;

    /**
     * Precio por unidad. En una venta, el cobrado al cliente (copiado del producto para que un cambio
     * de precio posterior no altere ventas ya hechas). En un ingreso, el costo facturado por el proveedor.
     * Vacío en los pedidos a proveedor.
     */
    private Long precioUnitario;

    /**
     * Solo en ingresos de mercancía: unidades que cobra la factura del proveedor, para compararlas con
     * las recibidas, que son las de "cantidad" (SWR-15).
     */
    private Integer cantidadFacturada;

    /**
     * Solo en ingresos de mercancía: unidades faltantes que el proveedor entregó después del ingreso.
     * Se guardan aparte de "cantidad" para que se vea qué llegó el primer día y qué llegó después.
     */
    private Integer cantidadEntregadaDespues;

    protected LineaProducto() {
    }

    public LineaProducto(Producto producto, int cantidad) {
        this(producto, cantidad, null);
    }

    public LineaProducto(Producto producto, int cantidad, Long precioUnitario) {
        this.producto = producto;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
    }

    /** Línea de un ingreso: lo recibido, lo facturado y el costo unitario de la factura. */
    public static LineaProducto deIngreso(Producto producto, int recibida, int facturada, long costoUnitario) {
        LineaProducto linea = new LineaProducto(producto, recibida, costoUnitario);
        linea.cantidadFacturada = facturada;
        return linea;
    }

    public Producto getProducto() {
        return producto;
    }

    public int getCantidad() {
        return cantidad;
    }

    public Long getPrecioUnitario() {
        return precioUnitario;
    }

    /** cantidad × precio unitario; 0 si la línea no tiene precio (pedidos a proveedor). */
    public long getSubtotal() {
        return precioUnitario == null ? 0 : precioUnitario * cantidad;
    }

    /** Unidades facturadas; en líneas anteriores a la verificación se asume lo recibido. */
    public int getCantidadFacturada() {
        return cantidadFacturada == null ? cantidad : cantidadFacturada;
    }

    public int getCantidadEntregadaDespues() {
        return cantidadEntregadaDespues == null ? 0 : cantidadEntregadaDespues;
    }

    /** Lo recibido con el ingreso más los faltantes entregados después. */
    public int getCantidadRecibidaTotal() {
        return cantidad + getCantidadEntregadaDespues();
    }

    /** Suma unidades faltantes que el proveedor entregó después del ingreso. */
    public void registrarEntregaPosterior(int unidades) {
        cantidadEntregadaDespues = getCantidadEntregadaDespues() + unidades;
    }

    /** Recibido (contando entregas posteriores) menos facturado: negativo si faltan unidades, positivo si sobran. */
    public int getDiferencia() {
        return getCantidadRecibidaTotal() - getCantidadFacturada();
    }

    /** Valor de lo recibido, contando entregas posteriores: cantidad recibida total × costo unitario. */
    public long getSubtotalRecibido() {
        return precioUnitario == null ? 0 : precioUnitario * getCantidadRecibidaTotal();
    }

    /** Valor de lo facturado: cantidad facturada × costo unitario. */
    public long getSubtotalFacturado() {
        return precioUnitario == null ? 0 : precioUnitario * getCantidadFacturada();
    }
}
