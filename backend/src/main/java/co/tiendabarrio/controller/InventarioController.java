package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.request.CompraRequest;
import co.tiendabarrio.dto.response.MovimientoResponse;
import co.tiendabarrio.service.InventarioService;
import jakarta.validation.Valid;

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

    @PostMapping("/compras")
    @ResponseStatus(HttpStatus.CREATED)
    public MovimientoResponse registrarCompra(@Valid @RequestBody CompraRequest compra) {
        return inventario.registrarCompra(compra);
    }
}
