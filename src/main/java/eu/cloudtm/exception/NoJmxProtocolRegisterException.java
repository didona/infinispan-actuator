package eu.cloudtm.exception;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class NoJmxProtocolRegisterException extends Exception {

    public NoJmxProtocolRegisterException() {
        super("No Jmx Protocols found. Please register at least one protocol");
    }
}
