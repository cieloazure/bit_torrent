import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendLastBitfieldAckMessage implements PeerState {
    ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    public ExpectedToSendLastBitfieldAckMessage(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo) {
        this.neighbourConnectionInfo = neighbourConnectionInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo peer, DataInputStream in, DataOutputStream out) {
        try {
            ActualMessage message = new ActualMessage(MessageType.LAST_BITFIELD_ACK);
            System.out.println("Sending last BF ACK to "+ context.getTheirPeerId());
            this.neighbourConnectionInfo.get(context.getTheirPeerId()).setSentLastBitfieldAck(true);
            out.write(message.serialize());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
