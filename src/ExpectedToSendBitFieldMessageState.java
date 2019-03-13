import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class ExpectedToSendBitFieldMessageState implements PeerState{
    @Override
    public void handleMessage(Peer.Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
       try{
           ActualMessage actualMessage = new ActualMessage(MessageType.BITFIELD, peer.getBitField().toByteArray());
           System.out.println("Sent bitfield message...");
           outputStream.writeObject(actualMessage);
           context.setState(1, new WaitForBitFieldMessageState(false));
       } catch (IOException e) {
           e.printStackTrace();
       }
    }
}
