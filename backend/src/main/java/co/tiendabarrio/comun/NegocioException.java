package co.tiendabarrio.comun;

/** Violación de una regla de negocio (p. ej. stock insuficiente). Se responde con HTTP 400. */
public class NegocioException extends RuntimeException {

    public NegocioException(String mensaje) {
        super(mensaje);
    }
}
