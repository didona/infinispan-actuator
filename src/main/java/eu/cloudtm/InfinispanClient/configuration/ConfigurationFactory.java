package eu.cloudtm.InfinispanClient.configuration;


import org.apache.commons.configuration.ConfigurationException;

/**
 * // TODO: Document this
 *
 * @author Diego Didona
 * @since 4.0
 */
public class ConfigurationFactory {
   private static InfinsipanActuatorConfig instance;

   public static InfinsipanActuatorConfig getInstance() {
      if (instance == null) {
         try {
            instance = new InfinsipanActuatorConfig("config/infinispan_actuator.properties");
         } catch (ConfigurationException e) {
            throw new RuntimeException(e);
         }
      }
      return instance;
   }
}
