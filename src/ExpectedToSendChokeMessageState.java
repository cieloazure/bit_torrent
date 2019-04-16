import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;


public class ExpectedToSendChokeMessageState implements PeerState {
    ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    SelfPeerInfo myPeerInfo;
    BitSet theirBitfield;
    boolean setState;

    public ExpectedToSendChokeMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo, SelfPeerInfo myPeerInfo) {
        this.neighbourConnectionInfo = neighbourConnectionInfo;
        this.myPeerInfo = myPeerInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            ActualMessage actualMessage = new ActualMessage(MessageType.CHOKE);
//            myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Sent CHOKE message to " + context.getTheirPeerId());

            outputStream.write(actualMessage.serialize());
            outputStream.flush();

            switch (neighbourConnectionInfo.get(context.getTheirPeerId()).getNeighbourState()) {
                case UNKNOWN:
                    neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                    break;
                case UNCHOKED_AND_INTERESTED:
                    neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_INTERESTED);
                    break;
                case CHOKED_AND_INTERESTED:
                    break;
                case CHOKED_AND_NOT_INTERESTED:
                    break;

            }

        }catch (SocketException e){
            if(!myPeerInfo.isHasTriggeredShutDown()){
                System.out.println("Triggering shutdown from socket exception in ExpectedToSendChokeMessageState");
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