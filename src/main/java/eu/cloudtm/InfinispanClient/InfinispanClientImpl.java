/*
 * INESC-ID, Instituto de Engenharia de Sistemas e Computadores Investigação e Desevolvimento em Lisboa
 * Copyright 2013 INESC-ID and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 3.0 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package eu.cloudtm.InfinispanClient;

import eu.cloudtm.InfinispanClient.configuration.ConfigurationFactory;
import eu.cloudtm.InfinispanClient.configuration.InfinsipanActuatorConfig;
import eu.cloudtm.InfinispanClient.exception.ComponentNotFoundException;
import eu.cloudtm.InfinispanClient.exception.ConnectionException;
import eu.cloudtm.InfinispanClient.exception.HostnameNotFoundException;
import eu.cloudtm.InfinispanClient.exception.InvocationException;
import eu.cloudtm.InfinispanClient.exception.NoJmxProtocolRegisterException;
import eu.cloudtm.InfinispanClient.jmxprotocol.finder.FenixObjectNameFinder;
import eu.cloudtm.InfinispanClient.jmxprotocol.finder.InfinispanObjectNameFinder;
import eu.cloudtm.InfinispanClient.jmxprotocol.protocol.JmxProtocol;
import eu.cloudtm.InfinispanClient.jmxprotocol.protocol.JmxRMIProtocol;
import eu.cloudtm.InfinispanClient.jmxprotocol.protocol.RemotingJmxProtocol;
import org.apache.log4j.Logger;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.ReflectionException;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * This class allows to the programmer to invoke JMX methods in single/any/multiple {@link InfinispanMachine}. <br/> It
 * allows to add your own JMX protocol. By default, the RMI and the JBoss' remoting-jmx is already registered. <br/>
 *
 * @author Pedro Ruivo
 * @author Fabio Perfetti
 * @author Diego Didona
 * @since 1.0
 */
public class InfinispanClientImpl implements InfinispanClient {

   private static final Logger log = Logger.getLogger(InfinispanClientImpl.class);

   private static final int MAX_RETRIES = 100;
   private static final long TIME_TO_SLEEP_MS = 2000;

   private final String infinispanDomain;
   private final String cacheName;
   private final String cacheManagerName;


   private final Map<InfinispanMachine, MBeanServerConnection> machine2connection = new HashMap<InfinispanMachine, MBeanServerConnection>();

   //private MBeanServerConnection mBeanServerConnection = null;

   /**
    * represents an invocation error. used in {@link #invokeInAllMachines(String, String, Object[], String[])}
    */
   public static final Object ERROR_INVOKING = new Object();

   /**
    * an array to use when the method to invoke has no parameters. using this array is not mandatory but it will avoid
    * creating every time a new array
    */
   public static final Object[] EMPTY_PARAMETER = new Object[0];

   /**
    * an array to use when the method to invoke has no parameters. using this array is not mandatory but it will avoid
    * creating every time a new array
    */
   public static final String[] EMPTY_SIGNATURE = new String[0];


   private final InfinispanObjectNameFinder infinispanObjectNameFinder;
   private final FenixObjectNameFinder fenixObjectNameFinder;

   private final Set<JmxProtocol> protocols = new HashSet<JmxProtocol>() {{
      InfinsipanActuatorConfig config = ConfigurationFactory.getInstance();
      final boolean trace = log.isTraceEnabled();
      if (config.useRMI()) {
         if (trace) log.trace("Activating RMI protocol for Infinispan Actuator");
         add(new JmxRMIProtocol());
      }
      if (config.useRemoting()) {
         if (trace) log.trace("Activating Remoting protocol for Infinispan Actuator");
         add(new RemotingJmxProtocol());
      }

   }};

   private final Set<InfinispanMachine> machines;

   /**
    * Create an instance with given ispn information and file-injected fenix information
    *
    * @param infinispanMachines
    * @param infinispanDomain
    * @param cacheName
    * @param cacheManagerName
    */
   public InfinispanClientImpl(Set<InfinispanMachine> infinispanMachines,
                               String infinispanDomain, String cacheName, String cacheManagerName
   ) {
      this.machines = new HashSet<InfinispanMachine>(infinispanMachines);
      this.infinispanDomain = infinispanDomain;
      this.cacheName = cacheName;
      this.cacheManagerName = cacheManagerName;

      this.infinispanObjectNameFinder = new InfinispanObjectNameFinder(infinispanDomain, cacheName, cacheManagerName);
      InfinsipanActuatorConfig config = ConfigurationFactory.getInstance();
      if (config.getFenixDomain() == null)
         this.fenixObjectNameFinder = null;
      else {
         this.fenixObjectNameFinder = new FenixObjectNameFinder(config.getFenixDomain(), config.getFenixAppName());
      }
   }


   public InfinispanClientImpl(Set<InfinispanMachine> infinispanMachines,
                               String infinispanDomain, String cacheName
   ) {
      this.machines = new HashSet<InfinispanMachine>(infinispanMachines);
      this.infinispanDomain = infinispanDomain;
      this.cacheName = cacheName;
      this.cacheManagerName = ConfigurationFactory.getInstance().getInfinispanCacheManagerName();

      this.infinispanObjectNameFinder = new InfinispanObjectNameFinder(infinispanDomain, cacheName, cacheManagerName);
      InfinsipanActuatorConfig config = ConfigurationFactory.getInstance();
      if (!config.isFenixActive())
         this.fenixObjectNameFinder = null;
      else {
         this.fenixObjectNameFinder = new FenixObjectNameFinder(config.getFenixDomain(), config.getFenixAppName());
      }
   }

   /**
    * Create an instance with everything file injected
    *
    * @param infinispanMachines
    */
   public InfinispanClientImpl(Set<InfinispanMachine> infinispanMachines) {
      this.machines = new HashSet<InfinispanMachine>(infinispanMachines);
      InfinsipanActuatorConfig config = ConfigurationFactory.getInstance();

      this.infinispanDomain = config.getInfinispanDomain();
      this.cacheName = config.getInfinispanCacheName();
      this.cacheManagerName = config.getInfinispanCacheManagerName();

      this.infinispanObjectNameFinder = new InfinispanObjectNameFinder(infinispanDomain, cacheName, cacheManagerName);
      if (config.isFenixActive())
         this.fenixObjectNameFinder = new FenixObjectNameFinder(config.getFenixDomain(), config.getFenixAppName());
      else
         this.fenixObjectNameFinder = null;
   }

   /**
    * Create an instance with all specified params
    *
    * @param infinispanMachines
    * @param infinispanDomain
    * @param cacheName
    * @param cacheManagerName
    * @param fenixJmxDomain
    * @param fenixAppName
    */
   public InfinispanClientImpl(Set<InfinispanMachine> infinispanMachines,
                               String infinispanDomain, String cacheName, String cacheManagerName, String fenixJmxDomain, String fenixAppName
   ) {
      this.machines = new HashSet<InfinispanMachine>(infinispanMachines);
      this.infinispanDomain = infinispanDomain;
      this.cacheName = cacheName;
      this.cacheManagerName = cacheManagerName;

      this.infinispanObjectNameFinder = new InfinispanObjectNameFinder(infinispanDomain, cacheName, cacheManagerName);

      this.fenixObjectNameFinder = new FenixObjectNameFinder(fenixJmxDomain, fenixAppName);

   }


   /**
    * return a {@link InfinispanMachine} with the given hostname.
    *
    * @param hostname the hostname machine
    */
   private final InfinispanMachine hostname2machine(String hostname) throws HostnameNotFoundException {
      if (hostname == null)
         throw new NullPointerException("hostname cannot be null");

      InfinispanMachine ret = null;
      synchronized (machines) {
         for (InfinispanMachine machine : machines) {
            if (machine.getHostname().contains(hostname)) {
               ret = machine;
               break;
            }
         }
      }
      if (ret == null) {
         throw new HostnameNotFoundException("Hostname " + hostname + " doesn't belong to any machine!!");
      }
      return ret;
   }


   /**
    * triggers the data placement optimizer.
    *
    * @param infinispanDomain the Infinispan JMX domain, it is like registered in Infinispan configuration file
    * @param cacheName        the cache name
    * @throws NoJmxProtocolRegisterException if no Jmx Protocol is registered
    * @throws InvocationException            if the method was not invoked successfully in any {@link InfinispanMachine}
    *                                        registered.
    */
   @Deprecated
   public final void triggerDataPlacement(String infinispanDomain, String cacheName) throws InvocationException, NoJmxProtocolRegisterException {
      invokeOnceInAnyMachine("DataPlacementManager", "dataPlacementRequest",
                             EMPTY_PARAMETER, EMPTY_SIGNATURE, false);
   }

   /**
    * triggers the switch mechanism to a new replication protocol
    *
    * @param infinispanDomain the Infinispan JMX domain, it is like registered in Infinispan configuration file
    * @param cacheName        the cache name
    * @param protocolId       the new replication protocol ID
    * @param forceStop        true if you want to force the use of stop-the-world model
    * @param abortOnStop      (only for stop-the-world model) if true, it aborts the running transactions instead of
    *                         waiting for them to finish
    * @throws NoJmxProtocolRegisterException if no JMX Protocol is registered
    * @throws InvocationException            if the method was not invoked successfully in any {@link InfinispanMachine}
    *                                        registered.
    */
   @Deprecated
   public final void triggerSwitchReplicationProtocol(String infinispanDomain, String cacheName,
                                                      String fenixFrameworkDomain, String applicationName,
                                                      String protocolId, boolean forceStop, boolean abortOnStop)
         throws NoJmxProtocolRegisterException, InvocationException {

      log.trace("Triggering switch replication protocol without contacting the primary first");
      invokeOnWorkerOnceInAnyMachine(fenixFrameworkDomain, applicationName, "setProtocol",
                                     new Object[]{protocolId}, new String[]{"String"});
      invokeOnceInAnyMachine("ReconfigurableReplicationManager",
                             "switchTo", new Object[]{protocolId, forceStop, abortOnStop},
                             new String[]{"String", "boolean", "boolean"}, false);
   }

   /**
    * triggers the replication degree mechanism to change it to a new replication degree
    *
    * @param infinispanDomain  the Infinispan JMX domain, it is like registered in Infinispan configuration file
    * @param cacheName         the cache name
    * @param replicationDegree the new replication degree
    * @throws NoJmxProtocolRegisterException if no JMX Protocol is registered
    * @throws InvocationException            if the method was not invoked successfully in any {@link InfinispanMachine}
    *                                        registered.
    */
   @Deprecated
   public final void triggerNewReplicationDegree(String infinispanDomain, String cacheName, int replicationDegree)
         throws NoJmxProtocolRegisterException, InvocationException {
      invokeOnceInAnyMachine("DataPlacementManager",
                             "setReplicationDegree", new Object[]{replicationDegree},
                             new String[]{"int"}, false);
   }

   /**
    * Generic JMX invocation method, that is only invoked in the specific {@param machine}
    *
    * @param machine       the machine to be invoked
    * @param componentName the component name
    * @param methodName    the method name
    * @param parameter     the method's parameters
    * @param signature     the method's signature
    * @return the value returned by the method invocation
    * @throws NoJmxProtocolRegisterException if no JMX Protocol is registered
    * @throws ConnectionException            if the connection ti the {@param machine} was not successfully made by any
    *                                        of the JMX protocols
    * @throws ComponentNotFoundException     if the component specified was not found
    * @throws InvocationException            if the method was not invoked successfully
    */
   public final Object invokeInMachine(InfinispanMachine machine, String componentName, String methodName,
                                       Object[] parameter, String[] signature)
         throws NoJmxProtocolRegisterException, InvocationException {

      log.info("Invoke in machine [" + machine + "] method " + componentName + "." + methodName + Arrays.toString(signature));

      try {
         MBeanServerConnection connection = createConnection(machine);
         ObjectName objectName = findCacheComponent(connection, infinispanDomain, cacheName, componentName);
         return connection.invoke(objectName, methodName, parameter, signature);
      } catch (ReflectionException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + methodName +
                        Arrays.toString(signature), e);
         throw new InvocationException(e);
      } catch (MBeanException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + methodName +
                        Arrays.toString(signature), e);
         throw new InvocationException(e);
      } catch (InstanceNotFoundException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + methodName +
                        Arrays.toString(signature), e);
         throw new InvocationException(e);
      } catch (IOException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + methodName +
                        Arrays.toString(signature), e);
         throw new InvocationException(e);
      } catch (ConnectionException e) {
         throw new InvocationException(e);
      } catch (ComponentNotFoundException e) {
         throw new InvocationException(e);
      }
   }

   /**
    * Generic JMX invocation method, that is only invoked in the specific {@param machine}
    *
    * @param machine       the machine to be invoked
    * @param componentName the component name
    * @param attributeName the attribute name
    * @return the value returned by the method invocation
    * @throws NoJmxProtocolRegisterException if no JMX Protocol is registered
    * @throws ConnectionException            if the connection ti the {@param machine} was not successfully made by any
    *                                        of the JMX protocols
    * @throws ComponentNotFoundException     if the component specified was not found
    * @throws InvocationException            if the method was not invoked successfully
    */
   public final Object getAttributeInMachine(InfinispanMachine machine, String componentName, String attributeName)
         throws NoJmxProtocolRegisterException, InvocationException {

      log.trace("GetAttribute in machine [" + machine + "] method " + componentName + "." + attributeName);

      try {
         MBeanServerConnection connection = createConnection(machine);
         ObjectName objectName = findCacheComponent(connection, infinispanDomain, cacheName, componentName);
         return connection.getAttribute(objectName, attributeName);
      } catch (ReflectionException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + attributeName, e);
         throw new InvocationException(e);
      } catch (MBeanException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + attributeName, e);
         throw new InvocationException(e);
      } catch (InstanceNotFoundException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + attributeName, e);
         throw new InvocationException(e);
      } catch (IOException e) {
         log.warn("[" + machine + "] error in method " + componentName + "." + attributeName, e);
         throw new InvocationException(e);
      } catch (ConnectionException e) {
         throw new InvocationException(e);
      } catch (ComponentNotFoundException e) {
         throw new InvocationException(e);
      } catch (AttributeNotFoundException e) {
         throw new InvocationException(e);
      }
   }


   /**
    * Generic JMX invocation, that invokes the method in all the machines.
    *
    * @param componentName the component name
    * @param attributeName the method name
    * @return a map between the {@link InfinispanMachine} and the returned value. If the invocation fails for some
    *         machine, them the value is equals to {@link #ERROR_INVOKING}
    * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
    */
   public final Map<InfinispanMachine, Object> getAttributeInAllMachine(String componentName, String attributeName) {
      log.trace("Invoke in *ALL* machine method " + componentName + "." + attributeName);
      Map<InfinispanMachine, Object> results = new HashMap<InfinispanMachine, Object>();
      MBeanServerConnection connection;
      ObjectName objectName;

      synchronized (machines) {
         for (InfinispanMachine machine : machines) {
            try {
               connection = createConnection(machine);
               objectName = findCacheComponent(connection, infinispanDomain, cacheName, componentName);
               Object result = connection.getAttribute(objectName, attributeName);
               results.put(machine, result);
            } catch (Exception e) {
               log.debug("invoke in all, [" + machine + "] error in method " + componentName + "." + attributeName, e);
               results.put(machine, ERROR_INVOKING);
            }
         }
      }

      log.debug("invoke in all, returned in method " + componentName + "." + attributeName + " = " + results);
      return results;
   }


   /**
    * Generic JMX invocation that tries to invoke the method in one of the machines registered, ensuring that the method
    * is invoked at least once (or it throws an exception)
    *
    * @param componentName the component name
    * @param methodName    the method name
    * @param parameter     the method's parameters
    * @param signature     the method's signature
    * @param global
    * @return null if set of machines is empty
    * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
    * @throws InvocationException            if the method was not invoked successfully by any machine registered
    */
   public final Object invokeOnceInAnyMachine(String componentName,
                                              String methodName,
                                              Object[] parameter,
                                              String[] signature, boolean global) throws InvocationException, NoJmxProtocolRegisterException {

      log.trace("Invoke in *ANY* machine method " + componentName + "." + methodName + Arrays.toString(signature));

      MBeanServerConnection connection;
      ObjectName objectName;
      Object retVal = null;
      boolean succeeded = false;
      synchronized (machines) {
         log.trace("Number of machines: " + machines);
         Iterator<InfinispanMachine> iter = machines.iterator();
         while (!succeeded && iter.hasNext()) {
            InfinispanMachine machine = iter.next();
            log.trace("trying with " + machine);

            try {
               connection = createConnection(machine);
               objectName = global ? findGlobalComponent(connection, infinispanDomain, componentName) :
                     findCacheComponent(connection, infinispanDomain, cacheName, componentName);
               retVal = connection.invoke(objectName, methodName, parameter, signature);
               log.debug("invoke in any, [" + machine + "] returned in method " + componentName + "." +
                               methodName + Arrays.toString(signature) + " = " + retVal);
               succeeded = true;
            } catch (ConnectionException e) {
               e.printStackTrace();
            } catch (InstanceNotFoundException e) {
               e.printStackTrace();
            } catch (MBeanException e) {
               e.printStackTrace();
            } catch (ReflectionException e) {
               e.printStackTrace();
            } catch (IOException e) {
               e.printStackTrace();
            } catch (ComponentNotFoundException e) {
               e.printStackTrace();
            }
         }
      }
      if (!succeeded)
         throw new InvocationException("An error occurs while trying to invoke " + componentName + "." + methodName + " on each infinispan instance");

      return retVal;
   }

   private void changeProtocolOnFenixIfNeeded(InfinispanMachine machine, String protocol) throws NoJmxProtocolRegisterException, InvocationException {
      if (fenixObjectNameFinder == null) {
         log.trace("Not going to change protocol in Fénix");
         return;
      }
      log.trace("Invoking switchTo on LARD");
      log.debug("Setting protocol to " + protocol + " in " + machine.getHostname() + "(" + machine.getPort() + ")");
      MBeanServerConnection connection = null;
      try {
         connection = createConnection(machine);
      } catch (ConnectionException e) {
         log.error("It was impossible to establish a connection with " + machine);
      }
      Set<ObjectName> fenixObjectNameSet = fenixObjectNameFinder.findFenixComponent(connection, "Worker");
      log.debug("Fenix found: " + fenixObjectNameSet);
      if (fenixObjectNameSet.isEmpty()) {
         log.trace("FenixObjectNameSet is empty. Returning");
         return;
      }
      final ObjectName workerObjectName = fenixObjectNameSet.iterator().next();
      final Object[] params = new Object[]{protocol};
      final String[] signature = new String[]{String.class.getName()};
      try {
         log.trace("Invoking setProtocol on " + workerObjectName);
         connection.invoke(workerObjectName, "setProtocol", params, signature);
      } catch (Exception e) {
         log.error("Error setting protocol in " + machine.getHostname() + "(" + machine.getPort() + ")", e);
      }
      log.debug("Fénix replication protocol successfully set to " + protocol + " on " + machine);
   }


   public final Object invokeOnWorkerOnceInAnyMachine(String fenixFrameworkDomain, String applicationName,
                                                      String methodName, Object[] parameter, String[] signature)
         throws NoJmxProtocolRegisterException, InvocationException {
      if (log.isInfoEnabled()) {
         log.trace("Invoke in *ANY* machine method Worker." + methodName + Arrays.toString(signature));
      }
      MBeanServerConnection connection;
      ObjectName objectName;
      synchronized (machines) {
         for (InfinispanMachine machine : machines) {
            try {
               connection = createConnection(machine);
               objectName = findFenixFrameworkWorker(connection, fenixFrameworkDomain, applicationName);
               Object retVal = connection.invoke(objectName, methodName, parameter, signature);
               if (log.isDebugEnabled()) {
                  log.debug("invoke in any, [" + machine + "] returned in method Worker." +
                                  methodName + Arrays.toString(signature) + " = " + retVal);
               }

            } catch (Exception e) {
               if (log.isDebugEnabled()) {
                  log.debug("invoke in any, [" + machine + "] error in method Worker." +
                                  methodName + Arrays.toString(signature), e);
               }
            }
         }
      }
      throw new InvocationException("An error occurs while trying to invoke Worker." + methodName);
   }

   /**
    * Generic JMX invocation, that invokes the method in all the machines.
    *
    * @param componentName the component name
    * @param methodName    the method name
    * @param parameter     the method's parameters
    * @param signature     the method's signature
    * @return a map between the {@link InfinispanMachine} and the returned value. If the invocation fails for some
    *         machine, them the value is equals to {@link #ERROR_INVOKING}
    * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
    */
   public final Map<InfinispanMachine, Object> invokeInAllMachines(String componentName, String methodName,
                                                                   Object[] parameter, String[] signature) {
      if (log.isInfoEnabled()) {
         log.trace("Invoke in *ALL* machine method " + componentName + "." + methodName + Arrays.toString(signature));
      }
      Map<InfinispanMachine, Object> results = new HashMap<InfinispanMachine, Object>();
      MBeanServerConnection connection;
      ObjectName objectName;

      synchronized (machines) {
         for (InfinispanMachine machine : machines) {
            try {
               connection = createConnection(machine);
               objectName = findCacheComponent(connection, infinispanDomain, cacheName, componentName);
               Object result = connection.invoke(objectName, methodName, parameter, signature);
               results.put(machine, result);
            } catch (Exception e) {
               if (log.isDebugEnabled()) {
                  log.debug("invoke in all, [" + machine + "] error in method " + componentName + "." +
                                  methodName + Arrays.toString(signature), e);
               }
               results.put(machine, ERROR_INVOKING);
            }
         }
      }
      if (log.isDebugEnabled()) {
         log.debug("invoke in all, returned in method " + componentName + "." +
                         methodName + Arrays.toString(signature) + " = " + results);
      }
      return results;
   }

   /**
    * creates a MBean Server Connection to the machine represented by {@param InfinispanMachine}
    *
    * @param machine the machine
    * @return the {@link MBeanServerConnection} to that machine
    * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
    * @throws ConnectionException            if it cannot connect via any JMX protocol registered
    */
   private MBeanServerConnection createConnection(InfinispanMachine machine) throws NoJmxProtocolRegisterException,
                                                                                    ConnectionException {
      synchronized (machine2connection) {
         MBeanServerConnection mBeanServerConnection = machine2connection.get(machine);
         if (mBeanServerConnection == null) {
            Map<String, Object> environment = new HashMap<String, Object>();
            if (machine.getUsername() != null && !machine.getUsername().isEmpty()) {
               environment.put(JMXConnector.CREDENTIALS, new String[]{machine.getUsername(), machine.getPassword()});
            }
            synchronized (protocols) {
               if (protocols.isEmpty()) {
                  throw new NoJmxProtocolRegisterException();
               }

               JMXConnector jmxConnector;

               for (JmxProtocol jmxProtocol : protocols) {
                  log.debug("trying to connect to " + machine + " using " + jmxProtocol);
                  try {
                     jmxConnector = JMXConnectorFactory.connect(jmxProtocol.createUrl(machine.getIp(),
                                                                                      String.valueOf(machine.getPort())), environment);
                  } catch (IOException e) {
                     log.debug("error trying to connect to " + machine + " using " + jmxProtocol, e);
                     continue;
                  }
                  try {
                     mBeanServerConnection = jmxConnector.getMBeanServerConnection();
                     break;
                  } catch (IOException e) {
                     log.debug("error trying to connect to " + machine + " using " + jmxProtocol, e);
                  }
               }
            }
            if (mBeanServerConnection == null) {
               throw new ConnectionException("Cannot Connect to " + machine);
            }
            machine2connection.put(machine, mBeanServerConnection);
         }
         return mBeanServerConnection;
      }
   }

   /**
    * tries to find the cache component defined by {@param infinispanDomain}, {@param cacheName} and {@param component}
    *
    * @param connection       the {@link MBeanServerConnection} in which it tries to find the {@link ObjectName}
    * @param infinispanDomain the Infinispan JMX domain, like it is registered in Infinispan configuration file
    * @param cacheName        the cache name
    * @param component        the component name
    * @return the {@link ObjectName} that represents the component
    * @throws ComponentNotFoundException if the component was not found in this connection
    * @throws ConnectionException        if a connection error occurs while trying to find the component
    */
   private ObjectName findCacheComponent(MBeanServerConnection connection, String infinispanDomain, String cacheName,
                                         String component) throws ComponentNotFoundException, ConnectionException {
      if (log.isDebugEnabled()) {
         log.debug("Trying to find the component defined by: " + infinispanDomain + ":" + cacheName + "." +
                         component);
      }
      try {
         for (ObjectName name : connection.queryNames(null, null)) {
            if (log.isTraceEnabled()) {
               log.trace("[" + infinispanDomain + ":" + cacheName + "." + component + "] Checking ObjectName " +
                               name);
            }
            if (name.getDomain().equals(infinispanDomain)) {
               if ("Cache".equals(name.getKeyProperty("type")) && ObjectName.quote(cacheName).equals(name.getKeyProperty("name")) &&
                     component.equals(name.getKeyProperty("component"))) {
                  if (log.isDebugEnabled()) {
                     log.debug("[" + infinispanDomain + ":" + cacheName + "." + component +
                                     "] ObjectName found: " + name);
                  }
                  return name;
               }
            }
         }
      } catch (IOException e) {
         log.warn("[" + infinispanDomain + ":" + cacheName + "." + component + "] an error occurs", e);
         throw new ConnectionException(e);
      }
      if (log.isDebugEnabled()) {
         log.debug("[" + infinispanDomain + ":" + cacheName + "." + component +
                         "] No ObjectName found");
      }
      throw new ComponentNotFoundException(infinispanDomain, cacheName, component);
   }


   /**
    * tries to find the cache component defined by {@param infinispanDomain}, {@param cacheName} and {@param component}
    *
    * @param connection       the {@link MBeanServerConnection} in which it tries to find the {@link ObjectName}
    * @param infinispanDomain the Infinispan JMX domain, like it is registered in Infinispan configuration file
    * @param component        the component name
    * @return the {@link ObjectName} that represents the component
    * @throws ComponentNotFoundException if the component was not found in this connection
    * @throws ConnectionException        if a connection error occurs while trying to find the component
    */
   private ObjectName findGlobalComponent(MBeanServerConnection connection, String infinispanDomain,
                                          String component) throws ComponentNotFoundException, ConnectionException {
      if (log.isDebugEnabled()) {
         log.debug("Trying to find the component defined by: " + infinispanDomain + ":" + cacheName + "." +
                         component);
      }
      try {
         for (ObjectName name : connection.queryNames(null, null)) {
            if (log.isTraceEnabled()) {
               log.trace("[" + infinispanDomain + ":" + cacheName + "." + component + "] Checking ObjectName " +
                               name);
            }
            if (name.getDomain().equals(infinispanDomain)) {
               if ("CacheManager".equals(name.getKeyProperty("type")) && ObjectName.quote("DefaultCacheManager").equals(name.getKeyProperty("name")) &&
                     component.equals(name.getKeyProperty("component"))) {
                  if (log.isDebugEnabled()) {
                     log.debug("[" + infinispanDomain + ":" + cacheName + "." + component +
                                     "] ObjectName found: " + name);
                  }
                  return name;
               }
            }
         }
      } catch (IOException e) {
         log.warn("[" + infinispanDomain + ":" + cacheName + "." + component + "] an error occurs", e);
         throw new ConnectionException(e);
      }
      if (log.isDebugEnabled()) {
         log.debug("[" + infinispanDomain + ":" + cacheName + "." + component +
                         "] No ObjectName found");
      }
      throw new ComponentNotFoundException(infinispanDomain, cacheName, component);
   }


   private ObjectName findFenixFrameworkWorker(MBeanServerConnection connection, String fenixFrameworkDomain,
                                               String applicationName)
         throws ComponentNotFoundException, ConnectionException {
      if (log.isDebugEnabled()) {
         log.debug("Trying to find the component defined by: " + fenixFrameworkDomain + ":" + applicationName);
      }
      try {
         for (ObjectName name : connection.queryNames(null, null)) {
            if (log.isTraceEnabled()) {
               log.trace("[" + fenixFrameworkDomain + ":" + applicationName + "] Checking ObjectName " +
                               name);
            }
            if (name.getDomain().equals(fenixFrameworkDomain)) {
               if (ObjectName.quote(applicationName).equals(name.getKeyProperty("application")) &&
                     "messaging".equals(name.getKeyProperty("category")) &&
                     "Worker".equals(name.getKeyProperty("component")) &&
                     applicationName.equals(name.getKeyProperty("remoteApplication"))) {
                  if (log.isDebugEnabled()) {
                     log.debug("[" + fenixFrameworkDomain + ":" + applicationName + "] ObjectName found: " +
                                     name);
                  }
                  return name;
               }
            }
         }
      } catch (IOException e) {
         log.warn("[" + fenixFrameworkDomain + ":" + applicationName + "] an error occurs", e);
         throw new ConnectionException(e);
      }
      if (log.isDebugEnabled()) {
         log.debug("[" + fenixFrameworkDomain + ":" + applicationName + "] No ObjectName found");
      }
      throw new ComponentNotFoundException(fenixFrameworkDomain, applicationName);
   }


    /* ************************* */
    /* **** Fabio's methods **** */
    /* ************************* */

   @Override
   public void triggerBlockingSwitchReplicationDegree(int degreeToApply) throws InvocationException, NoJmxProtocolRegisterException {

      // 1. retrieve coordinator
      InfinispanMachine coordinator = retrieveCoordinator();

      // 1.a ask current rep degree
      Long currRepDegree = (Long) getAttributeInMachine(coordinator, "ExtendedStatistics", "replicationDegree");

      if (currRepDegree == degreeToApply) {
         log.info("Already using replication degree " + currRepDegree);
         return;
      }

      // 2. execute switchTo, it returns currentRoundId + 1
      invokeInMachine(coordinator, "DataPlacementManager", "setReplicationDegree",
               new Object[]{degreeToApply}, new String[]{"int"});

      // 3. Spin while all the nodes are on the same roundId && !roundInProgress
      Set<InfinispanMachine> changingSet = new HashSet<InfinispanMachine>(machines);

      boolean awake = false;
      int maxRetries = MAX_RETRIES;

      while (!changingSet.isEmpty() && !awake && maxRetries > 0) {

         // 3.a asking roundId to all nodes
          Map<InfinispanMachine, Object> replicationDegrees = getAttributeInAllMachine("ExtendedStatistics",
                  "replicationDegree");
          Map<InfinispanMachine, Object> stateTransferInProgress = getAttributeInAllMachine("StateTransferManager",
                  "stateTransferInProgress");

          for (Map.Entry<InfinispanMachine, Object> entry : replicationDegrees.entrySet()) {
              InfinispanMachine currentMachine = entry.getKey();
              Object replicationDegreeObject = entry.getValue();
              Object stateInProgressObject = stateTransferInProgress.get(currentMachine);

              if (isNullOrError(replicationDegreeObject) || isNullOrError(stateInProgressObject)) {
                  log.warn("An error occurred during connection with " + currentMachine + "! Removing the machine...");
                  changingSet.remove(currentMachine);
              } else {
                  long replicationDegree = (Long) replicationDegreeObject;
                  boolean stateInProgress = (Boolean) stateInProgressObject;
                  if (replicationDegree == degreeToApply && !stateInProgress) {
                      // 3.b currentMachine is aligned on the round...asking isRoundInProgress
                      changingSet.remove(currentMachine);
                  }
              }
          }

         try {
            Thread.sleep(TIME_TO_SLEEP_MS);
            maxRetries--;
         } catch (InterruptedException e) {
            log.warn("Awake...exit!");
            awake = true;
         }
      }

      if (maxRetries <= 0) {
         log.warn("WARNING number of retries exceeded! I should throw an exception...");
      }

   }

    private boolean isNullOrError(Object value) {
        return value == null || value == ERROR_INVOKING;
    }

   private Long changeProtocolOn(InfinispanMachine coordinator, String protocolIdToApply, boolean forceStop, boolean abortOnStop) throws InvocationException, NoJmxProtocolRegisterException {


      // 1.a ask current rep protocol
      String currProtocolId = (String) getAttributeInMachine(
            coordinator,
            "ReconfigurableReplicationManager",
            "currentProtocolId"
      );

      if (currProtocolId.equals(protocolIdToApply)) {
         log.info("Already using replication protocol " + currProtocolId + " thus skipping the reconfgiuration");
         return null;
      }

      // 2. execute switchTo, it returns currentEpoch + 1
      log.trace("Invoking switchTo on ispn coordinator");
      return (Long) invokeInMachine(coordinator, "ReconfigurableReplicationManager", "switchTo",
                                    new Object[]{protocolIdToApply, forceStop, abortOnStop},
                                    new String[]{"java.lang.String", "boolean", "boolean"});
   }

   private void ensureProtocolSwitched(long currentEpoch) throws NoJmxProtocolRegisterException, InvocationException {
      Set<InfinispanMachine> changingSet = new HashSet(machines);

      boolean awake = false;
      int maxRetries = MAX_RETRIES;
      log.trace("Going to wait until all replica are aligned with epoch...");
      while (!changingSet.isEmpty() && !awake && maxRetries > 0) {
         Map<InfinispanMachine, Object> machine2epoch =
               getAttributeInAllMachine("ReconfigurableReplicationManager", "currentEpoch");

         for (Map.Entry<InfinispanMachine, Object> entry : machine2epoch.entrySet()) {
            InfinispanMachine currentMachine = entry.getKey();

            if (entry.getValue() == ERROR_INVOKING) {
               log.warn("An error occurred during connection with " + currentMachine + "! Removing the machine...");
               changingSet.remove(currentMachine);
            } else {
               Long epochForCurrentMachine = (Long) entry.getValue();
               if (epochForCurrentMachine == currentEpoch) {
                  // 3.b currentMachine is aligned on the round...asking currentState
                  String currentState = (String) getAttributeInMachine(currentMachine,
                                                                       "ReconfigurableReplicationManager", "currentState");
                  if (currentState.equals("SAFE")) {
                     changingSet.remove(currentMachine);
                  } else {
                     log.trace("Machine " + entry.getKey().getIp() + " is aligned with epoch " + currentEpoch + " but the state is not safe yet");
                  }
               } else {
                  log.trace("Machine " + entry.getKey().getIp() + " is not aligned with epoch: its is " + epochForCurrentMachine + " but the current is " + currentEpoch);
               }
            }
         }

         try {
            Thread.sleep(TIME_TO_SLEEP_MS);
            maxRetries--;
         } catch (InterruptedException e) {
            log.warn("Awake...exit!");
            awake = true;
         }
      }

      if (maxRetries <= 0) {
         log.warn("WARNING number of retries exceeded! I should throw an exception...");
      }


   }

   @Override
   public void triggerBlockingSwitchReplicationProtocol(String protocolIdToApply, boolean forceStop, boolean abortOnStop) throws InvocationException, NoJmxProtocolRegisterException {
      // 1. retrieve coordinator
      InfinispanMachine coordinator = retrieveCoordinator();
      log.trace("Invoking protocol switching on coordinator " + coordinator);
      Long currentEpoch = changeProtocolOn(coordinator, protocolIdToApply, forceStop, abortOnStop);
      log.trace("Change protocol invoked on coordinator: new epoch will be " + currentEpoch);
      //2. Change protocol also on lard, if necessary
      changeProtocolOnFenixIfNeeded(coordinator, protocolIdToApply);
      // 3. Spin while all the nodes are on the same epoch
      log.trace("Now ensuring protocol switch has completed successfully");
      ensureProtocolSwitched(currentEpoch);


   }


   @Override
   public void triggerRebalancing(boolean enabled) throws InvocationException, NoJmxProtocolRegisterException {


      invokeOnceInAnyMachine("LocalTopologyManager",
                             "setRebalancingEnabled",
                             new Object[]{enabled},
                             new String[]{"boolean"},
                             true);

   }


   @Override
   public void triggerBlockingDataPlacement() {
      throw new RuntimeException("TO IMPLEMENT");
   }

   private InfinispanMachine retrieveCoordinator() throws InvocationException, NoJmxProtocolRegisterException {
      final boolean trace = log.isTraceEnabled();
      if (trace)
         log.trace("Going to retrieve the coordinator");
      String hostname = (String) invokeOnceInAnyMachine("DataPlacementManager", "getCoordinatorHostName", EMPTY_PARAMETER, EMPTY_SIGNATURE, false);
      if (trace)
         log.trace("The coordinator is " + hostname);
      InfinispanMachine coordinator = null;

      boolean coordinatorFound = false;
      /**
       * Adding this lopp because while scaling down several times, the coordinator could change quickly...
       * and some slave could not be update.
       */
      while (!coordinatorFound) {
         try {
            coordinator = hostname2machine(hostname);
            coordinatorFound = true;
         } catch (HostnameNotFoundException e) {
            coordinatorFound = false;
            log.trace(e);
         }
      }

      log.info("Coordinator: [ hostname: " + coordinator.getHostname() + ", ip: " + coordinator.getIp() + " ]");
      return coordinator;
   }


   public final void triggerDataPlacement() throws NoJmxProtocolRegisterException, ConnectionException {
      MBeanServerConnection connection;
      Set<ObjectName> ispnObjectNameSet;
      ObjectName dataPlacementObjectName;
      for (InfinispanMachine machine : machines) {
         connection = createConnection(machine);
         log.debug("Triggering data placement in " + machine.getIp() + "(" + machine.getPort() + ")");
         ispnObjectNameSet = infinispanObjectNameFinder.findCacheComponent(connection, "DataPlacementManager");
         if (ispnObjectNameSet.isEmpty()) {
            log.debug("No ObjectName found searching for DataPlacementManager");
            return;
         }
         log.debug("Found: " + ispnObjectNameSet);
         dataPlacementObjectName = ispnObjectNameSet.iterator().next();
         try {
            connection.invoke(dataPlacementObjectName, "dataPlacementRequest", EMPTY_PARAMETER, EMPTY_SIGNATURE);
            log.debug("Data placement successfully triggered on " + machine.getIp() + "(" + machine.getPort() + ")");
         } catch (Exception e) {
            log.error("Error triggering dataplacement on " + machine.getIp() + "(" + machine.getPort() + ")", e);
         }
      }


   }


}


//    /**
//     * it register a new {@link JmxProtocol}. By default, the RMI and the remoting-jmx protocols are already registered
//     *
//     * @param protocol the JMX protocol
//     * @return true if successed, false otherwise
//     *
//     */
//    private final boolean registerJmxProtocol(JmxProtocol protocol) {
//        if(protocol == null)
//            return false;
//
//        boolean ret;
//        synchronized (protocols) {
//            ret = protocols.add(protocol);
//            if (ret) {
//                log.info("Registered a new JmxProtocol: " + protocol);
//            } else {
//                log.info("Tried to register a already existing JmxProtocol [" + protocol + "]... ignored");
//            }
//        }
//        return ret;
//    }
//
//
//    /**
//     * adds a new {@link InfinispanMachine} to the machine list.
//     *
//     * @param machine the new machine
//     */
//    private final boolean addMachine(InfinispanMachine machine) {
//        if(machine == null)
//            return false;
//
//        boolean ret;
//        synchronized (machines) {
//            ret = machines.add(machine);
//            if (ret) {
//                log.info("Added a new InfinispanMachine: " + machine);
//            } else {
//                log.info("Tried to add a already existing InfinispanMachine [" + machine + "]... ignored");
//            }
//        }
//        return ret;
//    }
//
//    /**
//     * removes the machine with the {@param hostname} and {@param port} from the available machines list.
//     *
//     * @param hostname the hostname
//     * @param port     the port (String representation)
//     */
//    private final boolean removeMachine(String hostname, int port, String ip) {
//        boolean ret;
//        synchronized (machines) {
//            InfinispanMachine toRemove = new InfinispanMachine(hostname, port, ip);
//
//            ret = machines.remove(toRemove);
//            if(ret){
//                log.info("Removed machine " + toRemove);
//            } else {
//                log.info("Tried to remove the machine [" + hostname + ":" + port + "] but it was not found");
//            }
//        }
//        return ret;
//    }
//
//    /**
//     * removes all the machines with the {@param hostname} independent of the port
//     *
//     * @param hostname the hostname
//     */
//    private final void removeMachines(String hostname) {
//        synchronized (machines) {
//            for (InfinispanMachine machine : machines) {
//                if (machine.getHostname().equals(hostname)) {
//                    machines.remove(machine);
//                    log.info("Removing machines with hostname " + hostname + ". Removed " + machine);
//                }
//            }
//        }
//    }
