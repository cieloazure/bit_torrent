import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;

public class WaitForBitFieldMessageState implements PeerState {
    boolean reply;

    public WaitForBitFieldMessageState(boolean reply){
        this.reply = reply;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            ActualMessage message = (ActualMessage)inputStream.readObject();
            if(message.getMessageType() == MessageType.BITFIELD){

                // Get their bitfield
                // TODO: check payload and perform operations
                BitSet theirPayload = BitSet.valueOf(message.payload);

                if(this.reply){
                    // send our bitfield
                    ActualMessage reply = new ActualMessage(MessageType.BITFIELD, peer.bitField.toByteArray());
                    outputStream.writeObject(reply);
                    context.setState(1, new WaitForBitFieldMessageState(false));
                }else{
//                    context.setState(0, new ExpectedToSendInterestedMessage());
                    context.setState(0, null);
                }

            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
