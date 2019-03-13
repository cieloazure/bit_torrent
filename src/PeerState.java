import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface PeerState {
    void handleMessage(Peer.Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream);
}
