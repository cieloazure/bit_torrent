import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class WaitForHandshakeMessageState implements PeerState{
    private boolean reply;

    public WaitForHandshakeMessageState(boolean reply){
        this.reply = reply;
    }

    @Override
    public void handleMessage(Peer.Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            HandshakeMessage message = (HandshakeMessage)inputStream.readObject();
            System.out.println("Got a handshake message....");

            if(this.reply){
                if(message.isValid()){
                    HandshakeMessage reply = new HandshakeMessage(peer.peerID);
                    outputStream.writeObject(reply);
                }
            }
            context.setState(new WaitForBitFieldMessageState(true));
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
