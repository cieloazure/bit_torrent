import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class WaitForUnknownMessage implements PeerState {
    @Override
    public void handleMessage(Peer.NeighbourInputHandler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            Object message = (Object)inputStream.readObject();
            System.out.println("Waiting for unknown message");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
