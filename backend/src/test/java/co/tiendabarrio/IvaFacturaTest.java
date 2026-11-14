package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import co.tiendabarrio.config.DatosEmisor;
import co.tiendabarrio.config.ResolucionFacturacion;
import co.tiendabarrio.dto.request.FacturaElectronicaRequest;
import co.tiendabarrio.dto.request.LineaProductoRequest;
import co.tiendabarrio.dto.request.ProductoRequest;
import co.tiendabarrio.dto.request.VentaRequest;
import co.tiendabarrio.dto.response.FacturaElectronicaResponse;
import co.tiendabarrio.dto.response.FacturaElectronicaResponse.Impuesto;
import co.tiendabarrio.dto.response.ProductoResponse;
import co.tiendabarrio.dto.response.VentaResponse;
import co.tiendabarrio.model.FormaPago;
import co.tiendabarrio.model.TipoDocumento;
import co.tiendabarrio.model.TratamientoIva;
import co.tiendabarrio.repository.ProductoRepository;
import co.tiendabarrio.repository.VentaRepository;
import co.tiendabarrio.service.AdquirienteService;
import co.tiendabarrio.service.ClienteService;
import co.tiendabarrio.service.ComprobanteService;
import co.tiendabarrio.service.FacturaElectronicaService;
import co.tiendabarrio.service.InventarioService;
import co.tiendabarrio.service.ProductoService;
import co.tiendabarrio.service.VentaService;
import co.tiendabarrio.util.Cufe;

/**
 * Verificación de SWR-33: clasificar los productos frente al IVA, venderlos en una tienda responsable de IVA
 * y validar que la factura electrónica separe la base y el impuesto por tarifa.
 */
@SpringBootTest
@Transactional
class IvaFacturaTest {

    private static final FacturaElectronicaRequest ANA = new FacturaElectronicaRequest(TipoDocumento.CC,
            "1023456789", "Ana Gómez", "ana@example.com", null, null, null);

    @Autowired
    ProductoService productos;
    @Autowired
    ProductoRepository productoRepository;
    @Autowired
    VentaService ventas;
    @Autowired
    VentaRepository ventaRepository;
    @Autowired
    FacturaElectronicaService facturas;
    @Autowired
    AdquirienteService adquirientes;
    @Autowired
    ClienteService clientes;
    @Autowired
    InventarioService inventario;
    @Autowired
    ComprobanteService comprobantes;
    @Autowired
    ResolucionFacturacion resolucion;

    @Test
    void productoSinClasificarQuedaEnLaTarifaGeneral() {
        ProductoResponse jabon = productos.crear(new ProductoRequest("Jabón", "Aseo", 2, 10, 3000L, 2000L));

        assertThat(jabon.tratamientoIva()).isEqualTo("TARIFA_19");
        assertThat(jabon.tratamientoIvaNombre()).isEqualTo("19 %");
    }

    @Test
    void elTratamientoDeIvaSeDefineAlCrearYSeCambiaAlEditar() {
        ProductoResponse arroz = producto("Arroz", 2800, TratamientoIva.EXENTO);
        assertThat(arroz.tratamientoIvaNombre()).isEqualTo("Exento (0 %)");

        ProductoResponse sinIndicarlo = productos.actualizar(arroz.id(),
                new ProductoRequest("Arroz", "Granos", 5, null, 2900L, 2200L));
        assertThat(sinIndicarlo.tratamientoIva()).as("editar sin indicarlo lo conserva").isEqualTo("EXENTO");

        ProductoResponse reclasificado = productos.actualizar(arroz.id(),
                new ProductoRequest("Arroz", "Granos", 5, null, 2900L, 2200L, TratamientoIva.EXCLUIDO));
        assertThat(reclasificado.tratamientoIva()).isEqualTo("EXCLUIDO");
    }

    @Test
    void tiendaResponsableDiscriminaElIvaPorTarifa() {
        ProductoResponse pan = producto("Pan", 6500, TratamientoIva.EXCLUIDO);
        ProductoResponse arroz = producto("Arroz", 2800, TratamientoIva.EXENTO);
        ProductoResponse azucar = producto("Azúcar", 4800, TratamientoIva.TARIFA_5);
        ProductoResponse aceite = producto("Aceite", 9500, TratamientoIva.TARIFA_19);

        VentaResponse venta = responsableDeIva().registrar(new VentaRequest(List.of(
                new LineaProductoRequest(pan.id(), 1), new LineaProductoRequest(arroz.id(), 1),
                new LineaProductoRequest(azucar.id(), 1), new LineaProductoRequest(aceite.id(), 2)),
                FormaPago.TARJETA, null, null, ANA));

        FacturaElectronicaResponse factura = facturas.generar(venta.id());
        assertThat(factura.emisor().regimen()).isEqualTo("Responsable de IVA");
        assertThat(factura.impuestos()).containsExactly(
                new Impuesto("Excluido", 6500, 0),
                new Impuesto("Exento (0 %)", 2800, 0),
                new Impuesto("5 %", 4571, 229),
                new Impuesto("19 %", 15966, 3034));
        assertThat(factura.total()).as("el IVA va incluido en el precio: el total no cambia").isEqualTo(33100);
        assertThat(factura.iva()).isEqualTo(3263);
        assertThat(factura.subtotal()).isEqualTo(29837);
        assertThat(factura.lineas()).extracting(FacturaElectronicaResponse.Linea::tarifaIva)
                .containsExactly("Excluido", "Exento", "IVA 5 %", "IVA 19 %");
        assertThat(factura.texto())
                .contains("NIT 123456789-6 · Responsable de IVA")
                .contains("2 x Aceite ($ 9.500 c/u): $ 19.000 · IVA 19 %")
                .contains("Subtotal: $ 29.837")
                .contains("IVA: $ 3.263")
                .contains("TOTAL: $ 33.100")
                .contains("IVA 5 %: base $ 4.571, impuesto $ 229")
                .contains("IVA 19 %: base $ 15.966, impuesto $ 3.034");
    }

    @Test
    void elCufeSeCalculaConLaBaseYElIva() {
        ProductoResponse aceite = producto("Aceite", 9500, TratamientoIva.TARIFA_19);
        VentaResponse venta = responsableDeIva().registrar(new VentaRequest(
                List.of(new LineaProductoRequest(aceite.id(), 2)), FormaPago.EFECTIVO, null, null, ANA));

        FacturaElectronicaResponse factura = facturas.generar(venta.id());

        assertThat(factura.cufe()).isEqualTo(Cufe.calcular(factura.numero().replace("-", ""), factura.fechaHora(),
                15966, 3034, "123456789", "1023456789", resolucion.claveTecnica(), Cufe.AMBIENTE_PRUEBAS));
    }

    @Test
    void reclasificarUnProductoNoCambiaLasFacturasYaExpedidas() {
        ProductoResponse aceite = producto("Aceite", 9500, TratamientoIva.TARIFA_19);
        VentaResponse venta = responsableDeIva().registrar(new VentaRequest(
                List.of(new LineaProductoRequest(aceite.id(), 2)), FormaPago.EFECTIVO, null, null, ANA));

        productos.actualizar(aceite.id(),
                new ProductoRequest("Aceite", null, 2, null, 9500L, 5000L, TratamientoIva.EXCLUIDO));

        FacturaElectronicaResponse factura = facturas.generar(venta.id());
        assertThat(factura.impuestos()).containsExactly(new Impuesto("19 %", 15966, 3034));
    }

    @Test
    void tiendaNoResponsableNoDiscriminaIvaAunqueElProductoTengaTarifa() {
        ProductoResponse aceite = producto("Aceite", 9500, TratamientoIva.TARIFA_19);

        VentaResponse venta = ventas.registrar(new VentaRequest(
                List.of(new LineaProductoRequest(aceite.id(), 2)), FormaPago.EFECTIVO, null, null, ANA));

        FacturaElectronicaResponse factura = facturas.generar(venta.id());
        assertThat(factura.emisor().regimen()).isEqualTo("No responsable de IVA");
        assertThat(factura.impuestos()).isEmpty();
        assertThat(factura.iva()).isZero();
        assertThat(factura.subtotal()).isEqualTo(19000);
        assertThat(factura.lineas().get(0).tarifaIva()).isNull();
        assertThat(factura.texto()).doesNotContain("IVA:").doesNotContain("IVA 19 %");
    }

    /** Servicio de ventas igual al de la aplicación, pero de una tienda responsable de IVA. */
    private VentaService responsableDeIva() {
        DatosEmisor emisor = new DatosEmisor("Tienda de Barrio", "Tienda de Barrio", "123456789-6", "", "", true);
        return new VentaService(ventaRepository, productoRepository, inventario, comprobantes, clientes,
                new FacturaElectronicaService(ventaRepository, adquirientes, clientes, emisor, resolucion), emisor);
    }

    private ProductoResponse producto(String nombre, long precio, TratamientoIva tratamiento) {
        return productos.crear(new ProductoRequest(nombre, "Granos", 5, 20, precio, precio / 2, tratamiento));
    }
}
