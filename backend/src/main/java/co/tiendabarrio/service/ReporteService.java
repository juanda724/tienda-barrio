package co.tiendabarrio.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.response.ReporteInventarioResponse;
import co.tiendabarrio.model.MovimientoInventario;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.TipoMovimiento;
import co.tiendabarrio.repository.MovimientoRepository;
import co.tiendabarrio.repository.ProductoRepository;

/** Reportes de inventario consultables desde el celular (F-04, SWR-09 y SWR-10). */
@Service
public class ReporteService {

    private final ProductoRepository productos;
    private final MovimientoRepository movimientos;

    public ReporteService(ProductoRepository productos, MovimientoRepository movimientos) {
        this.productos = productos;
        this.movimientos = movimientos;
    }

    /** Cantidad por producto, con filtros opcionales por categoría y por "solo en stock mínimo". */
    @Transactional(readOnly = true)
    public ReporteInventarioResponse inventario(String categoria, boolean soloBajoMinimo) {
        // totales[0] = unidades que han entrado, totales[1] = unidades que han salido
        Map<Long, int[]> totalesPorProducto = new HashMap<>();
        for (MovimientoInventario m : movimientos.findAll()) {
            int[] totales = totalesPorProducto.computeIfAbsent(m.getProducto().getId(), id -> new int[2]);
            if (m.getTipo() == TipoMovimiento.ENTRADA) {
                totales[0] += m.getCantidad();
            } else {
                totales[1] += m.getCantidad();
            }
        }

        boolean sinFiltroCategoria = categoria == null || categoria.isBlank();
        List<ReporteInventarioResponse.Fila> filas = productos.findAllByOrderByNombreAsc().stream()
                .filter(p -> sinFiltroCategoria || categoria.trim().equalsIgnoreCase(p.getCategoria()))
                .filter(p -> !soloBajoMinimo || p.isBajoMinimo())
                .map(p -> {
                    int[] totales = totalesPorProducto.getOrDefault(p.getId(), new int[2]);
                    return ReporteInventarioResponse.Fila.de(p, totales[0], totales[1]);
                })
                .toList();

        int unidades = filas.stream().mapToInt(ReporteInventarioResponse.Fila::stockActual).sum();
        int bajoMinimo = (int) filas.stream().filter(ReporteInventarioResponse.Fila::bajoMinimo).count();
        BigDecimal valorCosto = filas.stream().map(ReporteInventarioResponse.Fila::valorEnCosto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal valorVenta = filas.stream()
                .map(f -> f.precioVenta().multiply(BigDecimal.valueOf(f.stockActual())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new ReporteInventarioResponse(LocalDateTime.now(), filas.size(), unidades, bajoMinimo, valorCosto,
                valorVenta, filas);
    }

    /** El mismo reporte en CSV (separado por ";" y con BOM para que Excel respete las tildes). */
    @Transactional(readOnly = true)
    public String inventarioCsv(String categoria, boolean soloBajoMinimo) {
        StringBuilder csv = new StringBuilder("\uFEFF");
        csv.append("Producto;Categoría;Stock actual;Stock mínimo;Estado;Total entradas;Total salidas\r\n");
        for (ReporteInventarioResponse.Fila f : inventario(categoria, soloBajoMinimo).filas()) {
            csv.append(celda(f.nombre())).append(';')
                    .append(celda(f.categoria())).append(';')
                    .append(f.stockActual()).append(';')
                    .append(f.stockMinimo()).append(';')
                    .append(f.bajoMinimo() ? "Reabastecer" : "Disponible").append(';')
                    .append(f.totalEntradas()).append(';')
                    .append(f.totalSalidas()).append("\r\n");
        }
        return csv.toString();
    }

    private static String celda(String texto) {
        return "\"" + (texto == null ? "" : texto.replace("\"", "\"\"")) + "\"";
    }
}
