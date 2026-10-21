package co.tiendabarrio.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

/** Utilidades para trabajar con valores en pesos colombianos. */
public final class Dinero {

    private static final Locale COLOMBIA = Locale.of("es", "CO");

    private Dinero() {
    }

    /** Redondea a 2 decimales; un valor nulo se toma como cero. */
    public static BigDecimal redondear(BigDecimal valor) {
        return valor == null ? BigDecimal.ZERO.setScale(2) : valor.setScale(2, RoundingMode.HALF_UP);
    }

    /** Ejemplo: 12500 → "$12.500". */
    public static String formatear(BigDecimal valor) {
        return String.format(COLOMBIA, "$%,.0f", valor == null ? BigDecimal.ZERO : valor);
    }
}
