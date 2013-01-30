package eu.cloudtm.exception;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class InvocationException extends Exception {
    public InvocationException(Throwable throwable) {
        super(throwable);
    }

    public InvocationException(String message) {
        super(message);
    }
}
