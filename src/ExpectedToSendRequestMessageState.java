import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendRequestMessageState implements PeerState {
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;
    private int requestIndex;

    public ExpectedToSendRequestMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, int requestIndex) {
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
        this.requestIndex = requestIndex;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            byte[] requestBytes = ByteBuffer.allocate(4).putInt(requestIndex).array();
            neighbourConnectionsInfo.get(context.getTheirPeerId()).setRequestedPieceIndex(requestIndex);
            ActualMessage message = new ActualMessage(MessageType.REQUEST, requestBytes);
            outputStream.write(message.serialize());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
