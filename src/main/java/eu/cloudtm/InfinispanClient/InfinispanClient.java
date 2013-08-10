package eu.cloudtm.InfinispanClient;

import eu.cloudtm.InfinispanClient.exception.InvocationException;
import eu.cloudtm.InfinispanClient.exception.NoJmxProtocolRegisterException;

/**
 * Created with IntelliJ IDEA.
 * User: fabio
 * Date: 7/24/13
 * Time: 2:24 PM
 * To change this template use File | Settings | File Templates.
 */
public interface InfinispanClient {

    public void triggerBlockingSwitchReplicationProtocol(String protocolId, boolean forceStop, boolean abortOnStop) throws InvocationException, NoJmxProtocolRegisterException;

    public void triggerBlockingSwitchReplicationDegree(int replicationDegree) throws InvocationException, NoJmxProtocolRegisterException;

    public void triggerBlockingDataPlacement();

    public void triggerRebalancing(boolean enabled) throws InvocationException, NoJmxProtocolRegisterException;
}
