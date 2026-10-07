package co.tiendabarrio.service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import co.tiendabarrio.dto.request.CambioEstadoDevolucionRequest;
import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.request.LineaDevolucionRequest;
import co.tiendabarrio.dto.response.DevolucionResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.Devolucion;
import co.tiendabarrio.model.EstadoDevolucion;
import co.tiendabarrio.model.EvidenciaFoto;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.LineaDevolucion;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.model.Proveedor;
import co.tiendabarrio.repository.DevolucionRepository;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.util.Dinero;
import co.tiendabarrio.util.Enlaces;

/**
 * Devoluciones de productos no conformes al proveedor (F-07): registro con evidencia (SWR-18),
 * nota de devolución (SWR-19) y seguimiento hasta el reemplazo o la nota crédito (SWR-20).
 * Lo devuelto sale del inventario; si el proveedor lo reemplaza, vuelve a entrar.
 */
@Service
public class DevolucionService {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final int MAX_FOTOS = 6;

    private final DevolucionRepository devoluciones;
    private final IngresoRepository ingresos;
    private final InventarioService inventario;
    private final EvidenciaService evidencias;
    private final String nombreTienda;

    public DevolucionService(DevolucionRepository devoluciones, IngresoRepository ingresos,
                             InventarioService inventario, EvidenciaService evidencias,
                             @Value("${tienda.nombre:Tienda de Barrio}") String nombreTienda) {
        this.devoluciones = devoluciones;
        this.ingresos = ingresos;
        this.inventario = inventario;
        this.evidencias = evidencias;
        this.nombreTienda = nombreTienda;
    }

    @Transactional(readOnly = true)
    public List<DevolucionResponse> listar() {
        return devoluciones.findAllByOrderByFechaHoraDescIdDesc().stream().map(this::respuesta).toList();
    }

    @Transactional(readOnly = true)
    public DevolucionResponse obtener(Long id) {
        return respuesta(buscar(id));
    }

    /**
     * Registra la devolución de productos de un ingreso con su evidencia escrita (SWR-18). Solo se
     * puede devolver lo que llegó en ese ingreso y no se ha devuelto antes. Lo devuelto sale del stock
     * y el pago del ingreso queda bloqueado hasta resolver la devolución (RN-01).
     */
    @Transactional
    public DevolucionResponse registrar(DevolucionRequest datos) {
        IngresoMercancia ingreso = ingresos.findById(datos.ingresoId())
                .orElseThrow(() -> new NoEncontradoException("Ingreso de mercancía no encontrado"));

        // Primero se valida todo, para no tocar el stock si la devolución no puede hacerse
        Map<Long, Integer> pedidoAhora = new HashMap<>();
        Devolucion devolucion = new Devolucion(ingreso, datos.descripcion().trim());
        for (LineaDevolucionRequest l : datos.lineas()) {
            LineaProducto recibida = ingreso.getLineas().stream()
                    .filter(x -> x.getProducto().getId().equals(l.productoId()) && x.getCantidad() > 0)
                    .findFirst()
                    .orElseThrow(() -> new NegocioException("El producto no llegó en el ingreso #" + ingreso.getId()));
            int yaDevueltas = ingreso.unidadesDevueltas(l.productoId()) + pedidoAhora.getOrDefault(l.productoId(), 0);
            int disponibles = recibida.getCantidad() - yaDevueltas;
            if (l.cantidad() > disponibles) {
                throw new NegocioException("Del ingreso #" + ingreso.getId() + " solo se pueden devolver " + disponibles
                        + " unidades de " + recibida.getProducto().getNombre());
            }
            pedidoAhora.merge(l.productoId(), l.cantidad(), Integer::sum);
            long costo = recibida.getPrecioUnitario() != null ? recibida.getPrecioUnitario()
                    : recibida.getProducto().getCosto() == null ? 0 : recibida.getProducto().getCosto();
            devolucion.agregarLinea(new LineaDevolucion(recibida.getProducto(), l.cantidad(), l.motivo(), costo));
        }

        devoluciones.save(devolucion);
        ingreso.agregarDevolucion(devolucion);
        String referencia = "Ingreso #" + ingreso.getId() + " · " + ingreso.getProveedor().getNombre();
        for (LineaDevolucion linea : devolucion.getLineas()) {
            inventario.registrarSalida(linea.getProducto(), linea.getCantidad(), OrigenMovimiento.DEVOLUCION_PROVEEDOR,
                    devolucion.getId(), referencia);
        }
        return respuesta(devolucion);
    }

    /** Agrega fotos de evidencia a la devolución (SWR-18). */
    @Transactional
    public DevolucionResponse agregarFotos(Long id, List<MultipartFile> archivos) {
        Devolucion devolucion = buscar(id);
        if (archivos == null || archivos.isEmpty()) {
            throw new NegocioException("Seleccione al menos una foto");
        }
        if (devolucion.getFotos().size() + archivos.size() > MAX_FOTOS) {
            throw new NegocioException("Cada devolución admite hasta " + MAX_FOTOS + " fotos");
        }
        archivos.forEach(a -> devolucion.agregarFoto(evidencias.guardar(a)));
        devoluciones.flush(); // asigna el id de cada foto antes de armar sus enlaces en la respuesta
        return respuesta(devolucion);
    }

    @Transactional(readOnly = true)
    public EvidenciaFoto foto(Long id, Long fotoId) {
        return buscar(id).getFotos().stream().filter(f -> f.getId().equals(fotoId)).findFirst()
                .orElseThrow(() -> new NoEncontradoException("Foto no encontrada"));
    }

    public byte[] leer(EvidenciaFoto foto) {
        return evidencias.leer(foto);
    }

    /**
     * Avanza el seguimiento de la devolución (SWR-20). Con reemplazo, los productos nuevos entran
     * al inventario; con nota crédito, su valor se descuenta del pago del ingreso.
     */
    @Transactional
    public DevolucionResponse cambiarEstado(Long id, CambioEstadoDevolucionRequest datos) {
        Devolucion devolucion = buscar(id);
        String nota = datos.nota() == null || datos.nota().isBlank() ? null : datos.nota().trim();
        devolucion.cambiarEstado(datos.estado(), nota);
        if (datos.estado() == EstadoDevolucion.REEMPLAZADA) {
            String referencia = "Reemplazo de la devolución #" + devolucion.getId();
            for (LineaDevolucion linea : devolucion.getLineas()) {
                inventario.registrarEntrada(linea.getProducto(), linea.getCantidad(),
                        OrigenMovimiento.REEMPLAZO_DEVOLUCION, devolucion.getId(), referencia);
            }
        }
        return respuesta(devolucion);
    }

    private Devolucion buscar(Long id) {
        return devoluciones.findById(id).orElseThrow(() -> new NoEncontradoException("Devolución no encontrada"));
    }

    private DevolucionResponse respuesta(Devolucion d) {
        String nota = nota(d);
        Proveedor proveedor = d.getIngreso().getProveedor();
        String whatsapp = proveedor.getTelefono() == null ? null : Enlaces.whatsapp(proveedor.getTelefono(), nota);
        String correo = Enlaces.correo(proveedor.getCorreo(), "Nota de devolución #" + d.getId() + " - " + nombreTienda, nota);
        return DevolucionResponse.de(d, nota, whatsapp, correo);
    }

    /** Nota de devolución con producto, cantidad y motivo de la inconformidad (SWR-19). */
    String nota(Devolucion d) {
        IngresoMercancia ingreso = d.getIngreso();
        StringBuilder t = new StringBuilder()
                .append("NOTA DE DEVOLUCIÓN #").append(d.getId()).append('\n')
                .append(nombreTienda).append(" · ").append(d.getFechaHora().format(FECHA)).append('\n')
                .append("Proveedor: ").append(ingreso.getProveedor().getNombre()).append('\n')
                .append("Ingreso #").append(ingreso.getId());
        if (ingreso.getNumeroFactura() != null) {
            t.append(" · Factura ").append(ingreso.getNumeroFactura());
        }
        t.append("\n\nProductos devueltos:\n");
        for (LineaDevolucion l : d.getLineas()) {
            t.append("- ").append(l.getCantidad()).append(" x ").append(l.getProducto().getNombre())
                    .append(" — ").append(l.getMotivo().getNombre())
                    .append(" (").append(Dinero.formatear(l.getValor())).append(")\n");
        }
        t.append("\nValor total: ").append(Dinero.formatear(d.getValor())).append('\n')
                .append("Detalle: ").append(d.getDescripcion()).append('\n');
        if (!d.getFotos().isEmpty()) {
            t.append("Evidencia: ").append(d.getFotos().size()).append(d.getFotos().size() == 1 ? " foto" : " fotos")
                    .append(" registradas en la tienda.\n");
        }
        return t.append("\nSolicitamos el reemplazo de estos productos o una nota crédito por su valor. ¡Gracias!")
                .toString();
    }
}
