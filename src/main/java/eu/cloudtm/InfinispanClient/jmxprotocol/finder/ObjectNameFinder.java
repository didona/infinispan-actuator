package eu.cloudtm.InfinispanClient.jmxprotocol.finder;

import org.apache.log4j.Logger;

import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import java.io.IOException;
import java.util.Collections;
import java.util.Set;

/**
 * @author Pedro Ruivo
 * @since 1.0
 */
public abstract class ObjectNameFinder {

   protected abstract Logger getLog();

   protected final Set<ObjectName> find(MBeanServerConnection connection, ObjectName query) {
      getLog().debug("Querying " + query + " using as connection " + connection);
      if (connection == null) {
         return Collections.emptySet();
      }

      try {
         Set<ObjectName> objectNameSet = connection.queryNames(query, null);
         if (!objectNameSet.isEmpty()) {
            return objectNameSet;
         }
      } catch (IOException e) {
         e.printStackTrace();
         //ignored
      }
      return Collections.emptySet();
   }


}
