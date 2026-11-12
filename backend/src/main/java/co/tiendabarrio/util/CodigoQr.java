package co.tiendabarrio.util;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

/** Código QR como imagen SVG, lista para mostrarse e imprimirse a cualquier tamaño. */
public final class CodigoQr {

    private CodigoQr() {
    }

    /** Imagen del QR como data URI (data:image/svg+xml;base64,...), para usarla en el src de un &lt;img&gt;. */
    public static String imagen(String contenido) {
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg(contenido).getBytes(StandardCharsets.UTF_8));
    }

    /** SVG con un cuadro negro por cada módulo del código; el margen blanco lo incluye la matriz. */
    static String svg(String contenido) {
        BitMatrix matriz = matriz(contenido);
        int lado = matriz.getWidth();
        StringBuilder trazo = new StringBuilder();
        for (int y = 0; y < lado; y++) {
            for (int x = 0; x < lado; x++) {
                if (matriz.get(x, y)) {
                    trazo.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                }
            }
        }
        return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + lado + " " + lado
                + "\" shape-rendering=\"crispEdges\"><rect width=\"100%\" height=\"100%\" fill=\"#fff\"/>"
                + "<path fill=\"#000\" d=\"" + trazo + "\"/></svg>";
    }

    /** Con tamaño 0 la librería devuelve la matriz mínima: un punto por módulo. */
    private static BitMatrix matriz(String contenido) {
        try {
            return new QRCodeWriter().encode(contenido, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.CHARACTER_SET, StandardCharsets.UTF_8.name()));
        } catch (WriterException e) {
            throw new IllegalArgumentException("No se pudo generar el código QR", e);
        }
    }
}
