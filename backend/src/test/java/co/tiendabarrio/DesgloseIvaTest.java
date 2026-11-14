package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import co.tiendabarrio.model.DesgloseIva;
import co.tiendabarrio.model.DesgloseIva.Grupo;
import co.tiendabarrio.model.LineaProducto;
import co.tiendabarrio.model.Producto;
import co.tiendabarrio.model.TratamientoIva;

/** El precio ya incluye el IVA: el desglose lo separa en base e impuesto por cada tratamiento (SWR-33). */
class DesgloseIvaTest {

    @Test
    void separaLaBaseDelIvaIncluidoEnElPrecio() {
        DesgloseIva desglose = DesgloseIva.de(List.of(venta("Gaseosa", 3000, 1, TratamientoIva.TARIFA_19)));

        assertThat(desglose.grupos()).containsExactly(new Grupo(TratamientoIva.TARIFA_19, 2521, 479));
        assertThat(desglose.totalBase()).isEqualTo(2521);
        assertThat(desglose.totalIva()).isEqualTo(479);
    }

    @Test
    void agrupaPorTratamientoDeExcluidoATarifaGeneral() {
        DesgloseIva desglose = DesgloseIva.de(List.of(
                venta("Aceite", 9500, 2, TratamientoIva.TARIFA_19),
                venta("Pan", 6500, 1, TratamientoIva.EXCLUIDO),
                venta("Azúcar", 4800, 1, TratamientoIva.TARIFA_5),
                venta("Arroz", 2800, 1, TratamientoIva.EXENTO),
                venta("Leche", 4200, 1, TratamientoIva.EXENTO)));

        assertThat(desglose.grupos()).containsExactly(
                new Grupo(TratamientoIva.EXCLUIDO, 6500, 0),
                new Grupo(TratamientoIva.EXENTO, 7000, 0),
                new Grupo(TratamientoIva.TARIFA_5, 4571, 229),
                new Grupo(TratamientoIva.TARIFA_19, 15966, 3034));
        assertThat(desglose.totalBase() + desglose.totalIva()).as("base más IVA es lo cobrado").isEqualTo(37300);
    }

    @Test
    void redondeaPorTratamientoYNoPorLineaParaQueCuadreConLoCobrado() {
        // Línea por línea serían $ 160 de IVA en cada una ($ 480); sobre los $ 3.000 juntos son $ 479
        DesgloseIva desglose = DesgloseIva.de(List.of(
                venta("Jabón", 1000, 1, TratamientoIva.TARIFA_19),
                venta("Esponja", 1000, 1, TratamientoIva.TARIFA_19),
                venta("Fósforos", 1000, 1, TratamientoIva.TARIFA_19)));

        assertThat(desglose.grupos()).containsExactly(new Grupo(TratamientoIva.TARIFA_19, 2521, 479));
    }

    @Test
    void redondeaElIvaAlPesoMasCercano() {
        // $ 105 al 5 % son $ 5,00 exactos; $ 115 son $ 5,48 y $ 116 son $ 5,52
        assertThat(TratamientoIva.TARIFA_5.ivaIncluidoEn(105)).isEqualTo(5);
        assertThat(TratamientoIva.TARIFA_5.ivaIncluidoEn(115)).isEqualTo(5);
        assertThat(TratamientoIva.TARIFA_5.ivaIncluidoEn(116)).isEqualTo(6);
        assertThat(TratamientoIva.EXENTO.ivaIncluidoEn(5000)).isZero();
        assertThat(TratamientoIva.EXCLUIDO.ivaIncluidoEn(5000)).isZero();
    }

    @Test
    void ventaQueNoDiscriminoIvaNoTieneDesglose() {
        Producto arroz = new Producto("Arroz", "Granos", 5, 2800L, 2200L);
        DesgloseIva desglose = DesgloseIva.de(List.of(LineaProducto.deVenta(arroz, 2, false)));

        assertThat(desglose.vacio()).isTrue();
        assertThat(desglose.totalIva()).isZero();
    }

    private static LineaProducto venta(String nombre, long precio, int cantidad, TratamientoIva tratamiento) {
        Producto producto = new Producto(nombre, null, 0, precio, 0L);
        producto.setTratamientoIva(tratamiento);
        return LineaProducto.deVenta(producto, cantidad, true);
    }
}
