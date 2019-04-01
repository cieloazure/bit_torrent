import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendBitFieldMessageState implements PeerState{

    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionInfo;

    public ExpectedToSendBitFieldMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo){
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
       try{
           ActualMessage actualMessage = new ActualMessage(MessageType.BITFIELD, myPeerInfo.getBitFieldByteArray(1));
           System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Sent bitfield message to "+context.getTheirPeerId());
           outputStream.write(actualMessage.serialize());
           outputStream.flush();
           context.setState(new WaitForBitFieldMessageState(false, neighbourConnectionInfo), true);
       } catch (IOException e) {
           e.printStackTrace();
       }
    }
}
