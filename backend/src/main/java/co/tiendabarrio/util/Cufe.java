package co.tiendabarrio.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;

/**
 * CUFE (Código Único de Factura Electrónica): SHA-384 de los datos de la factura y la clave técnica de la
 * resolución, con la fórmula del anexo técnico de la DIAN. Los valores son pesos enteros.
 */
public final class Cufe {

    /** Ambiente de pruebas de la DIAN (el de producción es "1"): el prototipo no expide facturas reales. */
    public static final String AMBIENTE_PRUEBAS = "2";

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    /** Colombia no cambia de hora: siempre UTC-5. */
    private static final String ZONA = "-05:00";

    private static final String CODIGO_IVA = "01";
    private static final String CODIGO_INC = "04";
    private static final String CODIGO_ICA = "03";

    private Cufe() {
    }

    /**
     * numeroFactura va sin separadores (FE12); valorSinImpuestos más iva es el total. El prototipo no maneja
     * INC ni ICA, que entran en cero.
     */
    public static String calcular(String numeroFactura, LocalDateTime fechaHora, long valorSinImpuestos, long iva,
                                  String nitEmisor, String documentoAdquiriente, String claveTecnica, String ambiente) {
        String datos = numeroFactura + fecha(fechaHora) + hora(fechaHora)
                + valor(valorSinImpuestos)
                + CODIGO_IVA + valor(iva)
                + CODIGO_INC + valor(0)
                + CODIGO_ICA + valor(0)
                + valor(valorSinImpuestos + iva)
                + nitEmisor + documentoAdquiriente + claveTecnica + ambiente;
        try {
            MessageDigest sha384 = MessageDigest.getInstance("SHA-384");
            return HexFormat.of().formatHex(sha384.digest(datos.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-384 no está disponible", e);
        }
    }

    /** Fecha como la pide la DIAN: 2026-11-10. */
    public static String fecha(LocalDateTime fechaHora) {
        return fechaHora.format(FECHA);
    }

    /** Hora como la pide la DIAN: 14:05:33-05:00. */
    public static String hora(LocalDateTime fechaHora) {
        return fechaHora.format(HORA) + ZONA;
    }

    /** Valor en pesos con dos decimales y sin separador de miles: 5600.00. */
    public static String valor(long pesos) {
        return pesos + ".00";
    }
}
