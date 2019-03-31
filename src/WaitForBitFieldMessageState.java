import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class WaitForBitFieldMessageState implements PeerState {
    boolean reply;
    private ConcurrentSkipListSet<PeerInfo> neighbourConnectionsInfo;


    public WaitForBitFieldMessageState(boolean reply, ConcurrentSkipListSet<PeerInfo> neighbourConnectionsInfo){
        this.reply = reply;
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Waiting for bitfield message....with reply:" + this.reply + " from peer id " + context.getTheirPeerId());
            ActualMessage message = (ActualMessage)inputStream.readObject();
            System.out.println("[PEER:" + myPeerInfo.getPeerID() +"]Got a bitfield message from "+context.getTheirPeerId());

            // TODO: Check if message is a bitfield
            // TODO: Handle error if the message is not a bitfield
            // TODO: Proposal: Write a error handler class in case of unexpected message types

            // Get their bitfield
            // TODO: check payload and transition to next state
            //BitSet theirPayload = BitSet.valueOf(message.payload);

            if(this.reply){
                System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Sending a bitfield reply message to " + context.getTheirPeerId());

                // send our bitfield
                ActualMessage reply = new ActualMessage(MessageType.BITFIELD, myPeerInfo.getBitField().toByteArray());
                outputStream.writeObject(reply);

                System.out.println("NOT IMPLEMENTED! Wait for interested or not interested message!");
                context.setState(null, true);
            }else{
                System.out.println("NOT IMPLEMENTED! Expect to send interested or not interested message! Should give an exception because we're trying to put null in the output queue");
                context.setState(null, false);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }catch(EOFException e){
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
