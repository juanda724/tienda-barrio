package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.comun.NegocioException;
import co.tiendabarrio.inventario.InventarioService;
import co.tiendabarrio.inventario.LineaProductoRequest;
import co.tiendabarrio.inventario.OrigenMovimiento;
import co.tiendabarrio.producto.Producto;
import co.tiendabarrio.producto.ProductoRepository;
import co.tiendabarrio.producto.ProductoRequest;
import co.tiendabarrio.producto.ProductoService;
import co.tiendabarrio.proveedor.IngresoRequest;
import co.tiendabarrio.proveedor.IngresoService;
import co.tiendabarrio.proveedor.Proveedor;
import co.tiendabarrio.proveedor.ProveedorRequest;
import co.tiendabarrio.proveedor.ProveedorService;

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
        Producto arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 0));

        Proveedor proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", "Luis", "300 123 4567",
                "luis@example.com", List.of(arroz.getId())));

        Proveedor guardado = proveedores.obtener(proveedor.getId());
        assertThat(guardado.getTelefono()).isEqualTo("300 123 4567");
        assertThat(guardado.getCorreo()).isEqualTo("luis@example.com");
        assertThat(guardado.getProductos()).extracting(Producto::getNombre).containsExactly("Arroz");
    }

    @Test
    void ingresoDeMercanciaIncrementaElStock() {
        Producto arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 2));
        Producto azucar = productos.crear(new ProductoRequest("Azúcar", "Granos", 5, 1));
        Proveedor proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", null, "3001234567",
                null, List.of(arroz.getId(), azucar.getId())));

        ingresos.registrar(new IngresoRequest(proveedor.getId(), "FV-100", List.of(
                new LineaProductoRequest(arroz.getId(), 20),
                new LineaProductoRequest(azucar.getId(), 10))));

        assertThat(stockDe(arroz)).isEqualTo(22);
        assertThat(stockDe(azucar)).isEqualTo(11);
        assertThat(inventario.historial(arroz.getId()).get(0).origen()).isEqualTo(OrigenMovimiento.INGRESO_PROVEEDOR);
        assertThat(inventario.historial(arroz.getId()).get(0).referencia()).contains("Factura FV-100");
    }

    @Test
    void ingresoConProductoNoAsociadoSeRechaza() {
        Producto arroz = productos.crear(new ProductoRequest("Arroz", "Granos", 5, 2));
        Producto leche = productos.crear(new ProductoRequest("Leche", "Lácteos", 5, 2));
        Proveedor proveedor = proveedores.crear(new ProveedorRequest("Distribuidora", null, "3001234567",
                null, List.of(arroz.getId())));

        assertThatThrownBy(() -> ingresos.registrar(new IngresoRequest(proveedor.getId(), null, List.of(
                new LineaProductoRequest(leche.getId(), 5)))))
                .isInstanceOf(NegocioException.class)
                .hasMessageContaining("no está asociado");
        assertThat(stockDe(leche)).isEqualTo(2);
    }

    private int stockDe(Producto producto) {
        return productoRepository.findById(producto.getId()).orElseThrow().getStockActual();
    }
}
