import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

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
            outputStream.writeObject(message);
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"] Sent handshake message to "+ context.getHostName() + ":" + context.getPortNumber() +"...");
            context.setState(new WaitForHandshakeMessageState(false, this.neighbourConnectionInfo, this.peerInfoBuilder), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
