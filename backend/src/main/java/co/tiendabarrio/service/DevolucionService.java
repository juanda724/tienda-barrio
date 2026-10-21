package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.CambioEstadoRequest;
import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.response.DevolucionResponse;
import co.tiendabarrio.dto.response.FotoDevolucion;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Devolucion;
import co.tiendabarrio.model.EstadoDevolucion;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.DevolucionRepository;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.repository.ProductoRepository;

/**
 * Devoluciones de productos en mal estado al proveedor (F-07).
 * La devolución siempre nace de un ingreso de mercancía: el producto defectuoso sale del inventario al
 * registrarla y, si el proveedor lo reemplaza, vuelve a entrar al resolverla.
 */
@Service
public class DevolucionService {

    private static final int MAX_BYTES_FOTO = 2 * 1024 * 1024;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final DevolucionRepository devoluciones;
    private final IngresoRepository ingresos;
    private final ProductoRepository productos;
    private final InventarioService inventario;

    public DevolucionService(DevolucionRepository devoluciones, IngresoRepository ingresos,
                             ProductoRepository productos, InventarioService inventario) {
        this.devoluciones = devoluciones;
        this.ingresos = ingresos;
        this.productos = productos;
        this.inventario = inventario;
    }

    @Transactional(readOnly = true)
    public List<DevolucionResponse> listar() {
        return devoluciones.findAllByOrderByFechaHoraDescIdDesc().stream().map(DevolucionResponse::de).toList();
    }

    /** Registra el producto en mal estado con su evidencia y lo saca del inventario (SWR-18). */
    @Transactional
    public DevolucionResponse registrar(DevolucionRequest datos) {
        IngresoMercancia ingreso = ingresos.findById(datos.ingresoId())
                .orElseThrow(() -> new NoEncontradoException("Ingreso no encontrado"));
        Producto producto = productos.findById(datos.productoId())
                .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));

        int recibido = 0;
        for (LineaProducto linea : ingreso.getLineas()) {
            if (linea.getProducto().getId().equals(producto.getId())) {
                recibido += linea.getCantidad();
            }
        }
        if (recibido == 0) {
            throw new NegocioException(producto.getNombre() + " no fue recibido en el ingreso #" + ingreso.getId());
        }
        int yaDevuelto = devoluciones.findByIngresoIdAndProductoId(ingreso.getId(), producto.getId()).stream()
                .mapToInt(Devolucion::getCantidad).sum();
        int disponible = recibido - yaDevuelto;
        if (datos.cantidad() > disponible) {
            throw new NegocioException("Solo se pueden devolver " + disponible + " unidades de "
                    + producto.getNombre() + " de este ingreso");
        }

        Devolucion devolucion = devoluciones.save(new Devolucion(ingreso, producto, datos.cantidad(),
                datos.motivo(), datos.descripcion().trim(), decodificarFoto(datos), datos.fotoTipo()));
        inventario.registrarSalida(producto, datos.cantidad(), OrigenMovimiento.DEVOLUCION_PROVEEDOR,
                devolucion.getNumero() + " · Ingreso #" + ingreso.getId());
        return DevolucionResponse.de(devolucion);
    }

    /**
     * Hace avanzar la devolución: registrada → reclamada → reemplazada o acreditada (SWR-20).
     * Si el proveedor reemplaza el producto, las unidades vuelven a entrar al inventario.
     */
    @Transactional
    public DevolucionResponse cambiarEstado(Long id, CambioEstadoRequest datos) {
        Devolucion devolucion = buscar(id);
        EstadoDevolucion actual = devolucion.getEstado();
        EstadoDevolucion nuevo = datos.estado();
        boolean permitido = switch (actual) {
            case REGISTRADA -> nuevo == EstadoDevolucion.RECLAMADA || nuevo.esFinal();
            case RECLAMADA -> nuevo.esFinal();
            case REEMPLAZADA, ACREDITADA -> false;
        };
        if (!permitido) {
            throw new NegocioException("No se puede pasar la devolución " + devolucion.getNumero() + " de "
                    + actual + " a " + nuevo);
        }
        String nota = datos.nota() == null || datos.nota().isBlank() ? null : datos.nota().trim();
        devolucion.cambiarEstado(nuevo, nota);
        if (nuevo == EstadoDevolucion.REEMPLAZADA) {
            inventario.registrarEntrada(devolucion.getProducto(), devolucion.getCantidad(),
                    OrigenMovimiento.REEMPLAZO_PROVEEDOR, "Reemplazo de " + devolucion.getNumero());
        }
        return DevolucionResponse.de(devolucion);
    }

    /** Nota de devolución con el producto, la cantidad y el motivo, para enviarla al proveedor (SWR-19). */
    @Transactional(readOnly = true)
    public String nota(Long id) {
        Devolucion d = buscar(id);
        Proveedor proveedor = d.getIngreso().getProveedor();
        StringBuilder nota = new StringBuilder();
        nota.append("NOTA DE DEVOLUCIÓN ").append(d.getNumero()).append('\n');
        nota.append("Tienda de Barrio\n");
        nota.append("Fecha: ").append(d.getFechaHora().format(FORMATO_FECHA)).append('\n');
        nota.append("--------------------------------\n");
        nota.append("Proveedor: ").append(proveedor.getNombre()).append('\n');
        if (proveedor.getNombreContacto() != null) {
            nota.append("Contacto: ").append(proveedor.getNombreContacto()).append('\n');
        }
        nota.append("Teléfono: ").append(proveedor.getTelefono()).append('\n');
        nota.append("Ingreso de mercancía #").append(d.getIngreso().getId());
        if (d.getIngreso().getNumeroFactura() != null) {
            nota.append(" · Factura ").append(d.getIngreso().getNumeroFactura());
        }
        nota.append('\n');
        nota.append("--------------------------------\n");
        nota.append("Producto: ").append(d.getProducto().getNombre()).append('\n');
        nota.append("Cantidad: ").append(d.getCantidad()).append('\n');
        nota.append("Motivo: ").append(d.getMotivo().name().replace('_', ' ').toLowerCase()).append('\n');
        nota.append("Detalle: ").append(d.getDescripcion()).append('\n');
        nota.append("Evidencia fotográfica: ").append(d.tieneFoto() ? "adjunta" : "no adjunta").append('\n');
        nota.append("--------------------------------\n");
        nota.append("Estado: ").append(d.getEstado().name().toLowerCase()).append('\n');
        nota.append("Se solicita reemplazo o crédito por los productos indicados.");
        return nota.toString();
    }

    @Transactional(readOnly = true)
    public FotoDevolucion foto(Long id) {
        Devolucion d = buscar(id);
        if (!d.tieneFoto()) {
            throw new NoEncontradoException("La devolución no tiene foto");
        }
        return new FotoDevolucion(d.getFoto(), d.getFotoTipo());
    }

    private Devolucion buscar(Long id) {
        return devoluciones.findById(id).orElseThrow(() -> new NoEncontradoException("Devolución no encontrada"));
    }

    private byte[] decodificarFoto(DevolucionRequest datos) {
        if (datos.fotoBase64() == null || datos.fotoBase64().isBlank()) {
            return null;
        }
        if (datos.fotoTipo() == null || !datos.fotoTipo().startsWith("image/")) {
            throw new NegocioException("La evidencia fotográfica debe ser una imagen");
        }
        byte[] bytes;
        try {
            bytes = Base64.getDecoder().decode(datos.fotoBase64().trim());
        } catch (IllegalArgumentException e) {
            throw new NegocioException("La imagen no tiene un formato válido");
        }
        if (bytes.length > MAX_BYTES_FOTO) {
            throw new NegocioException("La imagen es demasiado pesada (máximo 2 MB)");
        }
        return bytes;
    }
}
