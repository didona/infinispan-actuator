package eu.cloudtm;

import eu.cloudtm.exception.ComponentNotFoundException;
import eu.cloudtm.exception.ConnectionException;
import eu.cloudtm.exception.InvocationException;
import eu.cloudtm.exception.NoJmxProtocolRegisterException;
import eu.cloudtm.jmxprotocol.JmxProtocol;
import eu.cloudtm.jmxprotocol.JmxRMIProtocol;
import eu.cloudtm.jmxprotocol.RemotingJmxProtocol;

import javax.management.*;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import java.io.IOException;
import java.util.*;

/**
 * // TODO: Document this
 *
 * @author Pedro Ruivo
 * @since 1.0
 */
public class InfinispanActuator {

    public static final Object ERROR_INVOKING = new Object();
    public static final Object[] EMPTY_PARAMETER = new Object[0];
    public static final String[] EMPTY_SIGNATURE = new String[0];
    private static final InfinispanActuator INFINISPAN_ACTUATOR = new InfinispanActuator();
    private final List<JmxProtocol> jmxProtocols;
    private final Set<InfinispanMachine> infinispanMachines;

    public InfinispanActuator() {
        this.jmxProtocols = new LinkedList<JmxProtocol>();
        this.infinispanMachines = new HashSet<InfinispanMachine>();
        registerJmxProtocol(new JmxRMIProtocol());
        registerJmxProtocol(new RemotingJmxProtocol());
    }

    public static InfinispanActuator getInstance() {
        return INFINISPAN_ACTUATOR;
    }

    public final void registerJmxProtocol(JmxProtocol protocol) {
        if (protocol == null) {
            return;
        }
        synchronized (jmxProtocols) {
            jmxProtocols.add(protocol);
        }
    }

    public final void addMachine(InfinispanMachine machine) {
        synchronized (infinispanMachines) {
            infinispanMachines.add(machine);
        }
    }

    public final void removeMachine(String hostname, String port) {
        if (port == null) {
            return;
        }
        synchronized (infinispanMachines) {
            for (Iterator<InfinispanMachine> iterator = infinispanMachines.iterator(); iterator.hasNext(); ) {
                InfinispanMachine machine = iterator.next();
                if (machine.getHostname().equals(hostname) && machine.getPort().equals(port)) {
                    iterator.remove();
                }
            }
        }
    }

    public final void removeMachines(String hostname) {
        synchronized (infinispanMachines) {
            for (Iterator<InfinispanMachine> iterator = infinispanMachines.iterator(); iterator.hasNext(); ) {
                InfinispanMachine machine = iterator.next();
                if (machine.getHostname().equals(hostname)) {
                    iterator.remove();
                }
            }
        }
    }

    public final void triggerDataPlacement(String infinispanDomain, String cacheName)
            throws NoJmxProtocolRegisterException, InvocationException {
        Object result = invokeOnceInAnyMachine(infinispanDomain, cacheName, "DataPlacementManager", "dataPlacementRequest",
                EMPTY_PARAMETER, EMPTY_SIGNATURE);
        if (result == ERROR_INVOKING) {
            throw new InvocationException("An error occurs while triggering data placement");
        }
    }

    public final void switchReplicationProtocol(String infinispanDomain, String cacheName, String protocolId,
                                                boolean forceStop, boolean abortOnStop)
            throws NoJmxProtocolRegisterException, InvocationException {
        Object result = invokeOnceInAnyMachine(infinispanDomain, cacheName, "ReconfigurableReplicationManager",
                "switchTo", new Object[]{protocolId, forceStop, abortOnStop},
                new String[]{"String", "boolean", "boolean"});
        if (result == ERROR_INVOKING) {
            throw new InvocationException("An error occurs while triggering data placement");
        }
    }

    public final void switchReplicationDegree(String infinispanDomain, String cacheName, int replicationDegree)
            throws NoJmxProtocolRegisterException, InvocationException {
        Object result = invokeOnceInAnyMachine(infinispanDomain, cacheName, "DataPlacementManager",
                "setReplicationDegree", new Object[]{replicationDegree},
                new String[]{"int"});
        if (result == ERROR_INVOKING) {
            throw new InvocationException("An error occurs while triggering data placement");
        }
    }

    public final Object invokeInMachine(InfinispanMachine machine, String infinispanDomain, String cacheName,
                                        String componentName, String methodName, Object[] parameter,
                                        String[] signature) throws NoJmxProtocolRegisterException, ConnectionException,
            ComponentNotFoundException, InvocationException {
        try {
            MBeanServerConnection connection = createConnection(machine);
            ObjectName objectName = getCacheComponent(connection, infinispanDomain, cacheName, componentName);
            return connection.invoke(objectName, methodName, parameter, signature);
        } catch (ReflectionException e) {
            throw new InvocationException(e);
        } catch (MBeanException e) {
            throw new InvocationException(e);
        } catch (InstanceNotFoundException e) {
            throw new ComponentNotFoundException(e);
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
    }

    public final Object invokeOnceInAnyMachine(String infinispanDomain, String cacheName, String componentName,
                                               String methodName, Object[] parameter, String[] signature)
            throws NoJmxProtocolRegisterException {
        MBeanServerConnection connection;
        ObjectName objectName;
        synchronized (infinispanMachines) {
            for (InfinispanMachine machine : infinispanMachines) {
                try {
                    connection = createConnection(machine);
                    objectName = getCacheComponent(connection, infinispanDomain, cacheName, componentName);
                    return connection.invoke(objectName, methodName, parameter, signature);
                } catch (Exception e) {
                    //no-op
                }
            }
        }
        return ERROR_INVOKING;
    }

    public final Map<InfinispanMachine, Object> invokeInAllMachines(String infinispanDomain, String cacheName,
                                                                    String componentName, String methodName,
                                                                    Object[] parameter, String[] signature)
            throws NoJmxProtocolRegisterException {
        Map<InfinispanMachine, Object> results = new HashMap<InfinispanMachine, Object>();
        MBeanServerConnection connection;
        ObjectName objectName;

        synchronized (infinispanMachines) {
            for (InfinispanMachine machine : infinispanMachines) {
                try {
                    connection = createConnection(machine);
                    objectName = getCacheComponent(connection, infinispanDomain, cacheName, componentName);
                    Object result = connection.invoke(objectName, methodName, parameter, signature);
                    results.put(machine, result);
                } catch (Exception e) {
                    results.put(machine, ERROR_INVOKING);
                }
            }
        }
        return results;
    }

    private MBeanServerConnection createConnection(InfinispanMachine machine) throws NoJmxProtocolRegisterException,
            ConnectionException {
        MBeanServerConnection mBeanServerConnection = null;
        synchronized (jmxProtocols) {
            if (jmxProtocols.isEmpty()) {
                throw new NoJmxProtocolRegisterException();
            }

            Map<String, Object> environment = new HashMap<String, Object>();
            if (machine.getUsername() != null && !machine.getUsername().isEmpty()) {
                environment.put(JMXConnector.CREDENTIALS, new String[]{machine.getUsername(), machine.getPassword()});
            }

            JMXConnector jmxConnector;

            for (JmxProtocol jmxProtocol : jmxProtocols) {
                try {
                    jmxConnector = JMXConnectorFactory.connect(jmxProtocol.createUrl(machine.getHostname(),
                            machine.getPort()), environment);
                } catch (IOException e) {
                    //no luck
                    continue;
                }
                try {
                    mBeanServerConnection = jmxConnector.getMBeanServerConnection();
                    break;
                } catch (IOException e) {
                    //no luck
                }
            }
        }
        if (mBeanServerConnection == null) {
            throw new ConnectionException("Cannot Connect to " + machine);
        }
        return mBeanServerConnection;
    }

    private ObjectName getCacheComponent(MBeanServerConnection connection, String infinispanDomain, String cacheName,
                                         String component) throws ComponentNotFoundException, ConnectionException {
        try {
            for (ObjectName name : connection.queryNames(null, null)) {
                if (name.getDomain().equals(infinispanDomain)) {

                    if ("Cache".equals(name.getKeyProperty("type")) && cacheName.equals(name.getKeyProperty("name")) &&
                            component.equals(name.getKeyProperty("component"))) {
                        return name;
                    }
                }
            }
        } catch (IOException e) {
            throw new ConnectionException(e);
        }
        throw new ComponentNotFoundException(infinispanDomain, cacheName, component);
    }

}
