import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
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

            byte[] length = new byte[4];
            inputStream.read(length, 0, 4);
            int len = ByteBuffer.allocate(4).wrap(length).getInt();

            byte[] messageBytes = new byte[len+4];
            int i = 0;
            for(; i < 4; i++){
                messageBytes[i] = length[i];
            }
            inputStream.read(messageBytes, 4, len);
            ActualMessage message = new ActualMessage(messageBytes);

            switch(message.getMessageType()){
                case HAVE:
                case PIECE:
                case REQUEST:
                case CHOKE:
                case UNCHOKE:
                    System.out.println("NOT IMPLEMENTED " + message.getMessageType() + "!");
                    break;
                case INTERESTED:
                    System.out.println("Received INTERESTED message! NOT IMPLEMENTED action");
                    break;
                case NOT_INTERESTED:
                    System.out.println("Received NOT INTERESTED message! NOT IMPLEMENTED action");
                    break;
            }
            context.setState(new WaitForAnyMessageState(neighbourConnectionsInfo), true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
