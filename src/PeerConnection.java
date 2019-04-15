import java.io.*;
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

    public void handleNewConnection(Socket connection, boolean sendHandshake, DataInputStream inputStream, DataOutputStream outputStream, int theirPeerId) {

        // Set up objects as condition vars
        Object inputMutex = new Object();
        Object outputMutex = new Object();

        // Set initial states
        AtomicReference<PeerState> inputStateRef = new AtomicReference<>(null);
        BlockingQueue<PeerState> outputStateRef = new LinkedBlockingDeque<>();

        // Initialize handlers
        Handler handler = new Handler(connection, this.myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex, theirPeerId);


        NeighbourInputHandler inputHandler = handler.getInputHandler(neighbourConnectionsInfo);
        NeighbourOutputHandler outputHandler = handler.getOutputHandler(neighbourConnectionsInfo);


        //  Start threads for input and output
        new Thread(inputHandler).start();
        new Thread(outputHandler).start();


        if (sendHandshake) {
            synchronized (outputMutex) {
                outputStateRef.offer(new ExpectedToSendHandshakeMessageState(neighbourConnectionsInfo));
                outputMutex.notifyAll();
            }
        } else {
            synchronized (inputMutex) {
                inputStateRef.set(new WaitForHandshakeMessageState(true, neighbourConnectionsInfo));
                inputMutex.notifyAll();
            }
        }

    }

    public SelfPeerInfo getMyPeerInfo() {
        return myPeerInfo;
    }

    public ConcurrentHashMap<Integer, NeighbourPeerInfo> getNeighbourConnectionsInfo() {
        return neighbourConnectionsInfo;
    }

    public void startScheduledExecution(int topKinterval, int optUnchokedInt, int peerCount) {
        // ScheduledExecutorService object which spawns the threads to execute periodic tasks like
        // selectKtopNeighbors and selectOptUnchNeighbor
        // TODO: Should there be a thread for the termination check as well?
        // TODO: (which periodically checks if everyone has the file and then triggers a graceful shutdown)
        PeriodicTasks pt = new PeriodicTasks(myPeerInfo, neighbourConnectionsInfo, peerCount);
        pt.startScheduledExecution(topKinterval, optUnchokedInt);
    }

    public void initializeNeighbourConnectionsInfo(String peerInfoConfigFile) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(peerInfoConfigFile));
            String peerInfoFileLine = in.readLine();
            int numberOfPeers = 0;
            while (peerInfoFileLine != null) {
                String[] splitLine = peerInfoFileLine.split(" ");
                int peerId = Integer.parseInt(splitLine[0]);
                if (peerId != this.myPeerInfo.getPeerID()) {
                    PeerInfo.Builder builder = new PeerInfo.Builder();
                    builder.withPeerID(peerId)
                            .withHostName(splitLine[1])
                            .withPortNumber(Integer.parseInt(splitLine[2]))
                            .withHasFile(Boolean.parseBoolean(splitLine[3]));

                    NeighbourPeerInfo neighbourPeerInfo = builder.buildNeighbourPeerInfo();
                    this.neighbourConnectionsInfo.putIfAbsent(peerId, neighbourPeerInfo);
                    numberOfPeers++;
                }
                peerInfoFileLine = in.readLine();
            }

            myPeerInfo.setMyNeighboursCount(numberOfPeers);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                myPeerInfo.log("[PEER:" + this.myPeerInfo.getPeerID() + "]Listening for connections....at " + this.myPeerInfo.getHostName() + ":" + this.myPeerInfo.getPortNumber());
                ServerSocket listener = new ServerSocket(this.myPeerInfo.getPortNumber());
                listener.setReceiveBufferSize(myPeerInfo.getCommonConfig().RECEIVE_BUFFER_SIZE);

                this.myPeerInfo.setListener(listener);
                while (true) {
                    Socket newConnection = listener.accept();
                    newConnection.setReceiveBufferSize(myPeerInfo.getCommonConfig().RECEIVE_BUFFER_SIZE);

                    // Get input and output streams of the socket
                    // !IMPORTANT NOTE!
                    // Output stream needs to be created before input stream
                    // Output stream needs to flushed to write the headers over the wire
                    DataOutputStream outputStream = new DataOutputStream(newConnection.getOutputStream());
                    outputStream.flush();
                    DataInputStream inputStream = new DataInputStream(newConnection.getInputStream());

                    // Read the peer id for connection
                    int theirPeerId = inputStream.readInt();

                    myPeerInfo.log("Peer [peer_ID " + myPeerInfo.getPeerID() + "] is connected from Peer[peer_ID " + theirPeerId + "]");

                    // Spawn handlers for the new connection
                    handleNewConnection(newConnection, false, inputStream, outputStream, theirPeerId);
                }

            } catch (IOException e) {
//                e.printStackTrace();
//                myPeerInfo.log(e.getMessage());
            }
        }
    }

}
