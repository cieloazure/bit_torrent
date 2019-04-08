import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.BitSet;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class WaitForHandshakeMessageStateTest {

    private Object inputMutex;
    private AtomicReference<PeerState> inputStateRef;
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo = new ConcurrentHashMap<>();
    private ByteArrayOutputStream bos;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        PeerInfo.Builder builder = new PeerInfo.Builder();

        // Following step in main
        Logger logger = null;
        logger = Peer.setUpLogger(1001, logger);

        BitSet bitfield = new BitSet(10);

        builder.withHostName("localhost")
               .withPortNumber(8000)
               .withHasFile(true)
               .withPeerID(1001)
               .withLogger(logger)
               .withFileChunks(null)
               .withBitField(bitfield);

        // Build my peer info
        SelfPeerInfo myPeerInfo = builder.buildSelfPeerInfo();

//        myPeerInfo.enableStdOutputLogging();

        // Fake their peer id
        int theirPeerId = 1002;

        PeerInfo.Builder builder2 = new PeerInfo.Builder();
        builder2.withPeerID(theirPeerId)
                .withHostName("localhost")
                .withPortNumber(8001)
                .withHasFile(false);

        NeighbourPeerInfo neighbourPeerInfo = builder2.buildNeighbourPeerInfo();
        neighbourConnectionsInfo.putIfAbsent(theirPeerId, neighbourPeerInfo);



        // Set up objects as condition vars
        inputMutex = new Object();
        Object outputMutex = new Object();

        // Set initial states
        inputStateRef = new AtomicReference<>(null);
        BlockingQueue<PeerState> outputStateRef = new LinkedBlockingDeque<>();

        // Fake input and output streams
        HandshakeMessage message = new HandshakeMessage(1002);
        ByteArrayInputStream bais = new ByteArrayInputStream(message.serialize());
        DataInputStream inputStream = new DataInputStream(bais);


        bos = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(bos);

        // Initialize handlers
        Handler handler = new Handler(myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex, theirPeerId);

        // Get handlers
        NeighbourInputHandler inputHandler = handler.getInputHandler();
        NeighbourOutputHandler outputHandler = handler.getOutputHandler();

        //  Start threads for input and output
        new Thread(inputHandler).start();
        new Thread(outputHandler).start();
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void handleMessageWithReply() throws InterruptedException {
        synchronized (inputMutex) {
            inputStateRef.set(new WaitForHandshakeMessageState(true, neighbourConnectionsInfo));
            inputMutex.notifyAll();
        }
        Thread.sleep(1000);
        while(bos.size() == 0){}
        HandshakeMessage reply = new HandshakeMessage(bos.toByteArray());
        assertEquals(1001, reply.getPeerID());
    }

    @org.junit.jupiter.api.Test
    void handleMessageWithOutReply() {
    }

    @org.junit.jupiter.api.Test
    void handleMessageInvalidHeader() {
    }

    @org.junit.jupiter.api.Test
    void handleMessageInvalid() {
    }
}