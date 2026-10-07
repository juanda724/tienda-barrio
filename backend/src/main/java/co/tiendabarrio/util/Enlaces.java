package co.tiendabarrio.util;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

/**
 * Enlaces que abren WhatsApp o el correo con un mensaje ya escrito. Quien usa la app revisa
 * el mensaje y lo envía desde su propia cuenta; el sistema no envía nada por su cuenta.
 */
public final class Enlaces {

    /** Indicativo de Colombia, para celulares escritos sin él (10 dígitos que empiezan por 3). */
    private static final String INDICATIVO_PAIS = "57";

    private Enlaces() {
    }

    /**
     * Enlace https://wa.me/... con el mensaje. Sin teléfono, WhatsApp deja elegir el contacto.
     */
    public static String whatsapp(String telefono, String mensaje) {
        String digitos = telefono == null ? "" : telefono.replaceAll("\\D", "");
        if (digitos.length() == 10 && digitos.startsWith("3")) {
            digitos = INDICATIVO_PAIS + digitos;
        }
        return "https://wa.me/" + digitos + "?text=" + codificar(mensaje);
    }

    /**
     * Enlace que abre Gmail en el navegador con el correo ya escrito (destinatario, asunto y mensaje);
     * null si no hay correo. Se usa Gmail web en vez de mailto: porque mailto depende del programa de
     * correo configurado en cada computador y en muchos no abre nada.
     */
    public static String correo(String correo, String asunto, String mensaje) {
        if (correo == null || correo.isBlank()) {
            return null;
        }
        return "https://mail.google.com/mail/?view=cm&fs=1&to=" + codificar(correo.trim())
                + "&su=" + codificar(asunto) + "&body=" + codificar(mensaje);
    }

    /** URLEncoder usa "+" para los espacios; WhatsApp y los clientes de correo esperan "%20". */
    private static String codificar(String texto) {
        return URLEncoder.encode(texto, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
