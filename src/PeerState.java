import java.io.DataInputStream;
import java.io.DataOutputStream;

public interface PeerState {
    void handleMessage(Handler context, PeerInfo peer, DataInputStream in, DataOutputStream out);
}
