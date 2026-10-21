package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.request.AbonoRequest;
import co.tiendabarrio.dto.response.ClienteCreditoResponse;
import co.tiendabarrio.dto.response.ClienteDetalleResponse;
import co.tiendabarrio.service.CreditoService;
import jakarta.validation.Valid;

/** F-08: clientes con deuda, saldo pendiente y abonos. Las ventas a crédito se registran en /api/ventas. */
@RestController
@RequestMapping("/api/creditos")
public class CreditoController {

    private final CreditoService credito;

    public CreditoController(CreditoService credito) {
        this.credito = credito;
    }

    @GetMapping("/clientes")
    public List<ClienteCreditoResponse> clientes() {
        return credito.listarClientes();
    }

    @GetMapping("/clientes/{id}")
    public ClienteDetalleResponse detalle(@PathVariable Long id) {
        return credito.detalle(id);
    }

    @PostMapping("/clientes/{id}/abonos")
    @ResponseStatus(HttpStatus.CREATED)
    public ClienteCreditoResponse abonar(@PathVariable Long id, @Valid @RequestBody AbonoRequest datos) {
        return credito.registrarAbono(id, datos);
    }
}
