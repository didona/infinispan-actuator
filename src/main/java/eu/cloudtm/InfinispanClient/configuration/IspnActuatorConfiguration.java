package eu.cloudtm.InfinispanClient.configuration;


import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * // TODO: Document this
 *
 * @author diego
 * @since 4.0
 */
public class IspnActuatorConfiguration extends PropertiesConfiguration {

   public IspnActuatorConfiguration(String fileName) throws ConfigurationException {
      super(fileName);
   }

   public boolean useJboss() {
      return getString(KEYS.USE_JBOSS.key).equals("true");
   }


   private enum KEYS {

      USE_JBOSS("use_jboss");

      private final String key;

      private KEYS(String key) {
         this.key = key;
      }

      String key() {
         return key;
      }


   }
}
