import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public interface PeerState {
//    public void handleMessage(PeerV1 p, byte[] incomingMessage, int fromPeerId, ObjectOutputStream writeTo);
    void handleMessage(Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream);
}
