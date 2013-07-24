package eu.cloudtm;

import eu.cloudtm.exception.InvocationException;
import eu.cloudtm.exception.NoJmxProtocolRegisterException;
import eu.cloudtm.jmxprotocol.JmxProtocol;
import eu.cloudtm.jmxprotocol.JmxRMIProtocol;
import eu.cloudtm.jmxprotocol.RemotingJmxProtocol;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

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
        machines.add( new InfinispanMachine("localhost", 9998) );

        InfinispanActuator actuator = new InfinispanActuator(protocols, machines, "org.infinispan", "x(dist_sync)", 9998);
        try {
            actuator.triggerBlockingSwitchReplicationProtocol("TO", false, false);
        } catch (InvocationException e) {
            throw new RuntimeException(e);
        } catch (NoJmxProtocolRegisterException e) {
            throw new RuntimeException(e);
        }

        log.info("Switched!");

    }
}
