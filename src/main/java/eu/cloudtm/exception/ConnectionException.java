package eu.cloudtm.exception;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class ConnectionException extends Exception {

    public ConnectionException(Throwable throwable) {
        super(throwable);
    }

    public ConnectionException(String message) {
        super(message);
    }
}
