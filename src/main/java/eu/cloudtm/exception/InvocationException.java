package eu.cloudtm.exception;

/**
 * Represents all the invocation issues
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
