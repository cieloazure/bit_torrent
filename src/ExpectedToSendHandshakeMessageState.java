import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendHandshakeMessageState implements PeerState{
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionInfo;
    private PeerInfo.Builder peerInfoBuilder;

    public ExpectedToSendHandshakeMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo, PeerInfo.Builder peerInfoBuilder){
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
        this.peerInfoBuilder = peerInfoBuilder;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try{
            HandshakeMessage message = new HandshakeMessage(myPeerInfo.getPeerID());
            System.out.println("Sent handshake message....");
            outputStream.writeObject(message);
            context.setState(new WaitForHandshakeMessageState(false, neighbourConnectionInfo, peerInfoBuilder), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
