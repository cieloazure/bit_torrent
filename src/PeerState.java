import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public interface PeerState {
    void handleMessage(Handler context, PeerInfo peer, ObjectInputStream in, ObjectOutputStream out);
}
