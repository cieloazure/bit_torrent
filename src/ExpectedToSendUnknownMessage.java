import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ExpectedToSendUnknownMessage implements PeerState {
    @Override
    public void handleMessage(Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
    }
}
