import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Comparator;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;

public class PeerConnection {

    private PeerInfo myPeerInfo;

    /* This peer's neighbours */
//    private ConcurrentSkipListSet<PeerInfo> neighbourConnectionsInfo;
    private ConcurrentSkipListSet<PeerInfo> neighbourConnectionsInfo;

    PeerConnection(PeerInfo myPeerInfo){
        this.myPeerInfo = myPeerInfo;
        // Set a comparator on download speed so that it will always track top k
        // Will have to check whether it updates dynamically or we need to remove the element in order to update it
        Comparator<PeerInfo> comparator = new Comparator<PeerInfo>() {
            @Override
            public int compare(PeerInfo o1, PeerInfo o2) {
                return o1.getDownloadingSpeed().compareTo(o2.getDownloadingSpeed());
            }
        };
        this.neighbourConnectionsInfo = new ConcurrentSkipListSet<>(comparator);
    }

    public void startListenerThread() {
        // Start the listener process to listen for new connections
        ConnectionListener listener = new ConnectionListener(this);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
    }

    private class ConnectionListener implements Runnable{
        PeerConnection peerConnection;
        PeerInfo peer;
        ConcurrentSkipListSet<PeerInfo> neighbourConnectionMap;

        public ConnectionListener(PeerConnection peerConnection){
            this.peerConnection = peerConnection;
            this.peer = this.peerConnection.getMyPeerInfo();
            this.neighbourConnectionMap = this.peerConnection.getNeighbourConnectionsInfo();
        }

        @Override
        public void run() {
            try{
                System.out.println("[PEER:" + this.peer.getPeerID() + "]Listening for connections....at "+ this.peer.getHostName() + ":" + this.peer.getPortNumber());
                ServerSocket listener = new ServerSocket(this.peer.getPortNumber());
                while(true){
                    Socket newConnection = listener.accept();

                    System.out.println("[PEER:"+ this.peer.getPeerID() +"]Got a peer connection! Spawning Handlers for a peer...");

                    // Spawn handlers for the new connection
                    handleNewConnection(newConnection, false);
                }
            }catch(IOException e){
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
    }

    public void handleNewConnection(Socket connection, boolean sendHandshake){

        // Set up objects as condition vars
        Object inputMutex = new Object();
        Object outputMutex = new Object();


        // Set initial states
        AtomicReference<PeerState> inputStateRef = new AtomicReference<>(null);
        BlockingQueue<PeerState> outputStateQueue = new LinkedBlockingDeque<>();

        // Get output and input streams of the socket
        PeerInfo.Builder hisPeerInfoBuilder = new PeerInfo.Builder();
        hisPeerInfoBuilder.withInputHandlerVars(inputMutex, inputStateRef);
        hisPeerInfoBuilder.withOutputHandlerVars(outputMutex, outputStateQueue);
        try{
            // !IMPORTANT NOTE!
            // Output stream needs to be created before input stream
            // Output stream needs to flushed to write the headers over the wire
            ObjectOutputStream outputStream = new ObjectOutputStream(connection.getOutputStream());
            outputStream.flush();
            ObjectInputStream inputStream = new ObjectInputStream(connection.getInputStream());
            hisPeerInfoBuilder.withSocketAndItsStreams(connection, inputStream, outputStream);

            // Initialize handlers
            Handler handler = new Handler(connection, myPeerInfo, inputStateRef,  outputStateQueue,  inputStream, outputStream, inputMutex, outputMutex);

            NeighbourInputHandler inputHandler = handler.getInputHandler();
            NeighbourOutputHandler outputHandler = handler.getOutputHandler();


            //  Start threads for input and output
            new Thread(inputHandler).start();
            new Thread(outputHandler).start();


            if(sendHandshake){
                synchronized (outputMutex){
                    outputStateQueue.offer(new ExpectedToSendHandshakeMessageState(neighbourConnectionsInfo, hisPeerInfoBuilder));
                    outputMutex.notifyAll();
                }
            }else{
                synchronized (inputMutex){
                    inputStateRef.set(new WaitForHandshakeMessageState(true, neighbourConnectionsInfo, hisPeerInfoBuilder));
                    inputMutex.notifyAll();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public PeerInfo getMyPeerInfo() {
        return myPeerInfo;
    }

    public ConcurrentSkipListSet<PeerInfo> getNeighbourConnectionsInfo() {
        return neighbourConnectionsInfo;
    }


}
