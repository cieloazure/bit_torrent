import java.io.EOFException;
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
    public void handleMessage(Peer.Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("Waiting for handshake message....with reply:" + this.reply);
            HandshakeMessage message = (HandshakeMessage) inputStream.readObject();

            PeerInfo.Builder builder = new PeerInfo.Builder()
                    .withPeerID(message.getPeerID())
                    .withHostNameAndPortNumber(context.getHostName(), context.getPortNumber())
                    .withPeerIndex(context.getTheirPeerIndex());

            PeerInfo theirPeerInfo = builder.build();
            neighbourConnectionsInfo.putIfAbsent(theirPeerInfo.getPeerID(), theirPeerInfo);
            myPeerInfo.getLogger().info("Waiting to send handshake message to some peer by "+myPeerInfo.getPeerID());
            System.out.println("Got a handshake message....And set concurrent hashmap");

            if (this.reply) {
                System.out.println("Sending a reply message....");
                if (message.isValid()) {
                    HandshakeMessage reply = new HandshakeMessage(myPeerInfo.getPeerID());
                    outputStream.writeObject(reply);
                }
                System.out.println("Setting state, going to sleep to sync");
                context.setState(1, new WaitForBitFieldMessageState(true, neighbourConnectionsInfo));
            } else {
                context.setState(0, new ExpectedToSendBitFieldMessageState(neighbourConnectionsInfo));
            }

        }catch (EOFException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }
}
