import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.util.BitSet;
import java.util.TreeMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.*;

class WaitForHandshakeMessageStateTest {

    private Object inputMutex;
    private Object outputMutex;
    private AtomicReference<PeerState> inputStateRef;
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo = new ConcurrentHashMap<>();
    private ByteArrayOutputStream bos;
    private Handler handler;
    private BlockingQueue<PeerState> outputStateRef;

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
        outputMutex = new Object();

        // Set initial states
        inputStateRef = new AtomicReference<>(null);
        outputStateRef = new LinkedBlockingDeque<>();

        // Fake input and output streams
        HandshakeMessage message = new HandshakeMessage(1002);
        ByteArrayInputStream bais = new ByteArrayInputStream(message.serialize());
        DataInputStream inputStream = new DataInputStream(bais);

        bos = new ByteArrayOutputStream();
        DataOutputStream outputStream = new DataOutputStream(bos);

        // Initialize handlers
        handler = new Handler(myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex, theirPeerId);

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
        // busy waiting
        while(bos.size() == 0){}
        HandshakeMessage reply = new HandshakeMessage(bos.toByteArray());
        assertEquals(1001, reply.getPeerID());
        assertTrue(reply.isValid());
        assertTrue(inputStateRef.get() instanceof WaitForBitFieldMessageState);
        assertEquals(0, outputStateRef.size());
    }

    @org.junit.jupiter.api.Test
    void handleMessageWithOutReply() throws InterruptedException {
        Runnable runnable = () -> {
            while(true){
                synchronized (outputMutex) {
                    while (outputStateRef.isEmpty()) {
                        System.out.println("Waiting on output handler!");
                        try {
                            outputMutex.wait();
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        System.out.println("Waiting done! This means there was an output message to be sent in the queue.\n However, the outputHandler of the peer consumes it, hence we don't get to see it.\n Checking the input state after it");
                        assertTrue(true);
                        assertTrue(inputStateRef.get() instanceof WaitForBitFieldMessageState);
                        return;
                    }
                }
            }
        };

        new Thread(runnable).start();
        Thread.sleep(2000);
        synchronized (inputMutex) {
            inputStateRef.set(new WaitForHandshakeMessageState(false, neighbourConnectionsInfo));
            inputMutex.notifyAll();
        }
        Thread.sleep(2000);
    }

    @org.junit.jupiter.api.Test
    void handleMessageInvalidHeader() {
    }

    @org.junit.jupiter.api.Test
    void handleMessageInvalid() {
    }
}