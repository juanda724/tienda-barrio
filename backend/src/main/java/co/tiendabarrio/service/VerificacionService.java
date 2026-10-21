package co.tiendabarrio.service;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.LineaFacturadaRequest;
import co.tiendabarrio.dto.request.VerificacionRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.VerificacionResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.exception.NoEncontradoException;
import co.tiendabarrio.model.EstadoPago;
import co.tiendabarrio.model.EstadoVerificacion;
import co.tiendabarrio.model.IngresoMercancia;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.LineaVerificacion;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.VerificacionIngreso;
import co.tiendabarrio.repository.IngresoRepository;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.VerificacionRepository;

/**
 * Confronta la factura del proveedor con lo físicamente recibido antes de pagar (F-06).
 * Flujo: ingreso de mercancía → verificación (APROBADO o CON_DIFERENCIAS) → confirmación de pago.
 */
@Service
public class VerificacionService {

    private final IngresoRepository ingresos;
    private final ProductoRepository productos;
    private final VerificacionRepository verificaciones;

    public VerificacionService(IngresoRepository ingresos, ProductoRepository productos,
                               VerificacionRepository verificaciones) {
        this.ingresos = ingresos;
        this.productos = productos;
        this.verificaciones = verificaciones;
    }

    @Transactional(readOnly = true)
    public List<VerificacionResponse> listar(Long ingresoId) {
        buscar(ingresoId);
        return verificaciones.findByIngresoIdOrderByFechaHoraDescIdDesc(ingresoId).stream()
                .map(VerificacionResponse::de).toList();
    }

    /**
     * Compara, producto por producto, lo facturado con lo recibido y guarda el resultado (SWR-15, SWR-17).
     * Un producto de la factura que no llegó cuenta como recibido = 0; uno recibido que no está en la
     * factura cuenta como facturado = 0. Se puede verificar de nuevo cuando el proveedor corrige la factura.
     */
    @Transactional
    public VerificacionResponse verificar(Long ingresoId, VerificacionRequest datos) {
        IngresoMercancia ingreso = buscar(ingresoId);
        if (ingreso.getEstadoPago() == EstadoPago.PAGADO) {
            throw new NegocioException("El ingreso #" + ingresoId + " ya fue pagado y no se puede verificar de nuevo");
        }

        Map<Long, Integer> recibido = new LinkedHashMap<>();
        for (LineaProducto linea : ingreso.getLineas()) {
            recibido.merge(linea.getProducto().getId(), linea.getCantidad(), Integer::sum);
        }
        Map<Long, Integer> facturado = new LinkedHashMap<>();
        for (LineaFacturadaRequest linea : datos.lineas()) {
            facturado.merge(linea.productoId(), linea.cantidadFacturada(), Integer::sum);
        }
        Set<Long> productoIds = new LinkedHashSet<>(recibido.keySet());
        productoIds.addAll(facturado.keySet());

        String observacion = datos.observacion() == null || datos.observacion().isBlank()
                ? null : datos.observacion().trim();
        VerificacionIngreso verificacion = new VerificacionIngreso(ingreso, observacion);
        boolean hayDiferencias = false;
        for (Long productoId : productoIds) {
            Producto producto = productos.findById(productoId)
                    .orElseThrow(() -> new NoEncontradoException("Producto no encontrado"));
            int cantidadFacturada = facturado.getOrDefault(productoId, 0);
            int cantidadRecibida = recibido.getOrDefault(productoId, 0);
            if (cantidadFacturada != cantidadRecibida) {
                hayDiferencias = true;
            }
            verificacion.agregarLinea(new LineaVerificacion(producto, cantidadFacturada, cantidadRecibida));
        }

        EstadoVerificacion resultado = hayDiferencias ? EstadoVerificacion.CON_DIFERENCIAS : EstadoVerificacion.APROBADO;
        verificacion.setResultado(resultado);
        ingreso.setEstadoVerificacion(resultado);
        return VerificacionResponse.de(verificaciones.save(verificacion));
    }

    /** Confirma el pago al proveedor. Se rechaza si la factura no coincide con lo recibido (SWR-16). */
    @Transactional
    public IngresoResponse confirmarPago(Long ingresoId) {
        IngresoMercancia ingreso = buscar(ingresoId);
        if (ingreso.getEstadoPago() == EstadoPago.PAGADO) {
            throw new NegocioException("El ingreso #" + ingresoId + " ya está pagado");
        }
        switch (ingreso.getEstadoVerificacion()) {
            case PENDIENTE -> throw new NegocioException(
                    "Antes de pagar debe verificar la factura contra los productos recibidos");
            case CON_DIFERENCIAS -> throw new NegocioException(
                    "No se puede confirmar el pago: la factura tiene diferencias con lo recibido. "
                            + "Resuélvalas con el proveedor y vuelva a verificar");
            case APROBADO -> ingreso.marcarPagado();
        }
        return IngresoResponse.de(ingreso);
    }

    private IngresoMercancia buscar(Long id) {
        return ingresos.findById(id).orElseThrow(() -> new NoEncontradoException("Ingreso no encontrado"));
    }
}
