package eu.cloudtm.jmxprotocol;

import javax.management.remote.JMXServiceURL;
import java.net.MalformedURLException;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class RemotingJmxProtocol implements JmxProtocol {

    @Override
    public JMXServiceURL createUrl(String hostname, String port) {
        try {
            return new JMXServiceURL(String.format("service:jmx:remoting-jmx://%s:%s", hostname, port));
        } catch (MalformedURLException e) {
            //no-op
        }
        throw new IllegalStateException("This should never happen");
    }
}
