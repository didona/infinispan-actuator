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
public class InfinispanObjectNameFinder extends ObjectNameFinder {

    private static final Logger log = Logger.getLogger(InfinispanObjectNameFinder.class);
    //eg: it.geograph:type=Cache,name="geograph(dist_sync)",manager="DefaultCacheManager",component=DataPlacementManager
    private static final String CACHE_COMPONENT_QUERY_FORMAT = "%s:type=Cache,name=\"%s(*)\",manager=\"%s\",component=%s,*";
    //eg: it.geograph:type=CacheManager,name="DefaultCacheManager",component=CacheManager
    private static final String CACHE_MANAGER_COMPONENT_QUERY_FORMAT = "%s:type=CacheManager,name=\"%s\",component=%s,*";
    private String jmxDomain;
    private String cacheManager;
    private String cacheName;

    public synchronized void update(Properties properties) {
        this.jmxDomain = properties.getProperty("infinispan.jmxDomain");
        this.cacheManager = properties.getProperty("infinispan.cacheManager");
        this.cacheName = properties.getProperty("infinispan.cacheName");
    }

    public synchronized final Set<ObjectName> findCacheComponent(MBeanServerConnection connection, String component) {
        return find(connection, getCacheComponentQuery(component));
    }

    public synchronized final Set<ObjectName> findCacheManagerComponent(MBeanServerConnection connection, String component) {
        return find(connection, getCacheManagerComponentQuery(component));
    }

    @Override
    protected Logger getLog() {
        return log;
    }

    private ObjectName getCacheComponentQuery(String component) {
        try {
            return new ObjectName(String.format(CACHE_COMPONENT_QUERY_FORMAT, jmxDomain, cacheName, cacheManager, component));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

    private ObjectName getCacheManagerComponentQuery(String component) {
        try {
            return new ObjectName(String.format(CACHE_MANAGER_COMPONENT_QUERY_FORMAT, jmxDomain, cacheManager, component));
        } catch (MalformedObjectNameException e) {
            throw new RuntimeException("Should never happen", e);
        }
    }

}
