import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

public class Peer {
    private static final String CONFIG_DIR = "config";
    private static final String COMMON_CONFIGURATION_FILE = "Common.cfg";
    private static final String PEER_INFO_CONFIGURATION_FILE = "PeerInfo.cfg";

    /* Configuration variables */
    private static CommonConfig commonConfig;

    /* Self peer info variables */
    private static PeerInfo myPeerInfo;

    /* Track connected peers */
    private static volatile ConcurrentHashMap<Integer, PeerInfo> neighbourConnectionsMap = new ConcurrentHashMap<>();

    /* Peer Index, state and mutex */
    private static Integer peerIndex = 0;
    private static volatile List<Pair<Object, PeerState>>  inputHandlersAndTheirMutexes = new ArrayList<>();
    private static volatile List<Pair<Object, PeerState>>  outputHandlersAndTheirMutexes = new ArrayList<>();

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


        myPeerInfo = peerInfoBuilder.build();

        startListenerThread(myPeerInfo);

        if(!myPeerInfo.hasFile()){
            // Connect to peers in PeerInfo.cfg which appear above the current line
            makeConnections();
        }
    }

    private static void startListenerThread(PeerInfo peerInfo) {
        // Start the listener process to listen for new connections
        ConnectionListener listener = new ConnectionListener(peerInfo);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
    }

    public static void makeConnections(){
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

                // Spawn handlers for the new connection
                handleNewConnection(newConnection);

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

    public static class Handler {
        protected ObjectOutputStream out;
        protected ObjectInputStream in;
        protected PeerInfo myPeerInfo;
        protected int theirPeerIndex;

        public Handler(PeerInfo myPeerInfo, ObjectOutputStream out, ObjectInputStream in, int theirPeerIndex){
            this.theirPeerIndex = theirPeerIndex;
            this.out = out;
            this.in = in;
            this.myPeerInfo = myPeerInfo;
        }

        public void setState(int whichState, PeerState newState) {
            Pair<Object, PeerState> output = outputHandlersAndTheirMutexes.get(this.theirPeerIndex);
            Object outputHandlerMutex = output.first();

            Pair<Object, PeerState> input = inputHandlersAndTheirMutexes.get(this.theirPeerIndex);
            Object inputHandlerMutex = input.first();

            if(whichState == 0){
                output.setState(newState);
                input.setState(null);
                synchronized (outputHandlerMutex){
                    if(output.getState() != null){
                        outputHandlerMutex.notify();
                    }
                }
            }else{
                output.setState(null);
                input.setState(newState);
                synchronized (inputHandlerMutex){
                    if(input.getState() != null){
                        inputHandlerMutex.notify();
                    }
                }
            }
        }

        public NeighbourOutputHandler getOutputHandler(){
            return new NeighbourOutputHandler(this.myPeerInfo, this.out, this.in, this.theirPeerIndex);
        }

        public NeighbourInputHandler getInputHandler(){
            return new NeighbourInputHandler(this.myPeerInfo, this.out, this.in, this.theirPeerIndex);
        }
    }

    public static class NeighbourOutputHandler extends Handler implements  Runnable{

        public NeighbourOutputHandler(PeerInfo myPeerInfo, ObjectOutputStream out, ObjectInputStream in, int theirPeerIndex){
            super(myPeerInfo, out, in, theirPeerIndex);
        }

        public void handleMessage(ObjectOutputStream out, ObjectInputStream in){
            PeerState outputHandlerState = outputHandlersAndTheirMutexes.get(this.theirPeerIndex).second();
            outputHandlerState.handleMessage(this, this.myPeerInfo, in, out);
        }

        @Override
        public void run() {
            Pair<Object, PeerState> output = outputHandlersAndTheirMutexes.get(this.theirPeerIndex);
            Object outputHandlerMutex = output.first();
            PeerState outputHandlerState = output.second();

            try{
                while(true) {
                    synchronized (outputHandlerMutex){
                        System.out.println("Waiting for output handler  mutex");
                        outputHandlerMutex.wait();
                    }

                    System.out.println("Output handler mutex released");
                    if(outputHandlerState != null){
                        handleMessage(out, in);
                    }else{
                        throw new RuntimeException("Output state is null");
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static class NeighbourInputHandler extends  Handler implements Runnable{

        public NeighbourInputHandler(PeerInfo myPeerInfo, ObjectOutputStream out, ObjectInputStream in, int theirPeerIndex){
            super(myPeerInfo, out, in, theirPeerIndex);
        }

        public void handleMessage(ObjectOutputStream out, ObjectInputStream in){
            PeerState inputHandlerState = inputHandlersAndTheirMutexes.get(this.theirPeerIndex).second();
            inputHandlerState.handleMessage(this, this.myPeerInfo, in, out);
        }

        @Override
        public void run() {
            Pair<Object, PeerState> input = inputHandlersAndTheirMutexes.get(this.theirPeerIndex);
            Object inputHandlerMutex = input.first();
            PeerState inputHandlerState = input.second();

            try{

                while(true){
                    synchronized (inputHandlerMutex){
                        System.out.println("Waiting for input handler mutex");
                        inputHandlerMutex.wait();
                    }
                    System.out.println("Input handler mutex released");
                    if(inputHandlerState != null){
                        handleMessage(out, in);
                    }else{
                        throw new RuntimeException("Input state is null");
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static class ConnectionListener implements Runnable{
        PeerInfo peer;


        public ConnectionListener(PeerInfo peer){
            this.peer = peer;
        }

        @Override
        public void run() {
            try{
                ServerSocket listener = new ServerSocket(this.peer.getPortNumber());
                while(true){
                    System.out.println("Listening for connections....at "+ this.peer.getHostName() + ":" + this.peer.getPortNumber());

                    Socket newConnection = listener.accept();

                    // Spawn handlers for the new connection
                    handleNewConnection(newConnection);

                    System.out.println("Got a peer connection! Spawning Handlers for a peer");
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


            BitSet bitField = new BitSet();
            if(hasFile){
                int pieces = (int)Math.ceil(commonConfig.getFileSize()/commonConfig.getPieceSize());
                for(int i = 0; i < pieces; i++){
                    bitField.set(i);
                }
                List<byte[]> fileChunks = splitFileIntoChunks(commonConfig.getFileName(), commonConfig.getFileSize(), commonConfig.getPieceSize());
                builder.withBitFieldAndFileChunks(bitField, fileChunks);
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

    private static void handleNewConnection(Socket newConnection) throws IOException {
        /* Get output and input streams for the connection */
        ObjectOutputStream out = new ObjectOutputStream(newConnection.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(newConnection.getInputStream());

        /* Set mutexes and initial states */
        Pair<Object, PeerState> input = new Pair<>(new Object(), null);
        inputHandlersAndTheirMutexes.set(peerIndex, input);

        Pair<Object, PeerState> output = new Pair<>(new Object(), null);
        outputHandlersAndTheirMutexes.set(peerIndex, output);

        /* Initialize handler super class */
        Handler handler = new Handler(myPeerInfo, out, in, peerIndex);

        /* Factory methods */
        NeighbourInputHandler inputHandler = handler.getInputHandler();
        NeighbourOutputHandler outputHandler = handler.getOutputHandler();

        new Thread(inputHandler).start();
        new Thread(outputHandler).start();

        peerIndex++;
    }

    private void sendMessage(int toPeerId, PeerState whichMessageState){
    }

    private void expectMessage(int fromPeerId, PeerState whichExpectMessageState){
    }
}
