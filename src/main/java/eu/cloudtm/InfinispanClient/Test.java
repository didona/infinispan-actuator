package eu.cloudtm.InfinispanClient;

import eu.cloudtm.InfinispanClient.exception.InvocationException;
import eu.cloudtm.InfinispanClient.exception.NoJmxProtocolRegisterException;
import eu.cloudtm.InfinispanClient.jmxprotocol.protocol.JmxProtocol;
import eu.cloudtm.InfinispanClient.jmxprotocol.protocol.JmxRMIProtocol;
import eu.cloudtm.InfinispanClient.jmxprotocol.protocol.RemotingJmxProtocol;
import org.apache.log4j.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Created with IntelliJ IDEA.
 * User: fabio
 * Date: 7/24/13
 * Time: 4:01 PM
 * To change this template use File | Settings | File Templates.
 */
public class Test {

    private static final Logger log = Logger.getLogger(Test.class);

    public static void main(String[] args){

        Set<JmxProtocol> protocols = new HashSet<JmxProtocol>();
        protocols.add( new JmxRMIProtocol() );
        protocols.add( new RemotingJmxProtocol() );

        Set<InfinispanMachine> machines = new HashSet<InfinispanMachine>();

        System.out.println("Adding infinispan instances...");
        machines.add( new InfinispanMachine("localhost", 9998, "127.0.0.1") );

        InfinispanClientImpl actuator = new InfinispanClientImpl(machines, "org.infinispan", "x(dist_sync)");

        try {
            System.out.println("Switching rep protocol...");
            actuator.triggerBlockingSwitchReplicationProtocol("TO", false, false);
            System.out.println("done!");

            System.out.println("Switching rep degree...");
            actuator.triggerBlockingSwitchReplicationDegree(2);
            System.out.println("done!");

        } catch (InvocationException e) {
            throw new RuntimeException(e);
        } catch (NoJmxProtocolRegisterException e) {
            throw new RuntimeException(e);
        }

        System.out.println("Finished!");
    }


}
