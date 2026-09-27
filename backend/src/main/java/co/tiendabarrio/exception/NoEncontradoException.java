package co.tiendabarrio.exception;

/** El recurso solicitado no existe. Se responde con HTTP 404. */
public class NoEncontradoException extends RuntimeException {

    public NoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
