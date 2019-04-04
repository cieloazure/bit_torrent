import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendHandshakeMessageState implements PeerState{
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    private PeerInfo.Builder peerInfoBuilder;

    public ExpectedToSendHandshakeMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, PeerInfo.Builder peerInfoBuilder){
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
        this.peerInfoBuilder = peerInfoBuilder;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try{
            HandshakeMessage message = new HandshakeMessage(myPeerInfo.getPeerID());

            outputStream.write(message.serialize());
            outputStream.flush();

            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"] Sent handshake message to "+ context.getHostName() + ":" + context.getPortNumber() +"...");
            context.setState(new WaitForHandshakeMessageState(false, this.neighbourConnectionInfo, this.peerInfoBuilder), true, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
