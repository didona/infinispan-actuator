package eu.cloudtm.exception;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class ComponentNotFoundException extends Exception {

    public ComponentNotFoundException(String domain, String cacheName, String component) {
        super("Component [" + component + "] not found in cache [" + cacheName + "] and domain [" + domain + "]");
    }

    public ComponentNotFoundException(Throwable throwable) {
        super(throwable);
    }
}
