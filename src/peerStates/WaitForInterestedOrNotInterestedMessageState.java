import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForInterestedOrNotInterestedMessageState implements PeerState {
    private boolean reply;
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;

    public WaitForInterestedOrNotInterestedMessageState(boolean reply, ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo) {
        this.reply = reply;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Peer.Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("NOT IMPLEMENTED! Wait for interested or not interested message state");
            inputStream.readObject();
        }catch(EOFException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
