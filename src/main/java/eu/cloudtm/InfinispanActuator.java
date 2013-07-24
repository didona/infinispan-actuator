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
package eu.cloudtm;

import eu.cloudtm.exception.ComponentNotFoundException;
import eu.cloudtm.exception.ConnectionException;
import eu.cloudtm.exception.InvocationException;
import eu.cloudtm.exception.NoJmxProtocolRegisterException;
import eu.cloudtm.jmxprotocol.JmxProtocol;
import org.apache.log4j.Logger;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.IOException;
import java.util.*;

/**
 * This class allows to the programmer to invoke JMX methods in single/any/multiple {@link InfinispanMachine}.
 * <br/>
 * It allows to add your own JMX protocol. By default, the RMI and the JBoss' remoting-jmx is already registered.
 * <br/>
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class InfinispanActuator implements IspnActuator {

    private static final int MAX_RETRIES = 100;
    private static final long TIME_TO_SLEEP_MS = 2000;

    private final String infinispanDomain;
    private final String cacheName;
    private final int jmxPort;


    /**
     * represents an invocation error. used in
     * {@link #invokeInAllMachines(String, String, Object[], String[])}
     */
    public static final Object ERROR_INVOKING = new Object();
    /**
     * an array to use when the method to invoke has no parameters. using this array is not mandatory but it will
     * avoid creating every time a new array
     */
    public static final Object[] EMPTY_PARAMETER = new Object[0];
    /**
     * an array to use when the method to invoke has no parameters. using this array is not mandatory but it will
     * avoid creating every time a new array
     */
    public static final String[] EMPTY_SIGNATURE = new String[0];

    private static final Logger log = Logger.getLogger(InfinispanActuator.class);

    private final Set<JmxProtocol> protocols;
    private final Set<InfinispanMachine> machines;

    public InfinispanActuator(Set<JmxProtocol> jmxProtocols,
                              Set<InfinispanMachine> infinispanMachines,
                              String infinispanDomain, String cacheName, int jmxPort) {
        this.protocols = new HashSet<JmxProtocol>(jmxProtocols);
        this.machines = new HashSet<InfinispanMachine>(infinispanMachines);
        this.infinispanDomain = infinispanDomain;
        this.cacheName = cacheName;
        this.jmxPort = jmxPort;
    }

    /**
     * it register a new {@link JmxProtocol}. By default, the RMI and the remoting-jmx protocols are already registered
     *
     * @param protocol the JMX protocol
     * @return true if successed, false otherwise
     *
     */
    private final boolean registerJmxProtocol(JmxProtocol protocol) {
        if(protocol == null)
            return false;

        boolean ret;
        synchronized (protocols) {
            ret = protocols.add(protocol);
            if (ret) {
                log.info("Registered a new JmxProtocol: " + protocol);
            } else {
                log.info("Tried to register a already existing JmxProtocol [" + protocol + "]... ignored");
            }
        }
        return ret;
    }

    /**
     * adds a new {@link InfinispanMachine} to the machine list.
     *
     * @param machine the new machine
     */
    private final boolean addMachine(InfinispanMachine machine) {
        if(machine == null)
            return false;

        boolean ret;
        synchronized (machines) {
            ret = machines.add(machine);
            if (ret) {
                log.info("Added a new InfinispanMachine: " + machine);
            } else {
                log.info("Tried to add a already existing InfinispanMachine [" + machine + "]... ignored");
            }
        }
        return ret;
    }

    /**
     * removes the machine with the {@param hostname} and {@param port} from the available machines list.
     *
     * @param hostname the hostname
     * @param port     the port (String representation)
     */
    private final boolean removeMachine(String hostname, int port) {
        boolean ret;
        synchronized (machines) {
            InfinispanMachine toRemove = new InfinispanMachine(hostname, port);

            ret = machines.remove(toRemove);
            if(ret){
                log.info("Removed machine " + toRemove);
            } else {
                log.info("Tried to remove the machine [" + hostname + ":" + port + "] but it was not found");
            }
        }
        return ret;
    }

    /**
     * removes all the machines with the {@param hostname} independent of the port
     *
     * @param hostname the hostname
     */
    private final void removeMachines(String hostname) {
        synchronized (machines) {
            for (InfinispanMachine machine : machines) {
                if (machine.getHostname().equals(hostname)) {
                    machines.remove(machine);
                    log.info("Removing machines with hostname " + hostname + ". Removed " + machine);
                }
            }
        }
    }

    /**
     * triggers the data placement optimizer.
     *
     * @param infinispanDomain the Infinispan JMX domain, it is like registered in Infinispan configuration file
     * @param cacheName        the cache name
     * @throws NoJmxProtocolRegisterException if no Jmx Protocol is registered
     * @throws InvocationException            if the method was not invoked successfully in any
     *                                        {@link InfinispanMachine} registered.
     */
    @Deprecated
    public final void triggerDataPlacement(String infinispanDomain, String cacheName) throws InvocationException, NoJmxProtocolRegisterException {
        invokeOnceInAnyMachine("DataPlacementManager", "dataPlacementRequest",
                EMPTY_PARAMETER, EMPTY_SIGNATURE);
    }

    /**
     * triggers the switch mechanism to a new replication protocol
     *
     * @param infinispanDomain the Infinispan JMX domain, it is like registered in Infinispan configuration file
     * @param cacheName        the cache name
     * @param protocolId       the new replication protocol ID
     * @param forceStop        true if you want to force the use of stop-the-world model
     * @param abortOnStop      (only for stop-the-world model) if true, it aborts the running transactions instead
     *                         of waiting for them to finish
     * @throws NoJmxProtocolRegisterException if no JMX Protocol is registered
     * @throws InvocationException            if the method was not invoked successfully in any
     *                                        {@link InfinispanMachine} registered.
     */
    @Deprecated
    public final void triggerSwitchReplicationProtocol(String infinispanDomain, String cacheName,
                                                       String fenixFrameworkDomain, String applicationName,
                                                       String protocolId, boolean forceStop, boolean abortOnStop)
            throws NoJmxProtocolRegisterException, InvocationException {
        invokeOnWorkerOnceInAnyMachine(fenixFrameworkDomain, applicationName, "setProtocol",
                new Object[] {protocolId}, new String[] {"String"});
        invokeOnceInAnyMachine("ReconfigurableReplicationManager",
                "switchTo", new Object[]{protocolId, forceStop, abortOnStop},
                new String[]{"String", "boolean", "boolean"});
    }

    /**
     * triggers the replication degree mechanism to change it to a new replication degree
     *
     * @param infinispanDomain  the Infinispan JMX domain, it is like registered in Infinispan configuration file
     * @param cacheName         the cache name
     * @param replicationDegree the new replication degree
     * @throws NoJmxProtocolRegisterException if no JMX Protocol is registered
     * @throws InvocationException            if the method was not invoked successfully in any
     *                                        {@link InfinispanMachine} registered.
     */
    @Deprecated
    public final void triggerNewReplicationDegree(String infinispanDomain, String cacheName, int replicationDegree)
            throws NoJmxProtocolRegisterException, InvocationException {
        invokeOnceInAnyMachine("DataPlacementManager",
                "setReplicationDegree", new Object[]{replicationDegree},
                new String[]{"int"});
    }

    /**
     * Generic JMX invocation method, that is only invoked in the specific {@param machine}
     *
     * @param machine          the machine to be invoked
     * @param componentName    the component name
     * @param methodName       the method name
     * @param parameter        the method's parameters
     * @param signature        the method's signature
     * @return the value returned by the method invocation
     * @throws NoJmxProtocolRegisterException if no JMX Protocol is registered
     * @throws ConnectionException            if the connection ti the {@param machine} was not successfully made by
     *                                        any of the JMX protocols
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
     * Generic JMX invocation that tries to invoke the method in one of the machines registered, ensuring that the
     * method is invoked at least once (or it throws an exception)
     *
     * @param componentName    the component name
     * @param methodName       the method name
     * @param parameter        the method's parameters
     * @param signature        the method's signature
     * @return the value returned by the method invocation
     * @return null if set of machines is empty
     * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
     * @throws InvocationException            if the method was not invoked successfully by any machine registered
     */
    public final Object invokeOnceInAnyMachine(String componentName,
                                               String methodName,
                                               Object[] parameter,
                                               String[] signature) throws InvocationException, NoJmxProtocolRegisterException {

        log.info("Invoke in *ANY* machine method " + componentName + "." + methodName + Arrays.toString(signature));

        MBeanServerConnection connection;
        ObjectName objectName;
        Object retVal = null;
        boolean succeeded = false;
        synchronized (machines) {

            Iterator<InfinispanMachine> iter = machines.iterator();
            while ( !succeeded && iter.hasNext() ){
                InfinispanMachine machine = iter.next();

                try {
                    connection = createConnection(machine);
                    objectName = findCacheComponent(connection, infinispanDomain, cacheName, componentName);
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
        if(!succeeded)
            throw new InvocationException("An error occurs while trying to invoke " + componentName + "." + methodName + " on each infinispan instance");

        return retVal;
    }

    public final Object invokeOnWorkerOnceInAnyMachine(String fenixFrameworkDomain, String applicationName,
                                                       String methodName, Object[] parameter, String[] signature)
            throws NoJmxProtocolRegisterException, InvocationException {
        if (log.isInfoEnabled()) {
            log.info("Invoke in *ANY* machine method Worker." + methodName + Arrays.toString(signature));
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
     * @param componentName    the component name
     * @param methodName       the method name
     * @param parameter        the method's parameters
     * @param signature        the method's signature
     * @return a map between the {@link InfinispanMachine} and the returned value. If the invocation fails for some
     *         machine, them the value is equals to {@link #ERROR_INVOKING}
     * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
     */
    public final Map<InfinispanMachine, Object> invokeInAllMachines(String componentName, String methodName,
                                                                    Object[] parameter, String[] signature) {
        if (log.isInfoEnabled()) {
            log.info("Invoke in *ALL* machine method " + componentName + "." + methodName + Arrays.toString(signature));
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
        MBeanServerConnection mBeanServerConnection = null;
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
                if (log.isDebugEnabled()) {
                    log.debug("trying to connect to " + machine + " using " + jmxProtocol);
                }
                try {
                    jmxConnector = JMXConnectorFactory.connect(jmxProtocol.createUrl( machine.getHostname(),
                            String.valueOf(machine.getPort()) ), environment);
                } catch (IOException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("error trying to connect to " + machine + " using " + jmxProtocol, e);
                    }
                    continue;
                }
                try {
                    mBeanServerConnection = jmxConnector.getMBeanServerConnection();
                    break;
                } catch (IOException e) {
                    if (log.isDebugEnabled()) {
                        log.debug("error trying to connect to " + machine + " using " + jmxProtocol, e);
                    }
                }
            }
        }
        if (mBeanServerConnection == null) {
            throw new ConnectionException("Cannot Connect to " + machine);
        }
        return mBeanServerConnection;
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
    public void triggerBlockingSwitchReplicationDegree(int replicationDegree) throws InvocationException, NoJmxProtocolRegisterException {
        // 1. retrieve coordinator
        InfinispanMachine coordinator = retrieveCoordinator();

        // 2. execute switchTo, it returns currentRoundId + 1
        Long currentRoundId = (Long) invokeInMachine(coordinator, "DataPlacementManager", "setReplicationDegree",
                new Object[]{replicationDegree}, new String[]{"int"});

        // 3. Spin while all the nodes are on the same roundId && !roundInProgress
        Set<InfinispanMachine> changingSet = new HashSet(machines);

        boolean awaked = false;
        int maxRetries = MAX_RETRIES;

        while( !changingSet.isEmpty() && !awaked && maxRetries>0 ){

            // 3.a asking roundId to all nodes
            Map<InfinispanMachine, Object> machine2epoch = invokeInAllMachines("DataPlacementManager",
                    "getCurrentRoundId", EMPTY_PARAMETER, EMPTY_SIGNATURE);

            for( Map.Entry<InfinispanMachine, Object> entry : machine2epoch.entrySet() ){
                InfinispanMachine currentMachine = entry.getKey();
                Long roundForCurrentMachine = (Long) entry.getValue();

                if(roundForCurrentMachine == currentRoundId){
                    // 3.b currentMachine is aligned on the round...asking isRoundInProgress
                    Boolean isRoundInProgress = (Boolean) invokeInMachine(currentMachine, "DataPlacementManager", "isRoundInProgress",
                            EMPTY_PARAMETER, EMPTY_SIGNATURE);
                    if( !isRoundInProgress ){
                        changingSet.remove( currentMachine );
                    }
                }
            }

            try {
                Thread.sleep(TIME_TO_SLEEP_MS);
                maxRetries--;
            } catch (InterruptedException e) {
                log.warn("Awaked...exiting");
                awaked = true;
            }
        }

    }

    @Override
    public void triggerBlockingSwitchReplicationProtocol(String protocolId, boolean forceStop, boolean abortOnStop) throws InvocationException, NoJmxProtocolRegisterException {
        // 1. retrieve coordinator
        InfinispanMachine coordinator = retrieveCoordinator();

        // 2. execute switchTo, it returns currentEpoch + 1
        Long currentEpoch = (Long) invokeInMachine(coordinator, "ReconfigurableReplicationManager", "switchTo", new Object[]{protocolId, forceStop, abortOnStop},
                new String[]{"String", "boolean", "boolean"});

        // 3. Spin while all the nodes are on the same epoch &&
        Set<InfinispanMachine> changingSet = new HashSet(machines);

        boolean awaked = false;
        int maxRetries = MAX_RETRIES;

        while( !changingSet.isEmpty() && !awaked && maxRetries>0 ){
            Map<InfinispanMachine, Object> machine2epoch = invokeInAllMachines("ReconfigurableReplicationManager",
                    "getCurrentEpoch", EMPTY_PARAMETER, EMPTY_SIGNATURE);

            for( Map.Entry<InfinispanMachine, Object> entry : machine2epoch.entrySet() ){
                InfinispanMachine currentMachine = entry.getKey();
                Long epochForCurrentMachine = (Long) entry.getValue();
                if(epochForCurrentMachine == currentEpoch){
                    // 3.b currentMachine is aligned on the round...asking currentState
                    String currentState = (String) invokeInMachine(currentMachine, "ReconfigurableReplicationManager", "getCurrentState",
                            EMPTY_PARAMETER, EMPTY_SIGNATURE);
                    if( currentState.equals("SAFE") ){
                        changingSet.remove( currentMachine );
                    }
                }
            }

            try {
                Thread.sleep(TIME_TO_SLEEP_MS);
                maxRetries--;
            } catch (InterruptedException e) {
                log.warn("Awaked...exiting");
                awaked = true;
            }
        }

    }

    @Override
    public void triggerBlockingDataPlacement() {
        throw new RuntimeException("TO IMPLEMENT");
    }

    private InfinispanMachine retrieveCoordinator() throws InvocationException, NoJmxProtocolRegisterException {
        String hostname = (String) invokeOnceInAnyMachine("DataPlacementManager", "getCoordinatorHostName", EMPTY_PARAMETER, EMPTY_SIGNATURE);
        log.debug("Coordinator's hostname: " + hostname);
        InfinispanMachine coordinator = new InfinispanMachine(hostname, jmxPort);
        return coordinator;
    }


}
