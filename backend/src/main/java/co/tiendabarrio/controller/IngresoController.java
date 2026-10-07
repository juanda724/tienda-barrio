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

import co.tiendabarrio.dto.request.EntregaFaltantesRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.PagoProveedorRequest;
import co.tiendabarrio.dto.request.ResolverDiferenciasRequest;
import co.tiendabarrio.dto.response.ComprobantePagoResponse;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.service.ComprobantePagoService;
import co.tiendabarrio.service.IngresoService;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/ingresos")
public class IngresoController {

    private final IngresoService ingresos;
    private final ComprobantePagoService comprobantes;

    public IngresoController(IngresoService ingresos, ComprobantePagoService comprobantes) {
        this.ingresos = ingresos;
        this.comprobantes = comprobantes;
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

    /** El proveedor entregó faltantes del ingreso, todos o una parte. */
    @PostMapping("/{id}/entregas-faltantes")
    public IngresoResponse registrarEntregaFaltantes(@PathVariable Long id, @Valid @RequestBody EntregaFaltantesRequest datos) {
        return ingresos.registrarEntregaFaltantes(id, datos);
    }

    /** Comprobante del pago al proveedor, con enlaces de WhatsApp y correo; solo para ingresos pagados. */
    @GetMapping("/{id}/comprobante-pago")
    public ComprobantePagoResponse comprobantePago(@PathVariable Long id) {
        return comprobantes.generar(id);
    }

    /** Pagar ya, o con formaPago CREDITO acordar un crédito con fecha de vencimiento. */
    @PostMapping("/{id}/pago")
    public IngresoResponse pagar(@PathVariable Long id, @Valid @RequestBody PagoProveedorRequest datos) {
        return ingresos.pagar(id, datos);
    }
}
