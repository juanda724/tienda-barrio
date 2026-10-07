package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.response.AlertaResponse;
import co.tiendabarrio.dto.response.ReposicionResponse;
import co.tiendabarrio.service.AlertaService;
import co.tiendabarrio.service.ReposicionService;

/** F-03: alertas de stock mínimo y lista de pedido automática. */
@RestController
@RequestMapping("/api")
public class ReposicionController {

    private final AlertaService alertas;
    private final ReposicionService reposicion;

    public ReposicionController(AlertaService alertas, ReposicionService reposicion) {
        this.alertas = alertas;
        this.reposicion = reposicion;
    }

    @GetMapping("/alertas")
    public List<AlertaResponse> alertasActivas() {
        return alertas.activas();
    }

    @PostMapping("/alertas/vistas")
    public List<AlertaResponse> marcarVistas() {
        return alertas.marcarVistas();
    }

    @GetMapping("/reposicion")
    public ReposicionResponse listaDePedido() {
        return reposicion.lista();
    }
}
