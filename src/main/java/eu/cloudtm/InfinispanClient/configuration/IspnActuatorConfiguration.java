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

   public boolean useRemoting() {
      return getString(KEYS.USE_REMOTING.key).equals("true");
   }
   public boolean useRMI(){
      return getString(KEYS.USE_RMI.key).equals("true");
   }


   private enum KEYS {

      USE_REMOTING("use_remoting"),
      USE_RMI("use_rmi");

      private final String key;

      private KEYS(String key) {
         this.key = key;
      }

      String key() {
         return key;
      }


   }
}
