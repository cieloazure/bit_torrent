import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForHandshakeMessageState implements PeerState{
    private boolean reply;
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;
    private PeerInfo.Builder peerInfoBuilder;

    public WaitForHandshakeMessageState(boolean reply, ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo, PeerInfo.Builder peerInfoBuilder){
        this.reply = reply;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
        this.peerInfoBuilder = peerInfoBuilder;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Waiting for handshake message....with reply:" + this.reply + " from " + context.getHostName() + ":" + context.getPortNumber());
            HandshakeMessage message = (HandshakeMessage) inputStream.readObject();

            peerInfoBuilder.withPeerID(message.getPeerID())
                    .withHostNameAndPortNumber(context.getHostName(), context.getPortNumber());

            PeerInfo theirPeerInfo = peerInfoBuilder.build();
            neighbourConnectionsInfo.putIfAbsent(theirPeerInfo.getPeerID(), theirPeerInfo);

            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Got a handshake message from "+context.getHostName() + ":" + context.getPortNumber() + " and it has peer id "+ message.getPeerID() + "....");

            if (this.reply) {
                System.out.println("[PEER"+myPeerInfo.getPeerID()+"]Sending a reply message to "+message.getPeerID()+"....");
                if (message.isValid()) {
                    HandshakeMessage reply = new HandshakeMessage(myPeerInfo.getPeerID());
                    outputStream.writeObject(reply);
                }
                System.out.println("NOT IMPLEMENTED! Wait for bitfield message with reply true");
                context.setState(null, true);
//                context.setState(1, new WaitForBitFieldMessageState(true, neighbourConnectionsInfo));
            } else {
                System.out.println("NOT IMPLEMENTED! Expected to send bitfield message");
                context.setState(null, false);
//                context.setState(0, new ExpectedToSendBitFieldMessageState(neighbourConnectionsInfo));
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
