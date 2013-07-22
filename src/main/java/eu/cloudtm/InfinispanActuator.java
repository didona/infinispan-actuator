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
public class InfinispanActuator {

    /**
     * represents an invocation error. used in
     * {@link #invokeInAllMachines(String, String, String, String, Object[], String[])}
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
    private final Set<JmxProtocol> jmxProtocols;
    private final Set<InfinispanMachine> infinispanMachines;

    public InfinispanActuator() {
        this(new HashSet<JmxProtocol>(), new HashSet<InfinispanMachine>());
    }

    public InfinispanActuator(Set<JmxProtocol> jmxProtocols,
                              Set<InfinispanMachine> infinispanMachines) {
        this.jmxProtocols = new HashSet<JmxProtocol>(jmxProtocols);
        this.infinispanMachines = new HashSet<InfinispanMachine>(infinispanMachines);
    }

    /**
     * it register a new {@link JmxProtocol}. By default, the RMI and the remoting-jmx protocols are already registered
     *
     * @param protocol the JMX protocol
     * @return true if successed, false otherwise
     *
     */
    public final boolean registerJmxProtocol(JmxProtocol protocol) {
        if(protocol == null)
            return false;

        boolean ret;
        synchronized (jmxProtocols) {
            ret = jmxProtocols.add(protocol);
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
    public final boolean addMachine(InfinispanMachine machine) {
        if(machine == null)
            return false;

        boolean ret;
        synchronized (infinispanMachines) {
            ret = infinispanMachines.add(machine);
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
    public final boolean removeMachine(String hostname, int port) {
        boolean ret;
        synchronized (infinispanMachines) {
            InfinispanMachine toRemove = new InfinispanMachine(hostname, port);

            ret = infinispanMachines.remove(toRemove);
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
    public final void removeMachines(String hostname) {
        synchronized (infinispanMachines) {
            for (InfinispanMachine machine : infinispanMachines) {
                if (machine.getHostname().equals(hostname)) {
                    infinispanMachines.remove(machine);
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
    public final void triggerDataPlacement(String infinispanDomain, String cacheName) throws InvocationException, NoJmxProtocolRegisterException {
        invokeOnceInAnyMachine(infinispanDomain, cacheName, "DataPlacementManager", "dataPlacementRequest",
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
    public final void triggerSwitchReplicationProtocol(String infinispanDomain, String cacheName, String protocolId,
                                                       boolean forceStop, boolean abortOnStop)
            throws NoJmxProtocolRegisterException, InvocationException {
        invokeOnceInAnyMachine(infinispanDomain, cacheName, "ReconfigurableReplicationManager",
                "switchTo", new Object[]{protocolId, forceStop, abortOnStop},
                new String[]{"String", "boolean", "boolean"});
    }


    public final void triggerBlockingSwitchReplicationProtocol(String infinispanDomain, String cacheName, String protocolId,
                                                               boolean forceStop, boolean abortOnStop) throws NoJmxProtocolRegisterException, InvocationException {

        triggerSwitchReplicationProtocol(infinispanDomain, cacheName, protocolId, forceStop, abortOnStop);

        synchronized (infinispanMachines) {
            Set<InfinispanMachine> machines = new HashSet<InfinispanMachine>(infinispanMachines);
            while (!machines.isEmpty()){
                for (InfinispanMachine machine : machines){
                    Object result = invokeInMachine(machine, infinispanDomain, cacheName, "ReconfigurableReplicationManager",
                            "getCurrentProtocolState", EMPTY_PARAMETER, EMPTY_SIGNATURE);

                    if( result instanceof String ){
                        String resultString = (String) result;
                        if( resultString.equals("SAFE") ){
                            machines.remove( machine );
                        }
                    } else if( result == ERROR_INVOKING ){
                        machines.remove( machine );
                    }
                }
                log.trace("Still " + machines.size() + " machines changing " );
            }
        }
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
    public final void triggerNewReplicationDegree(String infinispanDomain, String cacheName, int replicationDegree)
            throws NoJmxProtocolRegisterException, InvocationException {
        invokeOnceInAnyMachine(infinispanDomain, cacheName, "DataPlacementManager",
                "setReplicationDegree", new Object[]{replicationDegree},
                new String[]{"int"});
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
    public final void triggerBlockingNewReplicationDegree(String infinispanDomain, String cacheName, int replicationDegree)
            throws NoJmxProtocolRegisterException, InvocationException {

        triggerNewReplicationDegree(infinispanDomain, cacheName, replicationDegree);

        synchronized (infinispanMachines) {


            // PHASE 1: checking that all machines are aligned on the same round
            long round = -1;
            boolean roundChanged = true,
                    aligned = false;

            while( roundChanged || !aligned ){
                Map<InfinispanMachine, Object> results = invokeInAllMachines(infinispanDomain, cacheName, "DataPlacementManager",
                        "getCurrentRoundId", EMPTY_PARAMETER, EMPTY_SIGNATURE);
                roundChanged = false;
                aligned = true;

                for (Map.Entry<InfinispanMachine, Object> entry : results.entrySet()){
                    Object result = entry.getValue();

                    if( result instanceof Long ){
                        Long resultLong = (Long) entry.getValue();
                        if( resultLong > round ){
                            roundChanged = true;
                            round = resultLong;
                            log.trace( "A new round has been found !" );
                        } else if( resultLong < round ){
                            aligned = false;
                            log.trace( "Machines are not aligned!" );
                        }
                    }
                }
            }
            log.trace( "Machines are aligned!" );

            // PHASE 2: checking that all the machines already ended the current round
            Set<InfinispanMachine> machines = new HashSet<InfinispanMachine>(infinispanMachines);
            while (!machines.isEmpty()){

                for(InfinispanMachine machine : machines){

                    Object result = invokeInMachine(machine, infinispanDomain, cacheName, "DataPlacementManager",
                            "isRoundInProgress", EMPTY_PARAMETER, EMPTY_SIGNATURE);

                    if( result instanceof Boolean ){
                        Boolean resultBoolean = (Boolean) result;
                        if( !resultBoolean ){ // finished!
                            machines.remove( machine );
                        }
                    }

                }
            }
        }
        log.info("Data degree changed");
    }


    /**
     * Generic JMX invocation method, that is only invoked in the specific {@param machine}
     *
     * @param machine          the machine to be invoked
     * @param infinispanDomain the Infinispan JMX domain, it is like registered in Infinispan configuration file
     * @param cacheName        the cache name
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
    public final Object invokeInMachine(InfinispanMachine machine, String infinispanDomain, String cacheName,
                                        String componentName, String methodName, Object[] parameter,
                                        String[] signature) throws NoJmxProtocolRegisterException, InvocationException {

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
     * @param infinispanDomain Infinispan JMX domain, like it is registered in Infinispan configuration file
     * @param cacheName        the cache name
     * @param componentName    the component name
     * @param methodName       the method name
     * @param parameter        the method's parameters
     * @param signature        the method's signature
     * @return the value returned by the method invocation
     * @return null if set of infinispanMachines is empty
     * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
     * @throws InvocationException            if the method was not invoked successfully by any machine registered
     */
    public final Object invokeOnceInAnyMachine(String infinispanDomain,
                                               String cacheName,
                                               String componentName,
                                               String methodName,
                                               Object[] parameter,
                                               String[] signature) throws InvocationException, NoJmxProtocolRegisterException {

        log.info("Invoke in *ANY* machine method " + componentName + "." + methodName + Arrays.toString(signature));

        MBeanServerConnection connection;
        ObjectName objectName;
        Object retVal = null;
        boolean succeeded = false;
        synchronized (infinispanMachines) {

            Iterator<InfinispanMachine> iter = infinispanMachines.iterator();
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
                    log.debug(e);
                } catch (InstanceNotFoundException e) {
                    log.debug(e);
                } catch (MBeanException e) {
                    log.debug(e);
                } catch (ReflectionException e) {
                    log.debug(e);
                } catch (IOException e) {
                    log.debug(e);
                } catch (ComponentNotFoundException e) {
                    log.debug(e);
                }
            }
        }
        if(!succeeded)
            throw new InvocationException("An error occurs while trying to invoke " + componentName + "." + methodName + " on each infinispan instance");

        return retVal;
    }

    /**
     * Generic JMX invocation, that invokes the method in all the machines.
     *
     * @param infinispanDomain the Infinispan JMX domain, like it is registered in Infinispan configuration file
     * @param cacheName        the cache name
     * @param componentName    the component name
     * @param methodName       the method name
     * @param parameter        the method's parameters
     * @param signature        the method's signature
     * @return a map between the {@link InfinispanMachine} and the returned value. If the invocation fails for some
     *         machine, them the value is equals to {@link #ERROR_INVOKING}
     * @throws NoJmxProtocolRegisterException if no JMX protocols are registered
     */
    public final Map<InfinispanMachine, Object> invokeInAllMachines(String infinispanDomain, String cacheName,
                                                                    String componentName, String methodName,
                                                                    Object[] parameter, String[] signature) {
        if (log.isInfoEnabled()) {
            log.info("Invoke in *ALL* machine method " + componentName + "." + methodName + Arrays.toString(signature));
        }
        Map<InfinispanMachine, Object> results = new HashMap<InfinispanMachine, Object>();
        MBeanServerConnection connection;
        ObjectName objectName;

        synchronized (infinispanMachines) {
            for (InfinispanMachine machine : infinispanMachines) {
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
        synchronized (jmxProtocols) {
            if (jmxProtocols.isEmpty()) {
                throw new NoJmxProtocolRegisterException();
            }

            JMXConnector jmxConnector;

            for (JmxProtocol jmxProtocol : jmxProtocols) {
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
                    log.trace("["+ infinispanDomain + ":" + cacheName + "." + component + "] Checking ObjectName " +
                            name);
                }
                if (name.getDomain().equals(infinispanDomain)) {
                    if ("Cache".equals(name.getKeyProperty("type")) && cacheName.equals(name.getKeyProperty("name")) &&
                            component.equals(name.getKeyProperty("component"))) {
                        if (log.isDebugEnabled()) {
                            log.debug("["+ infinispanDomain + ":" + cacheName + "." + component +
                                    "] ObjectName found: " + name);
                        }
                        return name;
                    }
                }
            }
        } catch (IOException e) {
            log.warn("["+ infinispanDomain + ":" + cacheName + "." + component + "] an error occurs", e);
            throw new ConnectionException(e);
        }
        if (log.isDebugEnabled()) {
            log.debug("["+ infinispanDomain + ":" + cacheName + "." + component +
                    "] No ObjectName found");
        }
        throw new ComponentNotFoundException(infinispanDomain, cacheName, component);
    }
}
