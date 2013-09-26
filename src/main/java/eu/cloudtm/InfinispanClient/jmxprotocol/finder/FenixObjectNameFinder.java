package eu.cloudtm.InfinispanClient.jmxprotocol.finder;

import org.apache.log4j.Logger;

import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import java.util.Properties;
import java.util.Set;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public class FenixObjectNameFinder extends ObjectNameFinder {

    private static final Logger log = Logger.getLogger(FenixObjectNameFinder.class);
    //eg: pt.ist.fenixframework:application="geograph",module=ispn,category=messaging,component=MessaginThreadPool
    private static final String FENIX_COMPONENT_QUERY_FORMAT = "%s:application=\"%s\",component=%s,*";
    private String jmxDomain;
    private String appName;

    public synchronized final void update(Properties properties) {
        this.jmxDomain = properties.getProperty("fenix.jmxDomain");
        this.appName = properties.getProperty("fenix.appName");
    }

    public synchronized final Set<ObjectName> findFenixComponent(MBeanServerConnection connection, String component) {
        return find(connection, createQuery(component));
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    private ObjectName createQuery(String component) {
        try {
            return new ObjectName(String.format(FENIX_COMPONENT_QUERY_FORMAT, jmxDomain, appName, component));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

}
