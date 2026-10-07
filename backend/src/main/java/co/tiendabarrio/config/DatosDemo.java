package co.tiendabarrio.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.dto.request.AbonoRequest;
import co.tiendabarrio.dto.request.ClienteRequest;
import co.tiendabarrio.dto.request.IngresoRequest;
import co.tiendabarrio.dto.request.LineaIngresoRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.PedidoProveedorRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.ProveedorRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.ClienteResponse;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.ProveedorResponse;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.service.ClienteService;
import co.tiendabarrio.service.IngresoService;
import co.tiendabarrio.service.PedidoProveedorService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.ProveedorService;
import co.tiendabarrio.service.VentaService;

/** Carga datos de ejemplo la primera vez que arranca la aplicación (tienda.datos-demo=true). */
@Component
@ConditionalOnProperty(name = "tienda.datos-demo", havingValue = "true")
public class DatosDemo implements CommandLineRunner {

    private final ProductoRepository productoRepository;
    private final ProductoService productos;
    private final ProveedorService proveedores;
    private final PedidoProveedorService pedidos;
    private final ClienteService clientes;
    private final VentaService ventas;
    private final IngresoService ingresos;

    public DatosDemo(ProductoRepository productoRepository, ProductoService productos, ProveedorService proveedores,
                     PedidoProveedorService pedidos, ClienteService clientes, VentaService ventas,
                     IngresoService ingresos) {
        this.productoRepository = productoRepository;
        this.productos = productos;
        this.proveedores = proveedores;
        this.pedidos = pedidos;
        this.clientes = clientes;
        this.ventas = ventas;
        this.ingresos = ingresos;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (productoRepository.count() > 0) {
            return;
        }
        ProductoResponse arroz = crear("Arroz 500 g", "Granos", 10, 24, 2800, 2200);
        ProductoResponse azucar = crear("Azúcar 1 kg", "Granos", 8, 6, 4800, 3900);
        ProductoResponse aceite = crear("Aceite 1 L", "Aceites", 10, 15, 9500, 7800);
        ProductoResponse leche = crear("Leche entera 1 L", "Lácteos", 12, 20, 4200, 3500);
        ProductoResponse huevos = crear("Huevos x 30", "Lácteos", 3, 4, 18000, 15000);
        ProductoResponse pan = crear("Pan tajado", "Panadería", 5, 9, 6500, 5000);

        ProveedorResponse distribuidora = proveedores.crear(new ProveedorRequest("Distribuidora El Nogal",
                "Luis Pérez", "300 123 4567", "pedidos@distrinogal.example",
                List.of(arroz.id(), azucar.id(), aceite.id())));
        ProveedorResponse lacteos = proveedores.crear(new ProveedorRequest("Lácteos La Pradera", "Ana Gómez",
                "310 765 4321", null, List.of(leche.id(), huevos.id())));

        // Un ingreso con diferencias: la factura cobra 12 leches pero llegaron 10, así que el pago queda bloqueado
        ingresos.registrar(new IngresoRequest(lacteos.id(), null, "LP-0457", List.of(
                new LineaIngresoRequest(leche.id(), 10, 12, 3500L),
                new LineaIngresoRequest(huevos.id(), 2, 2, 15000L))));

        // El azúcar arranca por debajo del mínimo: hay un pedido en espera para reabastecerlo
        pedidos.crear(new PedidoProveedorRequest(distribuidora.id(), List.of(
                new LineaProductoRequest(azucar.id(), 12)), null, null));

        // Una clienta con fiado y un abono parcial (saldo pendiente: $ 9.900), y otro cliente al día
        ClienteResponse maria = clientes.crear(new ClienteRequest("María Elena Castaño", "310 555 1234",
                "Calle 12 # 4-56, El Nogal"));
        clientes.crear(new ClienteRequest("Pedro Ramírez", "320 444 5566", null));
        ventas.registrar(new VentaRequest(List.of(
                new LineaProductoRequest(leche.id(), 2),
                new LineaProductoRequest(pan.id(), 1)), FormaPago.CREDITO, null, maria.id()));
        clientes.registrarAbono(maria.id(), new AbonoRequest(5000L, FormaPago.EFECTIVO, null, null));
    }

    private ProductoResponse crear(String nombre, String categoria, int stockMinimo, int stockInicial,
                                   long precioVenta, long costo) {
        return productos.crear(new ProductoRequest(nombre, categoria, stockMinimo, stockInicial, precioVenta, costo));
    }
}
