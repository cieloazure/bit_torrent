import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExpectedToSendLastBitfieldAckMessage implements PeerState {
    @Override
    public void handleMessage(Handler context, SelfPeerInfo peer, DataInputStream in, DataOutputStream out) {
        try {
            ActualMessage message = new ActualMessage(MessageType.LAST_BITFIELD_ACK);
            System.out.println("Sending last BF ACK to "+ context.getTheirPeerId());
            out.write(message.serialize());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
