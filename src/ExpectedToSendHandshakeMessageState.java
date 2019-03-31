import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendHandshakeMessageState implements PeerState{
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionInfo;

    public ExpectedToSendHandshakeMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo){
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Peer.Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try{
            HandshakeMessage message = new HandshakeMessage(myPeerInfo.getPeerID());
            System.out.println("Sent handshake message");
            outputStream.writeObject(message);
            context.setState(1, new WaitForHandshakeMessageState(false, neighbourConnectionInfo));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
