import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendHandshakeMessageState implements PeerState {
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;

    public ExpectedToSendHandshakeMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo) {
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            HandshakeMessage message = new HandshakeMessage(myPeerInfo.getPeerID());

            outputStream.write(message.serialize());
            outputStream.flush();

            myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "] Sent HANDSHAKE message to " + context.getHostName() + ":" + context.getPortNumber() + "...");
            context.setState(new WaitForHandshakeMessageState(false, this.neighbourConnectionInfo), true, true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
