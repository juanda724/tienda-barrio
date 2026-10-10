package co.tiendabarrio.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.response.AdquirienteResponse;
import co.tiendabarrio.model.TipoDocumento;
import co.tiendabarrio.service.AdquirienteService;

@RestController
@RequestMapping("/api/adquirientes")
public class AdquirienteController {

    private final AdquirienteService adquirientes;

    public AdquirienteController(AdquirienteService adquirientes) {
        this.adquirientes = adquirientes;
    }

    /** Datos guardados del cliente con ese documento; 204 si nunca ha pedido factura electrónica. */
    @GetMapping("/buscar")
    public ResponseEntity<AdquirienteResponse> buscar(@RequestParam TipoDocumento tipo, @RequestParam String numero) {
        return adquirientes.buscar(tipo, numero).map(ResponseEntity::ok).orElse(ResponseEntity.noContent().build());
    }
}
