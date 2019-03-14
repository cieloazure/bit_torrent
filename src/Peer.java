import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.*;

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
    private static Logger logger;
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
        //Setting up the logger
        setUpLogger(peerID);
        peerInfoBuilder.withLogger(logger);
        // Parse peer info file
        parsePeerInfoConfigFile(peerID, commonConfig, peerInfoBuilder);
        buildAddressToPeerIDHash(peerInfoBuilder);
        // Build myPeerInfo object
        myPeerInfo = peerInfoBuilder.build();
        // Start listening for connections on this thread
        startListenerThread(myPeerInfo);

        if(!myPeerInfo.hasFile()){
            // Connect to peers in PeerInfo.cfg which appear above the current line
            makeConnections();
            System.out.println("TCP Connections done, moving to handshakes.....");
            doHandshakes();
        }
    }

    private static void doHandshakes() {
        System.out.println(peerIndex);
        for(int peer = 0; peer < peerIndex; peer++){
            sendMessage(peer, new ExpectedToSendHandshakeMessageState(neighbourConnectionsMap));
        }
    }

    private static void startListenerThread(PeerInfo peerInfo) {
        // Start the listener process to listen for new connections
        ConnectionListener listener = new ConnectionListener(peerInfo);
        System.out.println("Port "+peerInfo.getPortNumber());
        System.out.println("host "+peerInfo.getHostName());
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
                System.out.println("adding log entry");
                myPeerInfo.getLogger().info("Peer [ "+myPeerInfo.getPeerID()+"] makes a connection to Peer[peer_ID "+linePeerId+"]");
                // Make a connection with the peer
                Socket newConnection = new Socket(neighbourHostName, neighbourPortNumber);
                System.out.println("neighbourPortNumber "+neighbourPortNumber );
                System.out.println("getLocalAddress I am sending "+newConnection.getLocalAddress());
                System.out.println("getRemoteSocketAddress I am sending "+newConnection.getRemoteSocketAddress());
                System.out.println("getLocalSocketAddress I am sending "+newConnection.getLocalSocketAddress());
                System.out.println("getInetAddress I am sending "+newConnection.getInetAddress());
                System.out.println("getLocalPort I am sending "+newConnection.getLocalPort());
                System.out.println("getReuseAddress I am sending "+newConnection.getReuseAddress());

                // Spawn handlers for the new connection
                handleNewConnection(newConnection);

                // Send a handshake message
                // TODO: synchronize on peerIndex

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
        protected  Socket connection;
        protected  boolean wait;
        protected int whichHandler;

        public Handler(Socket connection, PeerInfo myPeerInfo, ObjectOutputStream out, ObjectInputStream in, int theirPeerIndex){
            this.theirPeerIndex = theirPeerIndex;
            this.out = out;
            this.in = in;
            this.myPeerInfo = myPeerInfo;
            this.connection = connection;
            this.wait = true;
        }

        public void setState(int whichState, PeerState newState) {
            Pair<Object, PeerState> output = outputHandlersAndTheirMutexes.get(this.theirPeerIndex);
            Object outputHandlerMutex = output.first();

            Pair<Object, PeerState> input = inputHandlersAndTheirMutexes.get(this.theirPeerIndex);
            Object inputHandlerMutex = input.first();

            if(whichState == 0){
                output.setState(newState);
                input.setState(null);
                if(this.whichHandler != 0){
                    synchronized (outputHandlerMutex){
                        outputHandlerMutex.notify();
                    }
                    this.wait = true;
                }else{
                    this.wait = false;
                }
            }else if(whichState == 1){
                input.setState(newState);
                output.setState(null);
                if(this.whichHandler != 1){
                    synchronized (inputHandlerMutex){
                        inputHandlerMutex.notify();
                    }
                    this.wait = true;
                }else{
                    this.wait = false;
                }
            }else{
                this.wait = true;
                input.setState(null);
                output.setState(null);
            }
        }

        public NeighbourOutputHandler getOutputHandler(){
            NeighbourOutputHandler noh =  new NeighbourOutputHandler(this.connection, this.myPeerInfo, this.out, this.in, this.theirPeerIndex);
            noh.setWhichHandler(0);
            return noh;
        }

        public NeighbourInputHandler getInputHandler(){
            NeighbourInputHandler nih = new NeighbourInputHandler(this.connection, this.myPeerInfo, this.out, this.in, this.theirPeerIndex);
            nih.setWhichHandler(1);
            return nih;
        }

        public String getHostName() {
            return this.connection.getInetAddress().getHostName();
        }

        public int getPortNumber(){
            return this.connection.getPort();
        }

        public int getTheirPeerIndex(){
            return this.theirPeerIndex;
        }

        public void setWhichHandler(int whichHandler) {
            this.whichHandler = whichHandler;
        }
    }

    public static class NeighbourOutputHandler extends Handler implements  Runnable{

        public NeighbourOutputHandler(Socket connection, PeerInfo myPeerInfo, ObjectOutputStream out, ObjectInputStream in, int theirPeerIndex){
            super(connection, myPeerInfo, out, in, theirPeerIndex);
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
                    if(wait){
                        synchronized (outputHandlerMutex){
                            System.out.println("Waiting for output handler  mutex");
                            outputHandlerMutex.wait();
                        }
                    }

                    System.out.println("Output handler mutex released");
                    output = outputHandlersAndTheirMutexes.get(this.theirPeerIndex);
                    outputHandlerState = output.second();

                    while(outputHandlerState == null){
                        output = outputHandlersAndTheirMutexes.get(this.theirPeerIndex);
                        outputHandlerState = output.second();
                    }

                    handleMessage(out, in);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    public static class NeighbourInputHandler extends  Handler implements Runnable{

        public NeighbourInputHandler(Socket connection, PeerInfo myPeerInfo, ObjectOutputStream out, ObjectInputStream in, int theirPeerIndex){
            super(connection, myPeerInfo, out, in, theirPeerIndex);
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
                    if(wait){
                        synchronized (inputHandlerMutex){
                            System.out.println("Waiting for input handler mutex");
                            inputHandlerMutex.wait();
                        }
                    }

                    System.out.println("Input handler mutex released");
                    input = inputHandlersAndTheirMutexes.get(this.theirPeerIndex);
                    inputHandlerState = input.second();

                    while(inputHandlerState == null){
                        input = inputHandlersAndTheirMutexes.get(this.theirPeerIndex);
                        inputHandlerState = input.second();
                    }

                    handleMessage(out, in);
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
                    peer.getLogger().info("port "+newConnection.getPort());
                    peer.getLogger().info("inet address "+newConnection.getInetAddress());
                    peer.getLogger().info("Local port "+newConnection.getRemoteSocketAddress());
                    System.out.println("getLocalAddress I am receiving "+newConnection.getLocalAddress());
                    System.out.println("getRemoteSocketAddress I am receiving "+newConnection.getRemoteSocketAddress());
                    System.out.println("getLocalSocketAddress I am receiving "+newConnection.getLocalSocketAddress());
                    System.out.println("getInetAddress I am receiving "+newConnection.getInetAddress());
                    System.out.println("getLocalPort I am receiving "+newConnection.getLocalPort());
                    System.out.println("getReuseAddress I am receiving "+newConnection.getReuseAddress());
                    peer.getLogger().info("Peer [peer_ID "+peer.getPeerID()+"] is connected from Peer[peer_ID "+myPeerInfo.getAddressToIDHash().get("localhost:"+newConnection.getPort())+"]");

                    // Spawn handlers for the new connection
                    handleNewConnection(newConnection);

                    // WaitForHandshakeMessageState with reply
                    // TODO: synchronize peerIndex and make it thread safe
                    expectMessage(peerIndex - 1, new WaitForHandshakeMessageState(true, neighbourConnectionsMap));


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

    private static List<byte[]> splitFileIntoChunks(String fileName,long fileSize, long pieceSize){
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

        System.out.println("Handling new connection at peer index: "+ peerIndex);
        /* Get output and input streams for the connection */
        ObjectOutputStream out = new ObjectOutputStream(newConnection.getOutputStream());
        ObjectInputStream in = new ObjectInputStream(newConnection.getInputStream());

        /* Set mutexes and initial states */
        Pair<Object, PeerState> input = new Pair<>(new Object(), null);
        inputHandlersAndTheirMutexes.add(peerIndex, input);

        Pair<Object, PeerState> output = new Pair<>(new Object(), null);
        outputHandlersAndTheirMutexes.add(peerIndex, output);

        /* Initialize handler super class */
        Handler handler = new Handler(newConnection, myPeerInfo, out, in, peerIndex);

        /* Factory methods */
        NeighbourInputHandler inputHandler = handler.getInputHandler();
        NeighbourOutputHandler outputHandler = handler.getOutputHandler();

        new Thread(inputHandler).start();
        new Thread(outputHandler).start();

        peerIndex++;
    }

    private static void sendMessage(int toPeerIndex, PeerState whichMessageState){
        Pair<Object, PeerState> output = outputHandlersAndTheirMutexes.get(toPeerIndex);
        Object outputHandlerMutex = output.first();

        output.setState(whichMessageState);
        synchronized (outputHandlerMutex){
            outputHandlerMutex.notify();
        }
    }

    private static void expectMessage(int fromPeerIndex, PeerState whichExpectMessageState){
        Pair<Object, PeerState> input = inputHandlersAndTheirMutexes.get(fromPeerIndex);
        Object inputHandlerMutex = input.first();

        input.setState(whichExpectMessageState);
        synchronized (inputHandlerMutex){
            inputHandlerMutex.notify();
        }
    }

    private static void setUpLogger(int peerID){
        try{
            FileHandler fh;

            System.setProperty("java.util.logging.SimpleFormatter.format",
              "[%1$tF %1$tT] %5$s %n");
            SimpleFormatter formatter = new SimpleFormatter();
            logger = Logger.getLogger("log_peer_"+peerID);
            logger.setUseParentHandlers(false);
            fh = new FileHandler("log_peer_"+peerID+".log");
            logger.addHandler(fh);
            fh.setFormatter(formatter);
            System.out.println("Setup log");
            } catch (Exception e) {
            }


    }
    private static void buildAddressToPeerIDHash(PeerInfo.Builder builder){
        try{
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + PEER_INFO_CONFIGURATION_FILE));
            HashMap<String, Integer> addressToID = new HashMap<>();
            String line = in.readLine();
            while (line != null) {
                String[] splitLine = line.split(" ");
                addressToID.put(splitLine[1]+":"+splitLine[2], Integer.parseInt(splitLine[0]));
                // read next line
                line = in.readLine();
            }
            builder.withAddressToIDHash(addressToID);

            in.close();
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Peer Info configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Peer Info configuration file");
            e.printStackTrace();
        }
    }
}
