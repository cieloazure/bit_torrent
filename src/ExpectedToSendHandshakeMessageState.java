import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ExpectedToSendHandshakeMessageState implements PeerState{
    @Override
    public void handleMessage(Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try{
            HandshakeMessage message = new HandshakeMessage(peer.peerID);
            System.out.println("Sent handshake message....");
            outputStream.writeObject(message);
            context.setState(1, new WaitForHandshakeMessageState(false));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
