import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.BitSet;
import java.util.concurrent.ThreadLocalRandom;

public class WaitForAnyMessageState implements PeerState {
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;


    public WaitForAnyMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo) {
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
//            myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Waiting for any message....from peer id " + context.getTheirPeerId() + " whose current state is " + this.neighbourConnectionsInfo.get(context.getTheirPeerId()).getNeighbourState());

            int len = 0;
            byte[] messageBytes;
            Double downloadSpeed = 0.0;
            byte[] length = new byte[4];
            inputStream.read(length, 0, 4);
            len = ByteBuffer.allocate(4).wrap(length).getInt();

            messageBytes = new byte[len + 4];
            int i = 0;
            for (; i < 4; i++) {
                messageBytes[i] = length[i];
            }
            Long start = System.nanoTime();
            inputStream.read(messageBytes, 4, len);
            Long end = System.nanoTime();

            Long timediff = end - start;
            downloadSpeed = len / Double.parseDouble(timediff.toString());
            System.out.println("Message length is "+len);

            ActualMessage message = new ActualMessage(messageBytes);
//            System.out.println("[Wait for any message]:Received message:"+message.isValid());

            if(true){
                switch (message.getMessageType()) {
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
            }
            context.setState(new WaitForAnyMessageState(neighbourConnectionsInfo), true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingNotInterestedMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Update the state of the peer in concurrent hash map, this will be used when decided the peers to upload to, not interested peers won't be considered at all
        myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Got NOT INTERESTED message from peer " + context.getTheirPeerId() + "! Updating the state in hashmap to be used in next interval");
        switch (neighbourConnectionsInfo.get(context.getTheirPeerId()).getNeighbourState()) {

            case UNKNOWN:
                neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                break;
            case UNCHOKED_AND_INTERESTED:
                neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                break;
            case CHOKED_AND_INTERESTED:
                neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_NOT_INTERESTED);
                break;
            case CHOKED_AND_NOT_INTERESTED:
                break;
        }
    }

    private void handleIncomingInterestedMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Update the state of the peer in the concurrent hash map, this will be used when unchoking interval elapses in timertask1 or when optimistic unchoking interval elapses in timertask2

        myPeerInfo.log( "[PEER:" + myPeerInfo.getPeerID() + "]Got INTERESTED message from peer " + context.getTheirPeerId() + "! Updating the state in hashmap to be used in next interval");
        switch (neighbourConnectionsInfo.get(context.getTheirPeerId()).getNeighbourState()) {

            case UNKNOWN:
                neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_INTERESTED);
                break;
            case CHOKED_AND_NOT_INTERESTED:
                neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.CHOKED_AND_INTERESTED);
                break;
            case UNCHOKED_AND_INTERESTED:
                break;
            case CHOKED_AND_INTERESTED:
                break;

        }

    }

    private void handleIncomingUnchokeMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got UNCHOKE message from peer " + context.getTheirPeerId() + "! Updating the state in hashmap to be used in next interval");
        sendRequestMessage(context, neighbourConnectionsInfo, myPeerInfo);

        //5. Track to which peer we have sent a request message with that index, next time an unchoke message arrives, do not use the same index again, :

    }

    private void sendRequestMessage(Handler context, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 0. Update the state of the peer in concurrent hash map to unchoke **--->> done in ExpectedToSendChokeMessageState/Unchoke
        //neighbourConnectionsInfo.get(context.getTheirPeerId()).setNeighbourState(NeighbourState.UNCHOKED);

        // 1. xor & and the of the bitfield the neighbour with myPeerInfo.getBitField() to find pieces that can be requested from neighbour.
        BitSet missing = new BitSet();
        BitSet toRequest = new BitSet();
        BitSet theirBitSet = new BitSet();

        theirBitSet = (BitSet)(neighbourConnectionsInfo.get(context.getTheirPeerId())).getBitField().clone();
        missing = (BitSet)(myPeerInfo.getBitField()).clone();

        System.out.println("Server bitset:" +theirBitSet);
        System.out.println("client bitset:"+missing);

        missing.xor(theirBitSet);

        //pieces that are missing from client and present in server
        missing.and(theirBitSet);

        toRequest = (BitSet)missing.clone();
        //pieces that are not yet requested.
        toRequest.andNot(myPeerInfo.getRequestedPieces());

        System.out.printf("Bitset to be requested"+toRequest);

        // 2. From the set bits choose any random index (which has not been requesssted before)
        if(toRequest.cardinality() > 0){

            int randomIndex = ThreadLocalRandom.current().nextInt(0,toRequest.cardinality());
            int pieceToRequest = toRequest.nextSetBit(randomIndex);

            // 3. Send a request message with that index
            context.setState(new ExpectedToSendRequestMessageState(this.neighbourConnectionsInfo,pieceToRequest), false, false);

            //4.Set the piece index in requestedPieces bitset
            myPeerInfo.setRequestPiecesIndex(pieceToRequest);
        }else{
            System.out.println("RECEIVED ENTIRE FILE!");
        }
    }

    private void handleIncomingChokeMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 0. Update the state of the peer in concurrent hash map to choked **--->> done in ExpectedToSendChokeMessageState/Unchoke
        // 1. Do nothing!
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got CHOKE message from peer " + context.getTheirPeerId() + "! Updating the state in hashmap to be used in next interval");

    }

    private void handleIncomingRequestMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Get the piece index from the fileChunks
        // 2. Send a piece with that index through a piece message payload on output thread
        myPeerInfo.log( "RECEIVED REQUEST MESSAGE from "+ context.getTheirPeerId());
        byte[] payload = message.getPayload();

        // 1. Get the piece index from the fileChunks
        ByteBuffer buffer = ByteBuffer.allocate(payload.length).wrap(payload);
        int pieceIndex = buffer.getInt();

        // 2. Send a piece with that index through a piece message payload on output thread
        // 2.1. Check if the state is unchoked
        if (neighbourConnectionsInfo.get(context.getTheirPeerId()).isUnChoked()) {
            context.setState(new ExpectedToSendPieceMessageState(neighbourConnectionsInfo, pieceIndex), false, false);
        }
    }

    private void handleIncomingPieceMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, Double downloadSpeed, SelfPeerInfo myPeerInfo) {
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got PIECE message from peer " + context.getTheirPeerId() + "! Will send have messages to all neighbours and next request message to the peer");
//        System.out.println("RECEIVED PIECE MESSAGE from "+ context.getTheirPeerId());
        // 1. Track the download speed of the message by putting start time and end time around read bytes
        // Handled in handler message

        // 1.1. Get the piece index that was requested from this peer
        int gotPieceIndex = neighbourConnectionsInfo.get(context.getTheirPeerId()).getRequestedPieceIndex();

        // 2. Update the download speed of the peer in the concurrent hashmap
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setDownloadSpeed(downloadSpeed);

        // 3. Update our bitfield
        myPeerInfo.setBitFieldIndex(gotPieceIndex);

        // 3.1. Update file chunk index
        myPeerInfo.setFileChunkIndex(gotPieceIndex, message.getPayload());

        // 4. Send a have message to all the neighbouring peers
        for (Integer peerId : neighbourConnectionsInfo.keySet()) {
            NeighbourPeerInfo peerInfo = neighbourConnectionsInfo.get(peerId);
            if(peerInfo.getContext() != null){
                peerInfo.setContextState(new ExpectedToSendHaveMessageState(neighbourConnectionsInfo, gotPieceIndex), false, false);
            }
        }

        // 5. Send next request message to the same peer
        sendRequestMessage(context, neighbourConnectionsInfo, myPeerInfo);
    }

    private void handleIncomingHaveMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Update the bitfield `theirPeer` in concurrent hashmap
        // 2. Take xor of the bitfield with myPeerInfo.getBitField() and decide whether an interested or not interested message is to be sent
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got HAVE message from peer " + context.getTheirPeerId() + "! Will check whether I have that pieceIndex and send interested/not-interested message");
        byte[] payload = message.getPayload();
        int haveIndex = ByteBuffer.allocate(payload.length).wrap(payload).getInt();
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setBitFieldIndex(haveIndex);
//        if (!myPeerInfo.getBitField().get(haveIndex)) {
        context.setState(new ExpectedToSendInterestedOrNotInterestedMessageState(neighbourConnectionsInfo, neighbourConnectionsInfo.get(context.getTheirPeerId()).getBitField(), false), false, false);
//        }
    }
}
