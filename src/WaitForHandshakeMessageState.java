import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class WaitForHandshakeMessageState implements PeerState{

    @Override
    public void handleMessage(Peer.NeighbourInputHandler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("Waiting for a handshake message....");
            HandshakeMessage message = (HandshakeMessage)inputStream.readObject();
            if(message.isValid()){
                HandshakeMessage reply = new HandshakeMessage(peer.peerID);
                outputStream.writeObject(reply);
            }
            context.setState(new WaitForBitFieldMessageState());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
