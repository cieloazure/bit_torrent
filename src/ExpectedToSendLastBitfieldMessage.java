import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendLastBitfieldMessage implements PeerState {
    ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    SelfPeerInfo myPeerInfo;

    public ExpectedToSendLastBitfieldMessage(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo, SelfPeerInfo myPeerInfo) {
        this.neighbourConnectionInfo = neighbourConnectionInfo;
        this.myPeerInfo = myPeerInfo;
    }
    @Override
    public void handleMessage(Handler context, SelfPeerInfo peer, DataInputStream in, DataOutputStream out) {
        try {
            ActualMessage message = new ActualMessage(MessageType.BITFIELD, peer.getBitFieldByteArray(1));
            out.write(message.serialize());
            out.flush();
        }catch (SocketException e){
            if(!myPeerInfo.isHasTriggeredShutDown()){
//                System.out.println("Triggering shutdown from socket exception in ExpectedToSendLastBitfieldMessage");
                myPeerInfo.getLastBitfieldMessageSchExec().shutdownNow();
                PeriodicTasks pt = new PeriodicTasks(myPeerInfo, this.neighbourConnectionInfo);
                myPeerInfo.setHasTriggeredShutDown(true);
                pt.triggerImmediateShutdown(myPeerInfo);
            }

        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
