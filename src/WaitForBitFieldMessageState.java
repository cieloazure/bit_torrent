import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForBitFieldMessageState implements PeerState {
    boolean reply;
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;


    public WaitForBitFieldMessageState(boolean reply, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo) {
        this.reply = reply;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Waiting for bitfield message....with reply:" + this.reply + " from peer id " + context.getTheirPeerId());

            byte[] length = new byte[4];
            inputStream.read(length, 0, 4);
            int len = ByteBuffer.allocate(4).wrap(length).getInt();

            byte[] messageBytes = new byte[len + 4];
            int i = 0;
            for (; i < 4; i++) {
                messageBytes[i] = length[i];
            }
            inputStream.read(messageBytes, 4, len);
            ActualMessage message = new ActualMessage(messageBytes);

            myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Got a bitfield message from " + context.getTheirPeerId());

            // TODO: Check if message is a bitfield

            // TODO: Handle error if the message is not a bitfield
            // TODO: Proposal: Write a error handler class in case of unexpected message types
            // Get their bitfield
            // TODO: check payload and transition to next state

            BitSet theirBitfield = BitSet.valueOf(message.payload);
            this.neighbourConnectionsInfo.get(context.getTheirPeerId()).setBitField(theirBitfield);

            if (this.reply) {
                myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Sending a bitfield reply message to " + context.getTheirPeerId());

                // send our bitfield
                ActualMessage reply = new ActualMessage(MessageType.BITFIELD, myPeerInfo.getBitFieldByteArray(1));

                outputStream.write(reply.serialize());
                outputStream.flush();

                context.setState(new WaitForAnyMessageState(neighbourConnectionsInfo), true, true);
            } else {

                context.setState(new ExpectedToSendInterestedOrNotInterestedMessageState(this.neighbourConnectionsInfo, theirBitfield, true), false, true);
            }
        } catch (EOFException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
