package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.response.MovimientoResponse;
import co.tiendabarrio.service.InventarioService;

@RestController
@RequestMapping("/api")
public class InventarioController {

    private final InventarioService inventario;

    public InventarioController(InventarioService inventario) {
        this.inventario = inventario;
    }

    @GetMapping("/movimientos")
    public List<MovimientoResponse> historial(@RequestParam(required = false) Long productoId) {
        return inventario.historial(productoId);
    }
}
