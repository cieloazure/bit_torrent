import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.nio.ByteBuffer;
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
    private HandshakeMessage message;
    private SelfPeerInfo myPeerInfo;

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
        myPeerInfo = builder.buildSelfPeerInfo();

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
    }

    private void setup(SelfPeerInfo myPeerInfo, int theirPeerId, HandshakeMessage message) {
        // Fake input and output streams
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

    private byte[] getHandshakeMessageWithIncorrectHeader(int peerID){
        final String header = "WRONG_HEADER";
        int handshakeMessageSize = header.length() + 10 + 4;
        byte[] result = new byte[handshakeMessageSize];
        int i = 0;
        for (char c : header.toCharArray()) {
            if (c != '\0') {
                result[i] = (byte) c;
                i++;
            }
        }

        for (int j = 0; j < 10; j++) {
            result[i] = (byte) 0;
            i++;
        }

        byte[] peerIDBytes = ByteBuffer.allocate(4).putInt(peerID).array();
        for (int j = 0; j < peerIDBytes.length; j++) {
            result[i] = peerIDBytes[j];
            i++;
        }

        return result;
    }

    @org.junit.jupiter.api.AfterEach
    void tearDown() {
    }

    @org.junit.jupiter.api.Test
    void handleMessageWithReply() throws InterruptedException {
        // Still setup
        message = new HandshakeMessage(1002);
        setup(myPeerInfo, 1002, message);

        // Test starts
        synchronized (inputMutex) {
            inputStateRef.set(new WaitForHandshakeMessageState(true, neighbourConnectionsInfo));
            inputMutex.notifyAll();
        }
        // busy waiting
        System.out.printf("\n-------IGNORE EXCEPTIONS------\nThis is due to transition on state we are not testing.\n As we are mocking a peer there is no two way communication for unit test\nShould expect to see an exception of type `ArrayIndexOutOfBoundsException`");

        while(bos.size() == 0){}
        HandshakeMessage reply = new HandshakeMessage(bos.toByteArray());
        assertEquals(1001, reply.getPeerID());
        assertTrue(reply.isValid());
        assertTrue(inputStateRef.get() instanceof WaitForBitFieldMessageState);
        assertEquals(0, outputStateRef.size());
    }

    @org.junit.jupiter.api.Test
    void handleMessageWithOutReply() throws InterruptedException {
        // Still setup
        message = new HandshakeMessage(1002);
        setup(myPeerInfo, 1002, message);

        // Test starts
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
                        System.out.printf("\nWaiting done! This means there was an output message to be sent in the queue.\n However, the outputHandler of the peer consumes it, hence we don't get to see it.\nLet's be satisfied that while waiting for an message to be put in blocking queue after `notifyAll()` we got a message. Checking the input state after it\n");
                        assertTrue(true);
                        assertTrue(inputStateRef.get() instanceof WaitForBitFieldMessageState);
                        return;
                    }
                }
            }
        };

        new Thread(runnable).start();
        System.out.printf("\n-------IGNORE EXCEPTIONS------\nThis is due to transition on state we are not testing.\n As we are mocking a peer there is no two way communication for unit test\nShould expect to see an exception of type `ArrayIndexOutOfBoundsException`\n");
        Thread.sleep(2000);
        synchronized (inputMutex) {
            inputStateRef.set(new WaitForHandshakeMessageState(false, neighbourConnectionsInfo));
            inputMutex.notifyAll();
        }
        Thread.sleep(2000);
    }

    @org.junit.jupiter.api.Test
    void handleMessageInvalidPeerID() throws InterruptedException {
        // Create a situation of invalid header
        // Handler is for 1002 but we are sending peerID of 1003
        message = new HandshakeMessage(1003);
        setup(myPeerInfo, 1002, message);
        myPeerInfo.enableStdOutputLogging();

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
                        System.out.printf("\nWaiting done! This means there was an output message to be sent in the queue.\n However, the outputHandler of the peer consumes it, hence we don't get to see it.\nLet's be satisfied that while waiting for an message to be put in blocking queue after `notifyAll()` we got a message.\n Checking the input state after it\n");
                        assertTrue(true);
                        System.out.printf("\nAs an error has occured in Handshake state, we will send a handshake message again and go in wait for handshake state.\n However, an error occurs in handshake state as we don't have anything left in the input stream. Expecting a `EOF Exception`\n");
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
    void handleMessageInvalidHeader() throws InterruptedException {
        message = new HandshakeMessage(getHandshakeMessageWithIncorrectHeader(1002));
        setup(myPeerInfo, 1002, message);
        myPeerInfo.enableStdOutputLogging();

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
                        System.out.printf("\nWaiting done! This means there was an output message to be sent in the queue.\n However, the outputHandler of the peer consumes it, hence we don't get to see it.\nLet's be satisfied that while waiting for an message to be put in blocking queue after `notifyAll()` we got a message.\n Checking the input state after it\n");
                        assertTrue(true);
                        System.out.printf("\nAs an error has occured in Handshake state, we will send a handshake message again and go in wait for handshake state.\n However, an error occurs in handshake state as we don't have anything left in the input stream. Expecting a `EOF Exception`\n");
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
}