package eu.cloudtm.InfinispanClient.configuration;


import org.apache.commons.configuration.ConfigurationException;

/**
 * // TODO: Document this
 *
 * @author diego
 * @since 4.0
 */
public class ConfigurationFactory {
   private static IspnActuatorConfiguration instance;

   public static IspnActuatorConfiguration getInstance() {
      if (instance == null) {
         try {
            instance = new IspnActuatorConfiguration("config/infinispan-actuator/config.properties");
         } catch (ConfigurationException e) {
            throw new RuntimeException(e);
         }
      }
      return instance;
   }
}
