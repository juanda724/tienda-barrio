package co.tiendabarrio.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.response.ReporteInventarioResponse;
import co.tiendabarrio.service.ReporteService;

/** F-04: reportes de inventario (consulta en pantalla y descarga en CSV). */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private final ReporteService reportes;

    public ReporteController(ReporteService reportes) {
        this.reportes = reportes;
    }

    @GetMapping("/inventario")
    public ReporteInventarioResponse inventario(@RequestParam(required = false) String categoria,
                                                @RequestParam(defaultValue = "false") boolean soloBajoMinimo) {
        return reportes.inventario(categoria, soloBajoMinimo);
    }

    @GetMapping("/inventario.csv")
    public ResponseEntity<byte[]> inventarioCsv(@RequestParam(required = false) String categoria,
                                                @RequestParam(defaultValue = "false") boolean soloBajoMinimo) {
        byte[] contenido = reportes.inventarioCsv(categoria, soloBajoMinimo).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"reporte-inventario.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(contenido);
    }
}
