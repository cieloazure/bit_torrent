import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface PeerState {
    void handleMessage(Handler context, SelfPeerInfo peer, DataInputStream in, DataOutputStream out);
}
