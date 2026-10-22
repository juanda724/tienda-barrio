package co.tiendabarrio.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.tiendabarrio.dto.response.ReporteInventarioResponse;
import co.tiendabarrio.dto.response.ReporteVentasResponse;
import co.tiendabarrio.service.ReporteService;

/** F-04: reportes de inventario y de ventas, en JSON para la pantalla y en CSV para Excel. */
@RestController
@RequestMapping("/api/reportes")
public class ReporteController {

    private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

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
    public ResponseEntity<String> inventarioCsv(@RequestParam(required = false) String categoria,
                                                @RequestParam(defaultValue = "false") boolean soloBajoMinimo) {
        return archivo("inventario-" + LocalDate.now() + ".csv", reportes.inventarioCsv(categoria, soloBajoMinimo));
    }

    @GetMapping("/ventas")
    public ReporteVentasResponse ventas(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return reportes.ventas(desde, hasta);
    }

    @GetMapping("/ventas.csv")
    public ResponseEntity<String> ventasCsv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) {
        return archivo("ventas-por-producto-" + LocalDate.now() + ".csv", reportes.ventasCsv(desde, hasta));
    }

    private static ResponseEntity<String> archivo(String nombre, String contenido) {
        return ResponseEntity.ok()
                .contentType(CSV)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename(nombre).build().toString())
                .body(contenido);
    }
}
