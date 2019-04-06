import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;

public class PeerConnection {

    private final SelfPeerInfo myPeerInfo;

    /* This peer's neighbours */
//    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionsInfo;

    public PeerConnection(SelfPeerInfo myPeerInfo) {
        this.myPeerInfo = myPeerInfo;
        // Set a comparator on download speed so that it will always track top k
        // Will have to check whether it updates dynamically or we need to remove the element in order to update it
        this.neighbourConnectionsInfo = new ConcurrentHashMap<>();
    }

    public void startListenerThread() {
        // Start the listener process to listen for new connections
        ConnectionListener listener = new ConnectionListener(this);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
    }

    public void handleNewConnection(Socket connection, boolean sendHandshake) {

        // Set up objects as condition vars
        Object inputMutex = new Object();
        Object outputMutex = new Object();

        // Set initial states
        AtomicReference<PeerState> inputStateRef = new AtomicReference<>(null);
        BlockingQueue<PeerState> outputStateRef = new LinkedBlockingDeque<>();

        // Get output and input streams of the socket
        PeerInfo.Builder hisPeerInfoBuilder = new PeerInfo.Builder();
        try {
            // !IMPORTANT NOTE!
            // Output stream needs to be created before input stream
            // Output stream needs to flushed to write the headers over the wire
            DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
            outputStream.flush();
            DataInputStream inputStream = new DataInputStream(connection.getInputStream());

            // Initialize handlers
            Handler handler = new Handler(connection, this.myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex);

            NeighbourInputHandler inputHandler = handler.getInputHandler();
            NeighbourOutputHandler outputHandler = handler.getOutputHandler();


            //  Start threads for input and output
            new Thread(inputHandler).start();
            new Thread(outputHandler).start();


            if (sendHandshake) {
                synchronized (outputMutex) {
                    outputStateRef.offer(new ExpectedToSendHandshakeMessageState(neighbourConnectionsInfo, hisPeerInfoBuilder));
                    outputMutex.notifyAll();
                }
            } else {
                synchronized (inputMutex) {
                    inputStateRef.set(new WaitForHandshakeMessageState(true, neighbourConnectionsInfo, hisPeerInfoBuilder));
                    inputMutex.notifyAll();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public SelfPeerInfo getMyPeerInfo() {
        return myPeerInfo;
    }

    public ConcurrentHashMap<Integer, NeighbourPeerInfo> getNeighbourConnectionsInfo() {
        return neighbourConnectionsInfo;
    }

    private class ConnectionListener implements Runnable {
        PeerConnection peerConnection;
        SelfPeerInfo myPeerInfo;
        ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourConnectionMap;

        public ConnectionListener(PeerConnection peerConnection) {
            this.peerConnection = peerConnection;
            this.myPeerInfo = this.peerConnection.getMyPeerInfo();
            this.neighbourConnectionMap = this.peerConnection.getNeighbourConnectionsInfo();
        }

        @Override
        public void run() {
            try {
                System.out.println("[PEER:" + this.myPeerInfo.getPeerID() + "]Listening for connections....at " + this.myPeerInfo.getHostName() + ":" + this.myPeerInfo.getPortNumber());
                ServerSocket listener = new ServerSocket(this.myPeerInfo.getPortNumber());
                while (true) {
                    Socket newConnection = listener.accept();

                    //Todo: Need to find a way to add this log
                    //peer.getLogger().info("Peer [peer_ID "+peer.getPeerID()+"] is connected from Peer[peer_ID "+myPeerInfo.getAddressToIDHash().get(peerIndex)+"]");
                    System.out.println("[PEER:" + this.myPeerInfo.getPeerID() + "]Got a peer connection! Spawning Handlers for a peer...");

                    // Spawn handlers for the new connection
                    handleNewConnection(newConnection, false);
                }
            } catch (IOException e) {
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
    }


}
