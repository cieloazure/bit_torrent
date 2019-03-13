import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForHandshakeMessageState implements PeerState{
    private boolean reply;
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;

    public WaitForHandshakeMessageState(boolean reply, ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo){
        this.reply = reply;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Peer.Handler context, PeerInfo peerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("Waiting for handshake message....");
            HandshakeMessage message = (HandshakeMessage)inputStream.readObject();
            System.out.println("Got a handshake message....");

            if(this.reply){
                System.out.println("Sending a reply message....");
                if(message.isValid()){
                    HandshakeMessage reply = new HandshakeMessage(peerInfo.getPeerID());
                    outputStream.writeObject(reply);
                }
                context.setState(1, new WaitForBitFieldMessageState(true));
            }else{
                context.setState(0, new ExpectedToSendBitFieldMessageState());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
