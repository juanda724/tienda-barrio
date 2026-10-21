package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.request.CambioEstadoRequest;
import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.response.DevolucionResponse;
import co.tiendabarrio.dto.response.FotoDevolucion;
import co.tiendabarrio.service.DevolucionService;
import jakarta.validation.Valid;

/** F-07: devoluciones de productos en mal estado al proveedor. */
@RestController
@RequestMapping("/api/devoluciones")
public class DevolucionController {

    private final DevolucionService devoluciones;

    public DevolucionController(DevolucionService devoluciones) {
        this.devoluciones = devoluciones;
    }

    @GetMapping
    public List<DevolucionResponse> listar() {
        return devoluciones.listar();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DevolucionResponse registrar(@Valid @RequestBody DevolucionRequest datos) {
        return devoluciones.registrar(datos);
    }

    @PutMapping("/{id}/estado")
    public DevolucionResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoRequest datos) {
        return devoluciones.cambiarEstado(id, datos);
    }

    @GetMapping("/{id}/nota")
    public ResponseEntity<String> nota(@PathVariable Long id) {
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/plain; charset=UTF-8"))
                .body(devoluciones.nota(id));
    }

    @GetMapping("/{id}/foto")
    public ResponseEntity<byte[]> foto(@PathVariable Long id) {
        FotoDevolucion foto = devoluciones.foto(id);
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(foto.tipo())).body(foto.datos());
    }
}
