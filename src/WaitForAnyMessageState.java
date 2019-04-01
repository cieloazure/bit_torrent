import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForAnyMessageState implements PeerState{
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;


    public WaitForAnyMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo){
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try{
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Waiting for any message....from peer id " + context.getTheirPeerId());
            byte[] messageBytes = inputStream.readAllBytes();
            ActualMessage message = new ActualMessage(messageBytes);

            switch(message.getMessageType()){
                case HAVE:
                case CHOKE:
                case PIECE:
                case REQUEST:
                case UNCHOKE:
                case INTERESTED:
                case NOT_INTERESTED:
                    System.out.println("NOT IMPLEMENTED " + message.getMessageType() + "!");
                    break;
            }
            context.setState(new WaitForAnyMessageState(neighbourConnectionsInfo), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
