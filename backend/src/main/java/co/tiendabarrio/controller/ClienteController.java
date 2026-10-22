package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.request.AbonoRequest;
import co.tiendabarrio.dto.request.ClienteRequest;
import co.tiendabarrio.dto.response.ClienteResponse;
import co.tiendabarrio.dto.response.EstadoCuentaResponse;
import co.tiendabarrio.service.ClienteService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/clientes")
public class ClienteController {

    private final ClienteService clientes;

    public ClienteController(ClienteService clientes) {
        this.clientes = clientes;
    }

    @GetMapping
    public List<ClienteResponse> listar() {
        return clientes.listar();
    }

    @GetMapping("/{id}")
    public ClienteResponse obtener(@PathVariable Long id) {
        return clientes.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteResponse crear(@Valid @RequestBody ClienteRequest datos) {
        return clientes.crear(datos);
    }

    @PutMapping("/{id}")
    public ClienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody ClienteRequest datos) {
        return clientes.actualizar(id, datos);
    }

    @GetMapping("/{id}/estado-cuenta")
    public EstadoCuentaResponse estadoCuenta(@PathVariable Long id) {
        return clientes.estadoCuenta(id);
    }

    @PostMapping("/{id}/abonos")
    @ResponseStatus(HttpStatus.CREATED)
    public EstadoCuentaResponse registrarAbono(@PathVariable Long id, @Valid @RequestBody AbonoRequest datos) {
        return clientes.registrarAbono(id, datos);
    }
}
