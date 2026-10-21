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
import co.tiendabarrio.dto.request.VerificacionRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.VerificacionResponse;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.VerificacionService;
import jakarta.validation.Valid;

/** F-02: ingresos de mercancía. F-06: verificación de la factura y confirmación del pago. */
@RestController
@RequestMapping("/api/ingresos")
public class IngresoController {

    private final IngresoService ingresos;
    private final VerificacionService verificaciones;

    public IngresoController(IngresoService ingresos, VerificacionService verificaciones) {
        this.ingresos = ingresos;
        this.verificaciones = verificaciones;
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

    @GetMapping("/{id}/verificaciones")
    public List<VerificacionResponse> verificaciones(@PathVariable Long id) {
        return verificaciones.listar(id);
    }

    @PostMapping("/{id}/verificaciones")
    @ResponseStatus(HttpStatus.CREATED)
    public VerificacionResponse verificar(@PathVariable Long id, @Valid @RequestBody VerificacionRequest datos) {
        return verificaciones.verificar(id, datos);
    }

    @PostMapping("/{id}/pago")
    public IngresoResponse confirmarPago(@PathVariable Long id) {
        return verificaciones.confirmarPago(id);
    }
}
