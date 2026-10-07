package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.response.IngresoResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.exception.NegocioException;
import co.tiendabarrio.model.OrigenMovimiento;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/** Verificación de F-02 / PBI-02: registrar un proveedor, generar un ingreso y validar el inventario. */
@SpringBootTest
@Transactional
class ProveedoresIngresoTest {

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    ProveedorService proveedores;
    @Autowired
    IngresoService ingresos;
    @Autowired
    InventarioService inventario;

    @Test
    void proveedorConservaContactoYProductosAsociados() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 0, 1000L, 800L));

        ProveedorResponse proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", "Luis", "300 123 4567",
                "luis@example.com", List.of(arroz.id())));

        ProveedorResponse guardado = proveedores.obtener(proveedor.id());
        assertThat(guardado.telefono()).isEqualTo("300 123 4567");
        assertThat(guardado.correo()).isEqualTo("luis@example.com");
        assertThat(guardado.productos()).extracting(ProductoResponse::nombre).containsExactly("Arroz");
    }

    @Test
    void ingresoDeMercanciaIncrementaElStock() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 2, 1000L, 800L));
        ProductoResponse azucar = productos.crear(new ProductoRequest("Azúcar", "Granos", 5, 1, 1000L, 800L));
        ProveedorResponse proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", null, "3001234567",
                null, List.of(arroz.id(), azucar.id())));

        IngresoResponse ingreso = ingresos.registrar(new IngresoRequest(proveedor.id(), null, "FV-100", List.of(
                new LineaIngresoRequest(arroz.id(), 20, 20, 1000L),
                new LineaIngresoRequest(azucar.id(), 10, 10, 1000L))));

        assertThat(stockDe(arroz)).isEqualTo(22);
        assertThat(stockDe(azucar)).isEqualTo(11);
        assertThat(inventario.historial(arroz.id()).get(0).origen()).isEqualTo(OrigenMovimiento.INGRESO_PROVEEDOR);
        assertThat(inventario.historial(arroz.id()).get(0).numeroDocumento()).isEqualTo(ingreso.id());
        assertThat(inventario.historial(arroz.id()).get(0).referencia()).contains("Factura FV-100");
    }

    @Test
    void ingresoConProductoNoAsociadoSeRechaza() {
        ProductoResponse arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 2, 1000L, 800L));
        ProductoResponse leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 2, 1000L, 800L));
        ProveedorResponse proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", null, "3001234567",
                null, List.of(arroz.id())));

        assertThatThrownBy(() -> ingresos.registrar(new IngresoRequest(proveedor.id(), null, null, List.of(
                new LineaIngresoRequest(leche.id(), 5, 5, 1000L)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no está asociado");
        assertThat(stockDe(leche)).isEqualTo(2);
    }

    private int stockDe(ProductoResponse producto) {
        return productoRepository.findById(producto.id()).orElseThrow().getStockActual();
    }
}
