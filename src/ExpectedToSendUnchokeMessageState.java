import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;


public class ExpectedToSendUnchokeMessageState implements PeerState {
    ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    BitSet theirBitfield;
    boolean setState;

    public ExpectedToSendUnchokeMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo) {
        this.neighbourConnectionInfo = neighbourConnectionInfo;

    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            if (!neighbourConnectionInfo.get(context.getTheirPeerId()).isUnChoked()) {
                ActualMessage actualMessage = new ActualMessage(MessageType.UNCHOKE);
                outputStream.write(actualMessage.serialize());
                outputStream.flush();
                switch (neighbourConnectionInfo.get(context.getTheirPeerId()).getNeighbourState()) {
                    case UNKNOWN:
                        //stays choked
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.UNCHOKED_AND_INTERESTED);
                        break;
                    case CHOKED_AND_INTERESTED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.UNCHOKED_AND_INTERESTED);
                        break;
                    case CHOKED_AND_NOT_INTERESTED:
                        break;
                    case UNCHOKED_AND_INTERESTED:
                        break;

                }
                myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Sent UNCHOKE message to " + context.getTheirPeerId());

            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}