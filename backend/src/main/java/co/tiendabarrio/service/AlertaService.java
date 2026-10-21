package co.tiendabarrio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.AlertaResponse;
import co.tiendabarrio.model.Alerta;
import co.tiendabarrio.model.EstadoAlerta;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.repository.AlertaRepository;

/**
 * Monitorea el stock de cada producto frente a su mínimo (F-03).
 * Genera una alerta automática cuando el producto llega al mínimo (SWR-07) y la resuelve
 * cuando el producto vuelve a superarlo. Nunca hay dos alertas activas del mismo producto.
 */
@Service
public class AlertaService {

    private final AlertaRepository alertas;

    public AlertaService(AlertaRepository alertas) {
        this.alertas = alertas;
    }

    /** Se llama después de cada cambio de stock o de stock mínimo. */
    @Transactional
    public void sincronizar(Producto producto) {
        boolean hayActiva = alertas.existsByProductoIdAndEstado(producto.getId(), EstadoAlerta.ACTIVA);
        if (producto.isBajoMinimo()) {
            if (!hayActiva) {
                alertas.save(new Alerta(producto));
            }
        } else if (hayActiva) {
            alertas.findByProductoIdAndEstado(producto.getId(), EstadoAlerta.ACTIVA).forEach(Alerta::resolver);
        }
    }

    /** Todas las alertas, o solo las de un estado. */
    @Transactional(readOnly = true)
    public List<AlertaResponse> listar(EstadoAlerta estado) {
        List<Alerta> lista = estado == null
                ? alertas.findAllByOrderByFechaHoraDescIdDesc()
                : alertas.findByEstadoOrderByFechaHoraDescIdDesc(estado);
        return lista.stream().map(AlertaResponse::de).toList();
    }
}
