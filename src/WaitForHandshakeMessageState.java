import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class WaitForHandshakeMessageState implements PeerState{
    private boolean reply;
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;
    private PeerInfo.Builder peerInfoBuilder;

    public WaitForHandshakeMessageState(boolean reply, ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo, PeerInfo.Builder peerInfoBuilder){
        this.reply = reply;
        this.peerInfoBuilder = peerInfoBuilder;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
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

            context.setTheirPeerId(message.getPeerID());

            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Got a handshake message from "+context.getHostName() + ":" + context.getPortNumber() + " and it has peer id "+ message.getPeerID() + "....");

            if (this.reply) {
                System.out.println("[PEER"+myPeerInfo.getPeerID()+"]Sending a handshake reply message to "+message.getPeerID()+"....");
                if (message.isValid()) {
                    HandshakeMessage reply = new HandshakeMessage(myPeerInfo.getPeerID());
                    outputStream.writeObject(reply);
                }
                context.setState(new WaitForBitFieldMessageState(true, neighbourConnectionsInfo), true);
            } else {
                context.setState(new ExpectedToSendBitFieldMessageState(neighbourConnectionsInfo), false);
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
