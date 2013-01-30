package eu.cloudtm.jmxprotocol;

import javax.management.remote.JMXServiceURL;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public interface JmxProtocol {

    JMXServiceURL createUrl(String hostname, String port);

}
