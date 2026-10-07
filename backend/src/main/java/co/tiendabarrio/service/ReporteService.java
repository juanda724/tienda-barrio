package co.tiendabarrio.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.ReporteInventarioResponse;
import co.tiendabarrio.dto.response.ReporteVentasResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.Venta;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.VentaRepository;

/**
 * Reportes de inventario (F-04): cantidad por producto (SWR-09) y ventas por producto en un rango
 * de fechas. Ambos se pueden exportar a CSV para abrirlos en Excel.
 */
@Service
public class ReporteService {

    private static final int MAX_DIAS = 366;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ProductoRepository productos;
    private final VentaRepository ventas;

    public ReporteService(ProductoRepository productos, VentaRepository ventas) {
        this.productos = productos;
        this.ventas = ventas;
    }

    /** categoria y soloBajoMinimo son filtros opcionales. */
    @Transactional(readOnly = true)
    public ReporteInventarioResponse inventario(String categoria, boolean soloBajoMinimo) {
        List<Producto> todos = productos.findAllByOrderByNombreAsc();
        String filtro = categoria == null || categoria.isBlank() ? null : categoria.trim();
        List<ReporteInventarioResponse.Fila> filas = todos.stream()
                .filter(p -> filtro == null || filtro.equalsIgnoreCase(p.getCategoria()))
                .filter(p -> !soloBajoMinimo || p.isBajoMinimo())
                .map(ReporteService::fila)
                .toList();
        var totales = new ReporteInventarioResponse.Totales(
                filas.size(),
                filas.stream().mapToInt(ReporteInventarioResponse.Fila::stockActual).sum(),
                (int) filas.stream().filter(ReporteInventarioResponse.Fila::bajoMinimo).count(),
                filas.stream().mapToLong(ReporteInventarioResponse.Fila::valorCosto).sum(),
                filas.stream().mapToLong(ReporteInventarioResponse.Fila::valorVenta).sum());
        List<String> categorias = todos.stream().map(Producto::getCategoria).filter(Objects::nonNull)
                .distinct().sorted().toList();
        return new ReporteInventarioResponse(LocalDateTime.now(), filtro, filas, totales, categorias);
    }

    /** Sin fechas, los últimos 30 días hasta hoy. */
    @Transactional(readOnly = true)
    public ReporteVentasResponse ventas(LocalDate desde, LocalDate hasta) {
        LocalDate fin = hasta == null ? LocalDate.now() : hasta;
        LocalDate inicio = desde == null ? fin.minusDays(29) : desde;
        if (inicio.isAfter(fin)) {
            throw new NegocioException("La fecha inicial no puede ser posterior a la final");
        }
        if (inicio.plusDays(MAX_DIAS).isBefore(fin)) {
            throw new NegocioException("El rango del reporte no puede superar un año");
        }
        List<Venta> delPeriodo = ventas.findByFechaHoraGreaterThanEqualAndFechaHoraLessThanOrderByFechaHoraAsc(
                inicio.atStartOfDay(), fin.plusDays(1).atStartOfDay());

        Map<Producto, long[]> porProducto = new LinkedHashMap<>(); // [unidades, total]
        Map<FormaPago, long[]> porForma = new EnumMap<>(FormaPago.class); // [ventas, total]
        for (Venta v : delPeriodo) {
            if (v.getFormaPago() != null) {
                long[] f = porForma.computeIfAbsent(v.getFormaPago(), k -> new long[2]);
                f[0]++;
                f[1] += v.getTotal() == null ? 0 : v.getTotal();
            }
            for (LineaProducto l : v.getLineas()) {
                long[] p = porProducto.computeIfAbsent(l.getProducto(), k -> new long[2]);
                p[0] += l.getCantidad();
                p[1] += l.getSubtotal();
            }
        }

        List<ReporteVentasResponse.Fila> filas = porProducto.entrySet().stream()
                .map(e -> {
                    Producto p = e.getKey();
                    int unidades = (int) e.getValue()[0];
                    long total = e.getValue()[1];
                    long costo = p.getCosto() == null ? 0 : p.getCosto() * unidades;
                    return new ReporteVentasResponse.Fila(p.getId(), p.getNombre(), unidades, total, total - costo);
                })
                .sorted(Comparator.comparingInt(ReporteVentasResponse.Fila::unidades).reversed()
                        .thenComparing(ReporteVentasResponse.Fila::nombre))
                .toList();
        long total = filas.stream().mapToLong(ReporteVentasResponse.Fila::total).sum();
        var resumen = new ReporteVentasResponse.Resumen(
                delPeriodo.size(),
                filas.stream().mapToInt(ReporteVentasResponse.Fila::unidades).sum(),
                total,
                filas.stream().mapToLong(ReporteVentasResponse.Fila::gananciaEstimada).sum(),
                delPeriodo.isEmpty() ? 0 : total / delPeriodo.size(),
                porForma.entrySet().stream()
                        .map(e -> new ReporteVentasResponse.PorFormaPago(e.getKey().name(), e.getKey().getNombre(),
                                (int) e.getValue()[0], e.getValue()[1]))
                        .toList());
        return new ReporteVentasResponse(LocalDateTime.now(), inicio, fin, filas, resumen);
    }

    /** CSV separado por punto y coma, que Excel en español abre directamente. */
    public String inventarioCsv(String categoria, boolean soloBajoMinimo) {
        ReporteInventarioResponse r = inventario(categoria, soloBajoMinimo);
        Csv csv = new Csv()
                .fila("Reporte de inventario", "Generado " + r.generado().format(FECHA_HORA))
                .fila("Producto", "Categoría", "Stock", "Stock mínimo", "Estado", "Costo", "Precio de venta",
                        "Valor a costo", "Valor a precio de venta");
        for (var f : r.filas()) {
            csv.fila(f.nombre(), f.categoria(), f.stockActual(), f.stockMinimo(),
                    f.bajoMinimo() ? "Reabastecer" : "Disponible", f.costo(), f.precioVenta(), f.valorCosto(),
                    f.valorVenta());
        }
        var t = r.totales();
        return csv.fila("TOTAL", t.productos() + " productos", t.unidades(), null, t.bajoMinimo() + " por reabastecer",
                null, null, t.valorCosto(), t.valorVenta()).texto();
    }

    public String ventasCsv(LocalDate desde, LocalDate hasta) {
        ReporteVentasResponse r = ventas(desde, hasta);
        Csv csv = new Csv()
                .fila("Ventas por producto", "Del " + r.desde() + " al " + r.hasta())
                .fila("Producto", "Unidades vendidas", "Total vendido", "Ganancia estimada");
        r.filas().forEach(f -> csv.fila(f.nombre(), f.unidades(), f.total(), f.gananciaEstimada()));
        var s = r.resumen();
        return csv.fila("TOTAL", s.unidades(), s.total(), s.gananciaEstimada()).texto();
    }

    private static ReporteInventarioResponse.Fila fila(Producto p) {
        long costo = p.getCosto() == null ? 0 : p.getCosto();
        long precio = p.getPrecioVenta() == null ? 0 : p.getPrecioVenta();
        return new ReporteInventarioResponse.Fila(p.getId(), p.getNombre(), p.getCategoria(), p.getStockActual(),
                p.getStockMinimo(), p.isBajoMinimo(), p.getCosto(), p.getPrecioVenta(),
                costo * p.getStockActual(), precio * p.getStockActual());
    }

    /** Arma un CSV con ";" y la marca de orden de bytes (BOM) para que Excel lea bien las tildes. */
    private static final class Csv {
        private final StringBuilder texto = new StringBuilder().append((char) 0xFEFF);

        Csv fila(Object... celdas) {
            for (int i = 0; i < celdas.length; i++) {
                if (i > 0) {
                    texto.append(';');
                }
                texto.append(celda(celdas[i]));
            }
            texto.append("\r\n");
            return this;
        }

        String texto() {
            return texto.toString();
        }

        private static String celda(Object valor) {
            if (valor == null) {
                return "";
            }
            String s = valor.toString();
            return s.contains(";") || s.contains("\"") || s.contains("\n") ? "\"" + s.replace("\"", "\"\"") + "\"" : s;
        }
    }
}
