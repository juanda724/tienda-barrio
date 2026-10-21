package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.service.VentaService;
import jakarta.validation.Valid;

/** F-05: ventas y comprobante digital. */
@RestController
@RequestMapping("/api/ventas")
public class VentaController {

    private final VentaService ventas;

    public VentaController(VentaService ventas) {
        this.ventas = ventas;
    }

    @GetMapping
    public List<VentaResponse> listar() {
        return ventas.listar();
    }

    @GetMapping("/{id}")
    public VentaResponse obtener(@PathVariable Long id) {
        return ventas.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VentaResponse registrar(@Valid @RequestBody VentaRequest datos) {
        return ventas.registrar(datos);
    }

    @GetMapping("/{id}/comprobante")
    public ResponseEntity<String> comprobante(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .body(ventas.comprobante(id));
    }
}
