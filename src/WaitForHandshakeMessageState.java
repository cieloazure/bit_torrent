import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForHandshakeMessageState implements PeerState {
    private boolean reply;
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;

    public WaitForHandshakeMessageState(boolean reply, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo) {
        this.reply = reply;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Waiting for handshake message....with reply:" + this.reply + " from " + context.getHostName() + ":" + context.getPortNumber() + " peer id:" + context.getTheirPeerId());

            byte[] messageBytes = new byte[32];
            inputStream.readFully(messageBytes);
            HandshakeMessage message = new HandshakeMessage(messageBytes);

            if (message.getHeader().equals("P2PFILESHARINGPROJ")) {
                myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Verified Handshake header.");
            } else {
                myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]ERROR: Invalid Handshake header.");
            }

            neighbourConnectionsInfo.get(message.getPeerID()).setContext(context);

            myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Got a handshake message from " + context.getHostName() + ":" + context.getPortNumber() + " and it has peer id " + message.getPeerID() + "....");

            if (this.reply) {
                myPeerInfo.log( "[PEER" + myPeerInfo.getPeerID() + "]Sending a handshake reply message to " + message.getPeerID() + "....");
                if (message.isValid()) {
                    HandshakeMessage reply = new HandshakeMessage(myPeerInfo.getPeerID());
                    outputStream.write(reply.serialize());
                    outputStream.flush();
                }
                context.setState(new WaitForBitFieldMessageState(true, neighbourConnectionsInfo), true, true);
            } else {
                context.setState(new ExpectedToSendBitFieldMessageState(neighbourConnectionsInfo), false, true);
            }

        } catch (EOFException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
