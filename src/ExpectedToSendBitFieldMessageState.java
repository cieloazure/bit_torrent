import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendBitFieldMessageState implements PeerState {

    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;

    public ExpectedToSendBitFieldMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo) {
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            ActualMessage actualMessage = new ActualMessage(MessageType.BITFIELD, myPeerInfo.getBitFieldByteArray(1));
            myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Sent BITFIELD message to " + context.getTheirPeerId());
            outputStream.write(actualMessage.serialize());
            outputStream.flush();
            context.setState(new WaitForBitFieldMessageState(false, neighbourConnectionInfo), true, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
