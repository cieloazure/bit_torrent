import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class WaitForHandshakeMessageState implements PeerState{
    private boolean reply;

    public WaitForHandshakeMessageState(boolean reply){
        this.reply = reply;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("Waiting for handshake message....");
            HandshakeMessage message = (HandshakeMessage)inputStream.readObject();
            System.out.println("Got a handshake message....");

            if(this.reply){
                System.out.println("Sending a reply message....");
                if(message.isValid()){
                    HandshakeMessage reply = new HandshakeMessage(peer.peerID);
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
