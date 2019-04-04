import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForAnyMessageState implements PeerState{
    private ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo;


    public WaitForAnyMessageState(ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsInfo){
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, PeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try{
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Waiting for any message....from peer id " + context.getTheirPeerId());

            byte[] length = new byte[4];
            inputStream.read(length, 0, 4);
            int len = ByteBuffer.allocate(4).wrap(length).getInt();

            byte[] messageBytes = new byte[len+4];
            int i = 0;
            for(; i < 4; i++){
                messageBytes[i] = length[i];
            }
            inputStream.read(messageBytes, 4, len);
            ActualMessage message = new ActualMessage(messageBytes);

            switch(message.getMessageType()){
                case HAVE:
                    handleIncomingHaveMessage();
                case PIECE:
                    handleIncomingPieceMessage();
                case REQUEST:
                    handleIncomingRequestMessage();
                case CHOKE:
                    handleIncomingChokeMessage();
                case UNCHOKE:
                    handleIncomingUnchokeMessage();
                    break;
                case INTERESTED:
                    handleIncomingInterestedMessage();
                    break;
                case NOT_INTERESTED:
                    handleIncomingNotInterestedMessage();
                    break;
            }
            context.setState(new WaitForAnyMessageState(neighbourConnectionsInfo), true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingNotInterestedMessage() {
        // 1. Update the state of the peer in concurrent hash map, this will be used when decided the peers to upload to, not interested peers won't be considered at all
        System.out.println("RECEIVED NOT INTERESTED MESSAGE! NOT IMPLEMENTED!");
    }

    private void handleIncomingInterestedMessage() {
        // 1. Update the state of the peer in the concurrent hash map, this will be used when unchoking interval elapses in timertask1 or when optimistic unchoking interval elapses in timertask2
        System.out.println("RECEIVED INTERESTED MESSAGE! NOT IMPLEMENTED!");
    }

    private void handleIncomingUnchokeMessage() {
        // 0. Update the state of the peer in concurrent hash map to unchoke
        // 1. xor the bitfield the neighbour with myPeerInfo.getBitField()
        // 2. From the set bits choose any random index (which has not been requessted before)
        // 3. Send a request message with that index
        // 4. Track to which peer we have sent a request message with that index, next time an unchoke message arrives, do not use the same index again, :
        System.out.println("RECEIVED UNCHOKE MESSAGE! NOT IMPLEMENTED");
    }

    private void handleIncomingChokeMessage() {
        // 0. Update the state of the peer in concurrent hash map to choked
        // 1. Do nothing!
        System.out.println("RECEIVED CHOKE MESSAGE! NOT IMPLEMENTED!");
    }

    private void handleIncomingRequestMessage() {
        // 1. Get the piece index from the fileChunks
        // 2. Send a piece with that index through a piece message payload

        // context.setState(new ExpectedToSendPieceMessageState(pieceIndex), false, false);
        System.out.println("RECEIVED REQUEST MESSAGE! NOT IMPLEMENTED!");
    }

    private void handleIncomingPieceMessage() {
        // 1. Track the download speed of the message by putting start time and end time around read bytes
        // 2. Update the download speed of the peer in the concurrent hashmap
        // 3. Send a have message to all the neighbouring peers
        System.out.println("RECEIVED PIECE MESSAGE! NOT IMPLEMENTED");
    }

    private void handleIncomingHaveMessage() {
        // 1. Update the bitfield `theirPeer` in concurrent hashmap
        // 2. Take xor of the bitfield with myPeerInfo.getBitField() and decide whether an interested or not interested message is to be sent
        System.out.println("RECEIVED HAVE!NOT IMPLEMENTED!");
    }
}
