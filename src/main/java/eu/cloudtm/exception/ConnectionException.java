package eu.cloudtm.exception;

/**
 * Represents all the connection issues with the remote machine
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
