import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class ExpectedToSendBitFieldMessageState implements PeerState{

    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionInfo;

    public ExpectedToSendBitFieldMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo){
        this.neighbourConnectionInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
       try{
           ActualMessage actualMessage = new ActualMessage(MessageType.BITFIELD, myPeerInfo.getBitFieldByteArray(1));
           System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Sent bitfield message to "+context.getTheirPeerId());
           outputStream.writeObject(actualMessage);
           context.setState(new WaitForBitFieldMessageState(false, neighbourConnectionInfo), true);
       } catch (IOException e) {
           e.printStackTrace();
       }
    }
}
