import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.BitSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

public class WaitForAnyMessageState implements PeerState {
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;


    public WaitForAnyMessageState(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo) {
        this.neighbourConnectionsInfo = neighbourConnectionsInfo;
    }

    @Override
    public void handleMessage(Handler context, SelfPeerInfo myPeerInfo, DataInputStream inputStream, DataOutputStream outputStream) {
        try {
            myPeerInfo.log("\n[PEER:" + myPeerInfo.getPeerID() + "]Waiting for any message....from peer id " + context.getTheirPeerId() + " whose current state is " + this.neighbourConnectionsInfo.get(context.getTheirPeerId()).getNeighbourState() + "\n");

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


            ActualMessage message = new ActualMessage(messageBytes);

            myPeerInfo.log("\n[PEER:" + myPeerInfo.getPeerID() + "][WaitForAnyMessageState] Received a message with following stats\n1. Message Type:" + message.getMessageType() + "\n2.Message Length:" + message.getMessageLength() + "\n3. Validity:" + message.isValid() + "\n");

            if (message.isValid()) {
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
                    case FAILED:
                        handleIncomingFailedMessage(context, message, neighbourConnectionsInfo, myPeerInfo);

                }
            } else {
                handleMessageFailure(context, myPeerInfo);
            }

            context.setState(new WaitForAnyMessageState(neighbourConnectionsInfo), true, false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void handleIncomingNotInterestedMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        // 1. Update the state of the peer in concurrent hash map, this will be used when decided the peers to upload to, not interested peers won't be considered at all
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got NOT INTERESTED message from peer " + context.getTheirPeerId() + "! Updating the state in hashmap to be used in next interval");
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

        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got INTERESTED message from peer " + context.getTheirPeerId() + "! Updating the state in hashmap to be used in next interval");
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

        theirBitSet = (BitSet) (neighbourConnectionsInfo.get(context.getTheirPeerId())).getBitField().clone();
        missing = (BitSet) (myPeerInfo.getBitField()).clone();

        System.out.println("Server bitset:" + theirBitSet);
        System.out.println("client bitset:" + missing);

        missing.xor(theirBitSet);

        //pieces that are missing from client and present in server
        missing.and(theirBitSet);

        toRequest = (BitSet) missing.clone();
        //pieces that are not yet requested.
        toRequest.andNot(myPeerInfo.getRequestedPieces());

        System.out.printf("Bitset to be requested" + toRequest);

        // 2. From the set bits choose any random index (which has not been requesssted before)
        if (toRequest.cardinality() > 0) {

            int randomIndex = ThreadLocalRandom.current().nextInt(0, toRequest.cardinality());
            int pieceToRequest = toRequest.nextSetBit(randomIndex);

            // 3. Send a request message with that index
            ExpectedToSendRequestMessageState requestMessageState = new ExpectedToSendRequestMessageState(this.neighbourConnectionsInfo, pieceToRequest);
            context.setLastStateRef(requestMessageState);
            context.setState(requestMessageState, false, false);

            //4.Set the piece index in requestedPieces bitset
            myPeerInfo.setRequestPiecesIndex(pieceToRequest, 0);
        } else {
            System.out.println("RECEIVED ENTIRE FILE!");
            myPeerInfo.combineFileChunks();
            context.setState(new ExpectedToSendInterestedOrNotInterestedMessageState(neighbourConnectionsInfo, neighbourConnectionsInfo.get(context.getTheirPeerId()).getBitField(), false), false, false);
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

        byte[] payload = message.getPayload();

        // 1. Get the piece index from the fileChunks
        ByteBuffer buffer = ByteBuffer.allocate(payload.length).wrap(payload);
        int pieceIndex = buffer.getInt();
        myPeerInfo.log("RECEIVED REQUEST MESSAGE from " + context.getTheirPeerId() + " for piece " + pieceIndex);
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got REQUEST message from peer " + context.getTheirPeerId() + " for piece index " + pieceIndex + "! Will check the neighbour state and send a piece message if unchoked");

        // 2. Send a piece with that index through a piece message payload on output thread
        // 2.1. Check if the state is unchoked
        if (neighbourConnectionsInfo.get(context.getTheirPeerId()).isUnChoked()) {
            ExpectedToSendPieceMessageState pieceMessageState = new ExpectedToSendPieceMessageState(neighbourConnectionsInfo, pieceIndex);
            context.setLastStateRef(pieceMessageState);
            context.setState(pieceMessageState, false, false);
        } else {
            System.out.println("Received request but state is not UNCHOKED!");
        }
    }

    private void handleIncomingPieceMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, Double downloadSpeed, SelfPeerInfo myPeerInfo) {
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got PIECE message from peer " + context.getTheirPeerId() + "! Will send have messages to all neighbours and next request message to the peer");
        // 1. Track the download speed of the message by putting start time and end time around read bytes
        // Handled in handler message

        // 1.1. Get the piece index that was requested from this peer
        int gotPieceIndex = neighbourConnectionsInfo.get(context.getTheirPeerId()).getRequestedPieceIndex();

        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Got PIECE message " + gotPieceIndex + " from peer " + context.getTheirPeerId() + "! Will send have messages to all neighbours and next request message to the peer");

        // 2. Update the download speed of the peer in the concurrent hashmap
        neighbourConnectionsInfo.get(context.getTheirPeerId()).setDownloadSpeed(downloadSpeed);

        // 3. Update our bitfield
        myPeerInfo.setBitFieldIndex(gotPieceIndex);
        myPeerInfo.setRequestPiecesIndex(gotPieceIndex, 1);
        // 3.1. Update file chunk index
        myPeerInfo.setFileChunkIndex(gotPieceIndex, message.getPayload());

        // 4. Send a have message to all the neighbouring peers
        for (Integer peerId : neighbourConnectionsInfo.keySet()) {
            NeighbourPeerInfo peerInfo = neighbourConnectionsInfo.get(peerId);
            if (peerInfo.getContext() != null) {
                ExpectedToSendHaveMessageState haveMessageState = new ExpectedToSendHaveMessageState(neighbourConnectionsInfo, gotPieceIndex);
                context.setLastStateRef(haveMessageState);
                peerInfo.setContextState(haveMessageState, false, false);
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

    private void handleMessageFailure(Handler context, SelfPeerInfo myPeerInfo) {
        myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "] FAILED message from peer " + context.getTheirPeerId() + "!");

        context.setState(new ExpectedToSendFailedMessageState(), false, false);
        //5. Track to which peer we have sent a request message with that index, next time an unchoke message arrives, do not use the same index again, :

    }

    private void handleIncomingFailedMessage(Handler context, ActualMessage message, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo, SelfPeerInfo myPeerInfo) {
        System.out.println("Handle incoming failed message");
        context.setState(context.getLastStateRef(), false, false);
    }
}
