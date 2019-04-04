import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendInterestedOrNotInterestedMessageState implements PeerState{
    ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionInfo;
    BitSet theirBitfield;

    public ExpectedToSendInterestedOrNotInterestedMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionInfo, BitSet theirBitfield){
        this.neighbourConnectionInfo = neighbourConnectionInfo;
        this.theirBitfield = theirBitfield;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try{
            this.theirBitfield.xor(myPeerInfo.getBitField());
            if(this.theirBitfield.isEmpty()){
                ActualMessage actualMessage = new ActualMessage(MessageType.NOT_INTERESTED);
                System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Sent NOT INTERESTED message to "+context.getTheirPeerId());

                outputStream.write(actualMessage.serialize());
                outputStream.flush();

                context.setState(new WaitForAnyMessageState(neighbourConnectionInfo), true, true);
            }else{
                ActualMessage actualMessage = new ActualMessage(MessageType.INTERESTED);
                System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Sent INTERESTED message to "+context.getTheirPeerId());

                outputStream.write(actualMessage.serialize());
                outputStream.flush();

                context.setState(new WaitForAnyMessageState(neighbourConnectionInfo), true, true);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
