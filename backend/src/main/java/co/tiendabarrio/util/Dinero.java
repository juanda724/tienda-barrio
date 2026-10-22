package co.tiendabarrio.util;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

/** Formato de pesos colombianos sin decimales: 3200 → "$ 3.200". */
public final class Dinero {

    private static final DecimalFormatSymbols SIMBOLOS = new DecimalFormatSymbols();

    static {
        SIMBOLOS.setGroupingSeparator('.');
    }

    private Dinero() {
    }

    public static String formatear(long pesos) {
        return "$ " + new DecimalFormat("#,##0", SIMBOLOS).format(pesos);
    }
}
