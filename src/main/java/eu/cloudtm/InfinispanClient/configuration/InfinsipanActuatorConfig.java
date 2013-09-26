package eu.cloudtm.InfinispanClient.configuration;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * // TODO: Document this
 *
 * @author diego
 * @since 4.0
 */
public class InfinsipanActuatorConfig extends PropertiesConfiguration {

   public InfinsipanActuatorConfig(String fileName) throws ConfigurationException {
      super(fileName);
   }

   public boolean useRemoting() {
      return getString(KEYS.USE_REMOTING.key).equals("true");
   }

   public boolean useRMI() {
      return getString(KEYS.USE_RMI.key).equals("true");
   }

   public String getInfinispanDomain() {
      return getString(KEYS.ISPN_DOMAIN.key);
   }

   public String getFenixDomain() {
      return getString(KEYS.FENIX_DOMAIN.key);
   }

   public String getFenixAppName() {
      return getString(KEYS.FENIX_APP.key);
   }

   public String getInfinispanCacheName() {
      return getString(KEYS.ISPN_CACHE.key);
   }

   public String getInfinispanCacheManagerName() {
      return getString(KEYS.ISPN_CACHEMANAGER.key);
   }


   private enum KEYS {

      USE_REMOTING("protocols.useRemoting"),
      USE_RMI("protocols.useRmi"),
      ISPN_DOMAIN("infinispan.jmxDomain"),
      FENIX_DOMAIN("fenix.jmxDomain"),
      FENIX_APP("fenix.appName"),
      ISPN_CACHE("infinispan.cacheName"),
      ISPN_CACHEMANAGER("infinispan.cacheManager");

      private final String key;

      private KEYS(String key) {
         this.key = key;
      }

      String key() {
         return key;
      }


   }
}
