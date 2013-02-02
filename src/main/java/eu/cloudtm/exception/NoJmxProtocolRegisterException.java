package eu.cloudtm.exception;

/**
 * Thrown when no JMX protocols are registered
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class NoJmxProtocolRegisterException extends Exception {

    public NoJmxProtocolRegisterException() {
        super("No Jmx Protocols found. Please register at least one protocol");
    }
}
