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
    public void handleMessage(Peer.Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
       try{
           System.out.println("Bitfield message length:" + myPeerInfo.getBitFieldByteArray(1).length);
           ActualMessage actualMessage = new ActualMessage(MessageType.BITFIELD, myPeerInfo.getBitFieldByteArray(1));
           System.out.println("Sent bitfield message...");
           outputStream.writeObject(actualMessage);
           context.setState(1, new WaitForBitFieldMessageState(false, neighbourConnectionInfo));
       } catch (IOException e) {
           e.printStackTrace();
       }
    }
}
