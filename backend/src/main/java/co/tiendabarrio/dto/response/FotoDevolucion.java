package co.tiendabarrio.dto.response;

/** Imagen de evidencia de una devolución junto con su tipo (p. ej. image/jpeg). */
public record FotoDevolucion(byte[] datos, String tipo) {
}
