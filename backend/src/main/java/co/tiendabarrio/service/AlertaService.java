package co.tiendabarrio.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.AlertaResponse;
import co.tiendabarrio.model.AlertaStock;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.repository.AlertaStockRepository;

/**
 * Monitorea el stock de cada producto contra su mínimo (SyR de F-03). Se llama después de cada
 * cambio de stock o de stock mínimo: abre una alerta cuando el producto llega al mínimo (SWR-07)
 * y la cierra cuando se reabastece. Nunca hay dos alertas activas del mismo producto.
 */
@Service
public class AlertaService {

    private final AlertaStockRepository alertas;

    public AlertaService(AlertaStockRepository alertas) {
        this.alertas = alertas;
    }

    @Transactional
    public void revisar(Producto producto) {
        var activa = alertas.findFirstByProductoIdAndResueltaIsNull(producto.getId());
        if (producto.isBajoMinimo() && activa.isEmpty()) {
            alertas.save(new AlertaStock(producto));
        } else if (!producto.isBajoMinimo()) {
            activa.ifPresent(AlertaStock::resolver);
        }
    }

    /** Alertas activas, la más reciente primero. */
    @Transactional(readOnly = true)
    public List<AlertaResponse> activas() {
        return alertas.findByResueltaIsNullOrderByCreadaDescIdDesc().stream().map(AlertaResponse::de).toList();
    }

    /** El dueño abrió la campana: las alertas activas dejan de contarse como nuevas. */
    @Transactional
    public List<AlertaResponse> marcarVistas() {
        List<AlertaStock> activas = alertas.findByResueltaIsNullOrderByCreadaDescIdDesc();
        activas.forEach(AlertaStock::marcarVista);
        return activas.stream().map(AlertaResponse::de).toList();
    }
}
