import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendPieceMessageState implements PeerState {

    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    private int pieceIndex;

    public ExpectedToSendPieceMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, int pieceIndex) {
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
        this.pieceIndex = pieceIndex;

    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream in, DataOutputStream outputStream) {
        try {
            System.out.println("Piece index is " + this.pieceIndex);
            ActualMessage actualMessage = new ActualMessage(MessageType.PIECE, myPeerInfo.getFileChunk(this.pieceIndex));
            myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Sent PIECE " + this.pieceIndex + " message to " + context.getTheirPeerId());
            outputStream.write(actualMessage.serialize());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
