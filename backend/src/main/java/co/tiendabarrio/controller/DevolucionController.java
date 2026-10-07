package co.tiendabarrio.controller;

import java.util.List;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import co.tiendabarrio.dto.request.CambioEstadoDevolucionRequest;
import co.tiendabarrio.dto.request.DevolucionRequest;
import co.tiendabarrio.dto.response.DevolucionResponse;
import co.tiendabarrio.model.EvidenciaFoto;
import co.tiendabarrio.service.DevolucionService;
import jakarta.validation.Valid;

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

    @GetMapping("/{id}")
    public DevolucionResponse obtener(@PathVariable Long id) {
        return devoluciones.obtener(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DevolucionResponse registrar(@Valid @RequestBody DevolucionRequest datos) {
        return devoluciones.registrar(datos);
    }

    /** Fotos de evidencia, enviadas como multipart/form-data en el campo "fotos". */
    @PostMapping(path = "/{id}/fotos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public DevolucionResponse agregarFotos(@PathVariable Long id, @RequestParam("fotos") List<MultipartFile> fotos) {
        return devoluciones.agregarFotos(id, fotos);
    }

    @GetMapping("/{id}/fotos/{fotoId}")
    public ResponseEntity<byte[]> foto(@PathVariable Long id, @PathVariable Long fotoId) {
        EvidenciaFoto foto = devoluciones.foto(id, fotoId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(foto.getTipoContenido()))
                .cacheControl(CacheControl.noCache())
                .body(devoluciones.leer(foto));
    }

    @PostMapping("/{id}/estado")
    public DevolucionResponse cambiarEstado(@PathVariable Long id, @Valid @RequestBody CambioEstadoDevolucionRequest datos) {
        return devoluciones.cambiarEstado(id, datos);
    }
}
