import java.io.EOFException;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForHandshakeMessageState implements PeerState{
    private boolean reply;
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;
    private PeerInfo.Builder peerInfoBuilder;

    public WaitForHandshakeMessageState(boolean reply, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, PeerInfo.Builder peerInfoBuilder){
        this.reply = reply;
        this.peerInfoBuilder = peerInfoBuilder;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Waiting for handshake message....with reply:" + this.reply + " from " + context.getHostName() + ":" + context.getPortNumber() + " peer id:"+context.getTheirPeerId());

            byte[] messageBytes = new byte[32];
            inputStream.readFully(messageBytes);
            HandshakeMessage message = new HandshakeMessage(messageBytes);

            if(message.getHeader().equals("P2PFILESHARINGPROJ")) {
                System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Verified Handshake header.");
            }
            else {
                System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]ERROR: Invalid Handshake header.");
            }

            peerInfoBuilder
                    .withPeerID(message.getPeerID())
                    .withHostName(context.getHostName())
                    .withPortNumber(context.getPortNumber())
                    .withHandlerContext(context);

            NeighbourPeerInfo theirPeerInfo = peerInfoBuilder.buildNeighbourPeerInfo();
            neighbourConnectionsInfo.putIfAbsent(theirPeerInfo.getPeerID(), theirPeerInfo);

            System.out.println("**Sender PID:" + message.getPeerID());
            System.out.println("Receiver PID:" + myPeerInfo.getPeerID());

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
