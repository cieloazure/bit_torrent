import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendHaveMessageState implements PeerState {
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    private int haveIndex;

    public ExpectedToSendHaveMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo, int haveIndex) {
        this.neighbourConnectionInfo = neighbourConnectionInfo;
        this.haveIndex = haveIndex;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            byte[] haveIndexBytes = ByteBuffer.allocate(4).putInt(this.haveIndex).array();
            ActualMessage message = new ActualMessage(MessageType.HAVE, haveIndexBytes);
            outputStream.write(message.serialize());
            outputStream.flush();
            myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Sent HAVE message for piece index " + this.haveIndex + " to " + context.getTheirPeerId());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
