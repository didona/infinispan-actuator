package eu.cloudtm.jmxprotocol;

import javax.management.remote.JMXServiceURL;
import java.net.MalformedURLException;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class JmxRMIProtocol implements JmxProtocol {

    @Override
    public JMXServiceURL createUrl(String hostname, String port) {
        try {
            return new JMXServiceURL(String.format("service:jmx:rmi:///jndi/rmi://%s:%s/jmxrmi", hostname, port));
        } catch (MalformedURLException e) {
            //this should never happen
        }
        throw new IllegalStateException("Should never happen!");
    }
}