import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendInterestedOrNotInterestedMessageState implements PeerState {
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;

    public ExpectedToSendInterestedOrNotInterestedMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo) {
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Peer.Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("NOT IMPLEMENTED! Expected to send interested or not interested message state");
            inputStream.readObject();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
