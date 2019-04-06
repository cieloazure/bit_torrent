import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendInterestedOrNotInterestedMessageState implements PeerState {
    ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo;
    BitSet theirBitfield;
    boolean setState;

    public ExpectedToSendInterestedOrNotInterestedMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionInfo, BitSet theirBitfield, boolean setState) {
        this.neighbourConnectionInfo = neighbourConnectionInfo;
        this.theirBitfield = theirBitfield;
        this.setState = setState;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            this.theirBitfield.xor(myPeerInfo.getBitField());
            if (this.theirBitfield.isEmpty()) {
                ActualMessage actualMessage = new ActualMessage(MessageType.NOT_INTERESTED);
                myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Sent NOT INTERESTED message to " + context.getTheirPeerId());

                outputStream.write(actualMessage.serialize());
                outputStream.flush();

                switch (neighbourConnectionInfo.get(context.getTheirPeerId()).getNeighbourState()) {

                    case UNKNOWN:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.NOT_INTERESTED);
                        break;
                    case CHOKED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                        break;
                    case INTERESTED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.NOT_INTERESTED);
                        break;
                    case UNCHOKED:
                        //stays choked
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                        break;
                    case UNCHOKED_AND_INTERESTED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                        break;
                    case CHOKED_AND_INTERESTED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                        break;
                    case CHOKED_AND_NOT_INTERESTED:
                        break;
                    case NOT_INTERESTED:
                        break;

                }


            } else {
                ActualMessage actualMessage = new ActualMessage(MessageType.INTERESTED);
                myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Sent INTERESTED message to " + context.getTheirPeerId());

                outputStream.write(actualMessage.serialize());
                outputStream.flush();
                switch (neighbourConnectionInfo.get(context.getTheirPeerId()).getNeighbourState()) {

                    case UNKNOWN:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.INTERESTED);
                        break;
                    case CHOKED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_INTERESTED);
                        break;
                    case NOT_INTERESTED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.INTERESTED);
                        break;
                    case UNCHOKED:
                        //stays choked
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.UNCHOKED_AND_INTERESTED);
                        break;
                    case CHOKED_AND_NOT_INTERESTED:
                        neighbourConnectionInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_INTERESTED);
                        break;
                    case UNCHOKED_AND_INTERESTED:
                        break;
                    case CHOKED_AND_INTERESTED:
                        break;
                    case INTERESTED:
                        break;

                }
            }

            if (this.setState) {
                context.setState(new WaitForAnyMessageState(neighbourConnectionInfo), true, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
