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

import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.ResolverDiferenciasRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.service.IngresoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ingresos")
public class IngresoController {

    private final IngresoService ingresos;

    public IngresoController(IngresoService ingresos) {
        this.ingresos = ingresos;
    }

    @GetMapping
    public List<IngresoResponse> listar() {
        return ingresos.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public IngresoResponse registrar(@Valid @RequestBody IngresoRequest datos) {
        return ingresos.registrar(datos);
    }

    @PostMapping("/{id}/resolver-diferencias")
    public IngresoResponse resolverDiferencias(@PathVariable Long id, @Valid @RequestBody ResolverDiferenciasRequest datos) {
        return ingresos.resolverDiferencias(id, datos);
    }

    /** Pagar ya, o con formaPago CREDITO acordar un crédito con fecha de vencimiento. */
    @PostMapping("/{id}/pago")
    public IngresoResponse pagar(@PathVariable Long id, @Valid @RequestBody PagoProveedorRequest datos) {
        return ingresos.pagar(id, datos);
    }
}
