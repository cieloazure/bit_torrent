import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Peer {
    private static final String CONFIG_DIR = "config";
    private static final String COMMON_CONFIGURATION_FILE = "Common.cfg";
    private static final String PEER_INFO_CONFIGURATION_FILE = "PeerInfo.cfg";

    /* Configuration variables */
    private static CommonConfig commonConfig;

    /* Self peer info variables */
    private static PeerInfo myPeerInfo;

    /* Track connected peers */
    private static ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsMap = new ConcurrentHashMap<>();


    public static void main(String[] args){
        // Parse common config file
        CommonConfig.Builder configBuilder = new CommonConfig.Builder();
        parseCommonConfigFile(configBuilder);
        commonConfig = configBuilder.build();

        PeerInfo.Builder peerInfoBuilder = new PeerInfo.Builder();

        // Set peer ID
        // TODO: Check for NAN exception, terminate program in that case
        int peerID = Integer.parseInt(args[0]);
        peerInfoBuilder.withPeerID(peerID);

        // Parse peer info file
        parsePeerInfoConfigFile(peerID, commonConfig, peerInfoBuilder);

        // Build myPeerInfo object
        myPeerInfo = peerInfoBuilder.build();

        // Start listening for connections on this thread
        startListenerThread(myPeerInfo);

        if(!myPeerInfo.hasFile()){
            // Connect to peers in PeerInfo.cfg which appear above the current line
            makeConnections();
//            System.out.println("TCP Connections done, moving to handshakes.....");
//            doHandshakes();
        }
    }

    private static void startListenerThread(PeerInfo peerInfo) {
        // Start the listener process to listen for new connections
        ConnectionListener listener = new ConnectionListener(peerInfo);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
    }

    private static void makeConnections(){
        try{

            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + PEER_INFO_CONFIGURATION_FILE));
            // read line
            String peerInfoFileLine = in.readLine();

            // split line
            String[] splitLine = peerInfoFileLine.split(" ");

            // Get variables from the line
            int linePeerId = Integer.parseInt(splitLine[0]);
            String neighbourHostName = splitLine[1];
            int neighbourPortNumber = Integer.parseInt(splitLine[2]);

            while(linePeerId != myPeerInfo.getPeerID()){

                // Make a connection with the peer
                Socket newConnection = new Socket(neighbourHostName, neighbourPortNumber);

                System.out.println("[PEER:"+ myPeerInfo.getPeerID() +"]Connecting to a peer "+ linePeerId + "....");
                // Spawn handlers for the new connection
                handleNewConnection(newConnection, true);

                // read next line
                peerInfoFileLine = in.readLine();
                splitLine = peerInfoFileLine.split(" ");
                linePeerId = Integer.parseInt(splitLine[0]);
                neighbourHostName = splitLine[1];
                neighbourPortNumber = Integer.parseInt(splitLine[2]);
            }
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Peer Info configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Peer Info configuration file");
            e.printStackTrace();
        }
    }

    private static class ConnectionListener implements Runnable{
        PeerInfo peer;


        public ConnectionListener(PeerInfo peer){
            this.peer = peer;
        }

        @Override
        public void run() {
            try{
                System.out.println("[PEER:" + myPeerInfo.getPeerID() + "]Listening for connections....at "+ this.peer.getHostName() + ":" + this.peer.getPortNumber());
                ServerSocket listener = new ServerSocket(this.peer.getPortNumber());
                while(true){
                    Socket newConnection = listener.accept();

                    System.out.println("[PEER:"+ myPeerInfo.getPeerID() +"]Got a peer connection! Spawning Handlers for a peer...");

                    // Spawn handlers for the new connection
                    handleNewConnection(newConnection, false);
                }
            }catch(IOException e){
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
    }

    private static void parseCommonConfigFile(CommonConfig.Builder configBuilder) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" +COMMON_CONFIGURATION_FILE));
            int numOfPreferredNeighbours = Integer.parseInt(in.readLine().split(" ")[1]);
            int unchokingInterval = Integer.parseInt(in.readLine().split(" ")[1]);
            int optimisticUnchokingInterval = Integer.parseInt(in.readLine().split(" ")[1]);
            String fileName = in.readLine().split(" ")[1].trim();
            long fileSize = Long.parseLong(in.readLine().split(" ")[1]);
            long pieceSize = Long.parseLong(in.readLine().split(" ")[1]);

            configBuilder.withNumOfPreferredNeighboursAs(numOfPreferredNeighbours)
                         .withOptimisticUnchokingIntervalAs(optimisticUnchokingInterval)
                         .withUnchokingIntervalAs(unchokingInterval)
                         .withFileParametersAs(fileName, fileSize, pieceSize);

        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Common configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Common configuration file");
            e.printStackTrace();
        }
    }

    private static void parsePeerInfoConfigFile(int peerID, CommonConfig commonConfig, PeerInfo.Builder builder) {
        try{
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + PEER_INFO_CONFIGURATION_FILE));
            String peerInfoFileLine = in.readLine();
            int linePeerId = Integer.parseInt(peerInfoFileLine.split(" ")[0]);
            while(linePeerId != peerID){
                peerInfoFileLine = in.readLine();
                linePeerId = Integer.parseInt(peerInfoFileLine.split(" ")[0]);
            }
            String[] splitLine = peerInfoFileLine.split(" ");

            String hostName = splitLine[1];
            int portNumber = Integer.parseInt(splitLine[2]);
            builder.withHostNameAndPortNumber(hostName, portNumber);

            boolean hasFile = Integer.parseInt(splitLine[3]) == 1;
            builder.withHasFile(hasFile);


            int pieces = (int)Math.ceil(commonConfig.getFileSize()/commonConfig.getPieceSize());
            BitSet bitField = new BitSet(pieces);
            if(hasFile){
                for(int i = 0; i < pieces; i++){
                    bitField.set(i);
                }
                List<byte[]> fileChunks = splitFileIntoChunks(commonConfig.getFileName(), commonConfig.getFileSize(), commonConfig.getPieceSize());
                builder.withBitFieldAndFileChunks(bitField, fileChunks);
            }else{
                builder.withBitFieldAndFileChunks(bitField, null);
            }
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Peer Info configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Peer Info configuration file");
            e.printStackTrace();
        }
    }

    private static List<byte[]> splitFileIntoChunks(String fileName, long fileSize, long pieceSize){
        List<byte[]> chunks = new ArrayList<>();
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            int pieces = (int)Math.ceil(fileSize / pieceSize);
            for(int i = 0; i < pieces; i++){
                byte[] buffer = new byte[(int)pieceSize];
                while(fis.read(buffer) > 0){
                    chunks.add(buffer);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return chunks;
    }

    private static void handleNewConnection(Socket connection, boolean sendHandshake){
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
                    outputStateQueue.offer(new ExpectedToSendHandshakeMessageState(neighbourConnectionsMap, hisPeerInfoBuilder));
                    outputMutex.notifyAll();
                }
            }else{
                synchronized (inputMutex){
                    inputStateRef.set(new WaitForHandshakeMessageState(true, neighbourConnectionsMap, hisPeerInfoBuilder));
                    inputMutex.notifyAll();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
