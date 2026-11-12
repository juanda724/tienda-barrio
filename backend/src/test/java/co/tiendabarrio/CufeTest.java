package co.tiendabarrio;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import co.tiendabarrio.util.Cufe;

/** El CUFE se calcula con la fórmula de la DIAN: se comprueba con el ejemplo de su anexo técnico. */
class CufeTest {

    private static final LocalDateTime FECHA = LocalDateTime.of(2019, 1, 16, 10, 53, 10);

    @Test
    void coincideConElEjemploDelAnexoTecnicoDeLaDian() {
        String cufe = Cufe.calcular("323200000129", FECHA, 1_500_000, 285_000, "700085371", "800199436",
                "693ff6f2a553c3646a063436fd4dd9ded0311471", "1");

        assertThat(cufe).isEqualTo(
                "8bb918b19ba22a694f1da11c643b5e9de39adf60311cf179179e9b33381030bcd4c3c3f156c506ed5908f9276f5bd9b4");
    }

    @Test
    void cambiaSiCambiaCualquierDatoDeLaFactura() {
        String original = Cufe.calcular("FE12", FECHA, 5600, 0, "123456789", "1023456789", "clave", Cufe.AMBIENTE_PRUEBAS);

        assertThat(Cufe.calcular("FE13", FECHA, 5600, 0, "123456789", "1023456789", "clave", Cufe.AMBIENTE_PRUEBAS))
                .as("otro número de factura").isNotEqualTo(original);
        assertThat(Cufe.calcular("FE12", FECHA, 5601, 0, "123456789", "1023456789", "clave", Cufe.AMBIENTE_PRUEBAS))
                .as("otro valor").isNotEqualTo(original);
        assertThat(Cufe.calcular("FE12", FECHA, 5600, 0, "123456789", "1023456789", "otra", Cufe.AMBIENTE_PRUEBAS))
                .as("otra clave técnica").isNotEqualTo(original);
    }
}
