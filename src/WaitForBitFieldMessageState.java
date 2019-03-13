import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForBitFieldMessageState implements PeerState {
    boolean reply;
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;


    public WaitForBitFieldMessageState(boolean reply, ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo){
        this.reply = reply;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Peer.Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("Waiting for bitfield message....with reply:" + this.reply);
            ActualMessage message = (ActualMessage)inputStream.readObject();
            System.out.println("Got a bitfield message.....");

            // TODO: Check if message is a bitfield
            // TODO: Check why message check after deserialization is not working
            // if(message.getMessageType() == MessageType.BITFIELD)
            // TODO: Handle error if the message is not a bitfield
            // TODO: Proposal: Write a error handler class in case of unexpected message types

            // Get their bitfield
            // TODO: check payload and transition to next state
            //BitSet theirPayload = BitSet.valueOf(message.payload);

            if(this.reply){
                System.out.println("Sending a reply...");
                // send our bitfield
                ActualMessage reply = new ActualMessage(MessageType.BITFIELD, myPeerInfo.getBitField().toByteArray());
                outputStream.writeObject(reply);
                context.setState(1, new WaitForInterestedOrNotInterestedMessageState(true, neighbourConnectionsInfo));
            }else{
//                    context.setState(0, new ExpectedToSendInterestedMessage());
                context.setState(0,new ExpectedToSendInterestedOrNotInterestedMessageState(neighbourConnectionsInfo));
            }

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
