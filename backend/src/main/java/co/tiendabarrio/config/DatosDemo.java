package co.tiendabarrio.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;

/** Carga datos de ejemplo la primera vez que arranca la aplicación (tienda.datos-demo=true). */
@Component
@ConditionalOnProperty(name = "tienda.datos-demo", havingValue = "true")
public class DatosDemo implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final ProductoService productos;
    private final ProveedorService proveedores;

    public DatosDemo(ProductoRepository productoRepository, ProductoService productos,
                     ProveedorService proveedores) {
        this.productoRepository = productoRepository;
        this.productos = productos;
        this.proveedores = proveedores;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productoRepository.count() > 0) {
            return;
        }
        ProductoResponse arroz = crear("Arroz 500 g", "Granos", 10, 24);
        ProductoResponse azucar = crear("Azúcar 1 kg", "Granos", 8, 6);
        ProductoResponse aceite = crear("Aceite 1 L", "Aceites", 10, 15);
        ProductoResponse leche = crear("Leche entera 1 L", "Lácteos", 12, 20);
        ProductoResponse huevos = crear("Huevos x 30", "Lácteos", 3, 4);
        crear("Pan tajado", "Panadería", 5, 9);

        proveedores.crear(new ProveedorRequest("Distribuidora El Nogal", "Luis Pérez", "300 123 4567",
                "pedidos@distrinogal.example", List.of(arroz.id(), azucar.id(), aceite.id())));
        proveedores.crear(new ProveedorRequest("Lácteos La Pradera", "Ana Gómez", "310 765 4321",
                null, List.of(leche.id(), huevos.id())));
    }

    private ProductoResponse crear(String nombre, String categoria, int stockMinimo, int stockInicial) {
        return productos.crear(new ProductoRequest(nombre, categoria, stockMinimo, stockInicial));
    }
}
