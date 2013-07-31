//package eu.cloudtm;
//
//import eu.cloudtm.InfinispanClient.exception.InvocationException;
//import eu.cloudtm.InfinispanClient.exception.NoJmxProtocolRegisterException;
//import eu.cloudtm.InfinispanClient.jmxprotocol.JmxProtocol;
//import eu.cloudtm.InfinispanClient.jmxprotocol.JmxRMIProtocol;
//import eu.cloudtm.InfinispanClient.jmxprotocol.RemotingJmxProtocol;
//import org.apache.log4j.Logger;
//
//import java.util.HashSet;
//import java.util.Set;
//
///**
// * Created with IntelliJ IDEA.
// * User: fabio
// * Date: 7/24/13
// * Time: 4:01 PM
// * To change this template use File | Settings | File Templates.
// */
//public class Test {
//
//    private static final Logger log = Logger.getLogger(Test.class);
//
//    public static void main(String[] args){
//
//        Set<JmxProtocol> protocols = new HashSet<JmxProtocol>();
//        protocols.add( new JmxRMIProtocol() );
//        protocols.add( new RemotingJmxProtocol() );
//
//        Set<InfinispanMachine> machines = new HashSet<InfinispanMachine>();
//        machines.add( new InfinispanMachine("localhost", 9998) );
//        machines.add( new InfinispanMachine("localhost", 9997) );
//        machines.add( new InfinispanMachine("localhost", 9996) );
//        machines.add( new InfinispanMachine("localhost", 9995) );
//
//        InfinispanClientImpl actuator = new InfinispanClientImpl(machines, "org.infinispan", "x(dist_sync)", 9998);
//
//        try {
//            actuator.triggerBlockingSwitchReplicationProtocol("TO", false, false);
//            actuator.triggerBlockingSwitchReplicationDegree(1);
//
//            for(InfinispanMachine m : machines){
//                //Boolean response = (Boolean) actuator.getAttributeInMachine(m, "DataPlacementManager", "roundInProgress");
//                //System.out.println("resp: " + response);
//            }
//        } catch (InvocationException e) {
//            throw new RuntimeException(e);
//        } catch (NoJmxProtocolRegisterException e) {
//            throw new RuntimeException(e);
//        }
//
//        System.out.println("Finished!");
//    }
//
//
//}
