package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.response.AlertaResponse;
import co.tiendabarrio.dto.response.ListaPedidoResponse;
import co.tiendabarrio.model.EstadoAlerta;
import co.tiendabarrio.service.AlertaService;
import co.tiendabarrio.service.ReposicionService;

/** F-03: alertas de stock mínimo y lista de pedido automática. */
@RestController
@RequestMapping("/api")
public class AlertaController {

    private final AlertaService alertas;
    private final ReposicionService reposicion;

    public AlertaController(AlertaService alertas, ReposicionService reposicion) {
        this.alertas = alertas;
        this.reposicion = reposicion;
    }

    @GetMapping("/alertas")
    public List<AlertaResponse> listar(@RequestParam(required = false) EstadoAlerta estado) {
        return alertas.listar(estado);
    }

    @GetMapping("/reposicion/lista-pedido")
    public ListaPedidoResponse listaPedido() {
        return reposicion.listaPedido();
    }
}
