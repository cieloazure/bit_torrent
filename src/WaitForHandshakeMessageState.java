import java.io.EOFException;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ConcurrentHashMap;

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
    public void handleMessage(Handler context, PeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Waiting for handshake message....with reply:" + this.reply + " from " + context.getHostName() + ":" + context.getPortNumber());

            byte[] messageBytes = new byte[32];
            inputStream.readFully(messageBytes);
            HandshakeMessage message = new HandshakeMessage(messageBytes);

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
                    outputStream.write(reply.serialize());
                    outputStream.flush();
                }
                context.setState(new WaitForBitFieldMessageState(true, neighbourConnectionsInfo), true, true);
            } else {
                context.setState(new ExpectedToSendBitFieldMessageState(neighbourConnectionsInfo), false, true);
            }

        }catch (EOFException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
