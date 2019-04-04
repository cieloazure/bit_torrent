import javax.swing.*;
import java.io.IOException;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class WaitForAnyMessageState implements PeerState{
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;


    public WaitForAnyMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo){
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try{
            System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Waiting for any message....from peer id " + context.getTheirPeerId() + " whose current state is " + this.neighbourConnectionsInfo.get(context.getTheirPeerId()).getNeighbourState());

            byte[] length = new byte[4];
            inputStream.read(length, 0, 4);
            int len = ByteBuffer.allocate(4).wrap(length).getInt();

            byte[] messageBytes = new byte[len+4];
            int i = 0;
            for(; i < 4; i++){
                messageBytes[i] = length[i];
            }
            Long start = System.nanoTime();
            inputStream.read(messageBytes, 4, len);
            Long end = System.nanoTime();

            Long timediff = end - start;
            Double downloadSpeed = len / Double.parseDouble(timediff.toString());

            ActualMessage message = new ActualMessage(messageBytes);

            switch(message.getMessageType()){
                case HAVE:
                    handleIncomingHaveMessage(context, message, neighbourConnectionsInfo, myPeerInfo);
                    break;
                case PIECE:
                    handleIncomingPieceMessage(context, message, neighbourConnectionsInfo, downloadSpeed, myPeerInfo);
                    break;
                case REQUEST:
                    handleIncomingRequestMessage(context, message, neighbourConnectionsInfo, myPeerInfo);
                    break;
                case CHOKE:
                    handleIncomingChokeMessage(context, message, neighbourConnectionsInfo, myPeerInfo);
                    break;
                case UNCHOKE:
                    handleIncomingUnchokeMessage(context, message, neighbourConnectionsInfo, myPeerInfo);
                    break;
                case INTERESTED:
                    handleIncomingInterestedMessage(context, message, neighbourConnectionsInfo, myPeerInfo);
                    break;
                case NOT_INTERESTED:
                    handleIncomingNotInterestedMessage(context, message, neighbourConnectionsInfo, myPeerInfo);
                    break;
            }
            context.setState(new WaitForAnyMessageState(neighbourConnectionsInfo), true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingNotInterestedMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Update the state of the peer in concurrent hash map, this will be used when decided the peers to upload to, not interested peers won't be considered at all
        System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Got NOT INTERESTED message from peer "+context.getTheirPeerId()+"! Updating the state in hashmap to be used in next interval");
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.NOT_INTERESTED);
    }

    private void handleIncomingInterestedMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Update the state of the peer in the concurrent hash map, this will be used when unchoking interval elapses in timertask1 or when optimistic unchoking interval elapses in timertask2
        System.out.println("[PEER:"+myPeerInfo.getPeerID()+"]Got INTERESTED message from peer "+context.getTheirPeerId()+"! Updating the state in hashmap to be used in next interval");
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.INTERESTED);
    }

    private void handleIncomingUnchokeMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 0. Update the state of the peer in concurrent hash map to unchoke
        // 1. xor the of the bitfield the neighbour with myPeerInfo.getBitField()
        // 2. From the set bits choose any random index (which has not been requessted before)
        // 3. Send a request message with that index
        // 4. Track to which peer we have sent a request message with that index, next time an unchoke message arrives, do not use the same index again, :
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.UNCHOKED);
//        neighbourConnectionsInfo.get(context.getTheirPeerId()).setRequestedPieceIndex(randomlyChosePieceIndexAmongXoredBitFields);
        System.out.println("RECEIVED UNCHOKE MESSAGE! NOT IMPLEMENTED");
    }

    private void handleIncomingChokeMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 0. Update the state of the peer in concurrent hash map to choked
        // 1. Do nothing!
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED);
        System.out.println("RECEIVED CHOKE MESSAGE! NOT IMPLEMENTED!");
    }

    private void handleIncomingRequestMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Get the piece index from the fileChunks
        // 2. Send a piece with that index through a piece message payload on output thread
        System.out.println("RECEIVED REQUEST MESSAGE! IMPLEMENTED BUT NOT TESTED!");
        byte[] payload = message.getPayload();
        ByteBuffer buffer = ByteBuffer.allocate(payload.length).wrap(payload);
        int pieceIndex = buffer.getInt();
        // Check if the state is unchoked
        if(neighbourConnectionsInfo.get(context.getTheirPeerId()).getNeighbourState() == NeighbourState.UNCHOKED){
            context.setState(new ExpectedToSendPieceMessageState(neighbourConnectionsInfo, pieceIndex), false, false);
        }
    }

    private void handleIncomingPieceMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, Double downloadSpeed, SelfPeerInfo myPeerInfo) {
        // 1. Track the download speed of the message by putting start time and end time around read bytes
        // 2. Update the download speed of the peer in the concurrent hashmap
        // 3. Update out bitfield
        // 4. Send a have message to all the neighbouring peers
        int gotPieceIndex = neighbourConnectionsInfo.get(context.getTheirPeerId()).getRequestedPieceIndex();
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setDownloadSpeed(downloadSpeed);
        myPeerInfo.setBitFieldIndex(gotPieceIndex);
        for(Integer peerId: neighbourConnectionsInfo.keySet()){
            NeighbourPeerInfo peerInfo = neighbourConnectionsInfo.get(peerId);
            peerInfo.setContextState(new ExpectedToSendHaveMessageState(neighbourConnectionsInfo, gotPieceIndex), false, false);
        }
        System.out.println("RECEIVED PIECE MESSAGE! NOT IMPLEMENTED");
    }

    private void handleIncomingHaveMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Update the bitfield `theirPeer` in concurrent hashmap
        // 2. Take xor of the bitfield with myPeerInfo.getBitField() and decide whether an interested or not interested message is to be sent
        byte[] payload = message.getPayload();
        int haveIndex = ByteBuffer.allocate(payload.length).wrap(payload).getInt();
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setBitFieldIndex(haveIndex);
        if(!myPeerInfo.getBitField().get(haveIndex)){
            context.setState(new ExpectedToSendInterestedOrNotInterestedMessageState(neighbourConnectionsInfo, neighbourConnectionsInfo.get(context.getTheirPeerId()).getBitField(), false), false, false);
        }
        System.out.println("RECEIVED HAVE!NOT IMPLEMENTED!");
    }
}
