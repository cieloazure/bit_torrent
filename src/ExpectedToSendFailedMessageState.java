import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;

public class ExpectedToSendFailedMessageState implements PeerState {


    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream in, DataOutputStream outputStream) {
        try {
            ActualMessage actualMessage = new ActualMessage(MessageType.FAILED);
            System.out.println( "[PEER:" + myPeerInfo.getPeerID() + "]Sent FAILED message to " + context.getTheirPeerId());
            outputStream.write(actualMessage.serialize());
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
