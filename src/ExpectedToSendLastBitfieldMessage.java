import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExpectedToSendLastBitfieldMessage implements PeerState {
    @Override
    public void handleMessage(Handler context, SelfPeerInfo peer, DataInputStream in, DataOutputStream out) {
        try {
            ActualMessage message = new ActualMessage(MessageType.BITFIELD, peer.getBitFieldByteArray(1));
            out.write(message.serialize());
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
