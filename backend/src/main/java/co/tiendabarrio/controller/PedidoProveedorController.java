package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.request.CambioEstadoRequest;
import co.tiendabarrio.dto.request.PedidoProveedorRequest;
import co.tiendabarrio.dto.response.PedidoProveedorResponse;
import co.tiendabarrio.model.EstadoPedido;
import co.tiendabarrio.service.PedidoProveedorService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pedidos-proveedor")
public class PedidoProveedorController {

    private final PedidoProveedorService pedidos;

    public PedidoProveedorController(PedidoProveedorService pedidos) {
        this.pedidos = pedidos;
    }

    @GetMapping
    public List<PedidoProveedorResponse> listar(@RequestParam(required = false) EstadoPedido estado) {
        return pedidos.listar(estado);
    }

    @GetMapping("/{id}")
    public PedidoProveedorResponse obtener(@PathVariable Long id) {
        return pedidos.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PedidoProveedorResponse crear(@Valid @RequestBody PedidoProveedorRequest datos) {
        return pedidos.crear(datos);
    }

    @PostMapping("/{id}/estado")
    public PedidoProveedorResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest datos) {
        return pedidos.cambiarEstado(id, datos);
    }
}
