import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.BitSet;

public class WaitForBitFieldMessageState implements PeerState {
    @Override
    public void handleMessage(Peer.NeighbourInputHandler context, PeerInfo peer, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        try {
            ActualMessage message = (ActualMessage)inputStream.readObject();
            if(message.getMessageType() == MessageType.BITFIELD){
                // Get their bitfield
                BitSet theirPayload = BitSet.valueOf(message.payload);
                System.out.println("Their payload:"+theirPayload);

                // send our bitfield
                ActualMessage reply = new ActualMessage(MessageType.BITFIELD, peer.bitField.toByteArray());
                outputStream.writeObject(reply);
                context.setState(new WaitForUnknownMessage());
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
