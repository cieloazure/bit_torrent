import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;

public class Peer {
    public static final String CONFIG_DIR = "config";
    public static final String COMMON_CONFIGURATION_FILE = "Common.cfg";
    public static final String PEER_INFO_CONFIGURATION_FILE = "PeerInfo.cfg";

    /* Configuration variables */
    public static int numOfPreferredNeighbours;
    public static int unchokingInterval;
    public static int optimisticUnchokingInterval;
    public static String fileName;
    public static long fileSize;
    public static long pieceSize;

    /* Self peer info variables */
    public static int peerID;
    public static String hostName;

    public static int portNumber;
    public static boolean hasFile;
    public static BitSet bitField;
    public static List<byte[]> fileChunks;

    public static List<Socket> neighbourConnections;
    public static HashMap<Integer, PeerInfo> neighbourConnectionsMap;

    public static volatile PeerState inputState = null;
    public static volatile PeerState outputState = null;

    public static Object inputHandlerMutex = new Object();
    public static Object outputHandlerMutex = new Object();

    public static void main(String[] args){
        // Parse common config file
        parseCommonConfigFile();

        // Parse peer info file
        peerID = Integer.parseInt(args[0]);
        parsePeerInfoConfigFile(peerID);

        // Set the bitField array for the peer
        bitField = new BitSet();
        if(hasFile){
            setBitField();
            fileChunks = splitFileIntoChunks(fileName, fileSize, pieceSize);
        }

        PeerInfo peer = new PeerInfo(peerID, hostName, portNumber, hasFile, bitField);
        startListenerThread(peer);

        // Make connections to other peers yourself
        if(!hasFile){
            neighbourConnections = new ArrayList<>();
            makeConnections(peerID, neighbourConnections, peer);
        }
    }

    private static void setBitField() {
        int pieces = (int)Math.ceil(fileSize/pieceSize);
        for(int i = 0; i < pieces; i++){
            bitField.set(i);
        }
    }

    private static void startListenerThread(PeerInfo peer) {
        // Start the listener process to listen for new connections
        // Connect to peers in PeerInfo.cfg
        ConnectionListener listener = new ConnectionListener(peer, inputHandlerMutex, outputHandlerMutex);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();
    }

    public static void makeConnections(int peerID, List<Socket> neighbourConnections, PeerInfo peer){
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

            while(linePeerId != peerID){

                // Make a connection with the peer
                Socket newConnection = new Socket(neighbourHostName, neighbourPortNumber);
                ObjectOutputStream out = new ObjectOutputStream(newConnection.getOutputStream());
                ObjectInputStream inp = new ObjectInputStream(newConnection.getInputStream());

                NeighbourInputHandler inputHandler = new NeighbourInputHandler(newConnection, peer, out, inp, inputHandlerMutex, outputHandlerMutex);

                NeighbourOutputHandler outputHandler = new NeighbourOutputHandler(newConnection, peer, out, inp, inputHandlerMutex, outputHandlerMutex);

                new Thread(inputHandler).start();
                new Thread(outputHandler).start();

                outputState = new ExpectedToSendHandshakeMessageState();
                synchronized (outputHandlerMutex){
                    outputHandlerMutex.notify();
                }

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

    public static void parseCommonConfigFile() {
        try {
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" +COMMON_CONFIGURATION_FILE));
            numOfPreferredNeighbours = Integer.parseInt(in.readLine().split(" ")[1]);
            unchokingInterval = Integer.parseInt(in.readLine().split(" ")[1]);
            optimisticUnchokingInterval = Integer.parseInt(in.readLine().split(" ")[1]);
            fileName = in.readLine().split(" ")[1].trim();
            fileSize = Long.parseLong(in.readLine().split(" ")[1]);
            pieceSize = Long.parseLong(in.readLine().split(" ")[1]);
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Common configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Common configuration file");
            e.printStackTrace();
        }
    }

    public static void parsePeerInfoConfigFile(int peerID) {
        try{
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + PEER_INFO_CONFIGURATION_FILE));
            String peerInfoFileLine = in.readLine();
            int linePeerId = Integer.parseInt(peerInfoFileLine.split(" ")[0]);
            while(linePeerId != peerID){
               peerInfoFileLine = in.readLine();
               linePeerId = Integer.parseInt(peerInfoFileLine.split(" ")[0]);
            }
            String[] splitLine = peerInfoFileLine.split(" ");
            hostName = splitLine[1];
            portNumber = Integer.parseInt(splitLine[2]);
            hasFile = Integer.parseInt(splitLine[3]) == 1;
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Peer Info configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Peer Info configuration file");
            e.printStackTrace();
        }
    }

    public static List<byte[]> splitFileIntoChunks(String fileName, long fileSize, long pieceSize){
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

    public static class NeighbourOutputHandler implements  Handler, Runnable{
        public Socket connection;
        public ObjectOutputStream out;
        public ObjectInputStream in;
        public PeerInfo peer;

        private Object inputHandlerMutex;
        private Object outputHandlerMutex;


        public NeighbourOutputHandler(Socket connection, PeerInfo peer, ObjectOutputStream out, ObjectInputStream in, Object inputHandlerMutex, Object outputHandlerMutex){
            this.connection = connection;
            this.peer = peer;
            this.out = out;
            this.in = in;

            this.inputHandlerMutex = inputHandlerMutex;
            this.outputHandlerMutex = outputHandlerMutex;
        }

        public void handleMessage(ObjectOutputStream out, ObjectInputStream in){
            outputState.handleMessage(this, this.peer, in, out);
        }

        @Override
        public void setState(int whichState, PeerState newState) {
            if(whichState == 0){
                outputState = newState;
                inputState = null;
                synchronized (this.outputHandlerMutex){
                    if(outputState != null){
                        this.outputHandlerMutex.notify();
                    }
                }
            }else{
                outputState = null;
                inputState = newState;
                synchronized (this.inputHandlerMutex){
                    if(inputState != null){
                        this.inputHandlerMutex.notify();
                    }
                }
            }
        }

        @Override
        public void run() {
            try{
                while(true) {
                    synchronized (this.outputHandlerMutex){
                        System.out.println("Waiting for output handler  mutex");
                        this.outputHandlerMutex.wait();
                    }

                    System.out.println("Output handler mutex released");
                    if(outputState != null){
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

    public static class NeighbourInputHandler implements Handler, Runnable{
        public Socket connection;
        public ObjectOutputStream out;
        public ObjectInputStream in;
        public PeerInfo peer;

        private Object inputHandlerMutex;
        private Object outputHandlerMutex;

        public NeighbourInputHandler(Socket connection, PeerInfo peer, ObjectOutputStream out, ObjectInputStream in, Object inputHandlerMutex, Object outputHandlerMutex){
            this.connection = connection;
            this.peer = peer;
            this.out = out;
            this.in = in;

            this.inputHandlerMutex = inputHandlerMutex;
            this.outputHandlerMutex = outputHandlerMutex;
        }

        public void handleMessage(ObjectOutputStream out, ObjectInputStream in){
            inputState.handleMessage(this, this.peer, in, out);
        }

        public void setState(int whichState, PeerState newState){
            if(whichState == 0){
                outputState = newState;
                inputState = null;
                synchronized (this.outputHandlerMutex){
                    if(outputState != null){
                        this.outputHandlerMutex.notify();
                    }
                }
            }else{
                outputState = null;
                inputState = newState;
                synchronized (this.inputHandlerMutex){
                    if(inputState != null){
                        this.inputHandlerMutex.notify();
                    }
                }
            }
        }

        @Override
        public void run() {
            try{

                while(true){
                    synchronized (inputHandlerMutex){
                        System.out.println("Waiting for input handler mutex");
                        inputHandlerMutex.wait();
                    }
                    System.out.println("Input handler mutex released");
                    if(inputState != null){
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


        Object inputHandlerMutex;
        Object outputHandlerMutex;

        public ConnectionListener(PeerInfo peer, Object inputHandlerMutex, Object outputHandlerMutex){
            this.peer = peer;
            this.inputHandlerMutex = inputHandlerMutex;
            this.outputHandlerMutex = outputHandlerMutex;
        }

        @Override
        public void run() {
            try{
                ServerSocket listener = new ServerSocket(this.peer.portNumber);
                while(true){
                    System.out.println("Listening for connections....at "+ this.peer.hostName + ":" + this.peer.portNumber);

                    Socket newConnection = listener.accept();
                    ObjectInputStream in = new ObjectInputStream(newConnection.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(newConnection.getOutputStream());

                    NeighbourInputHandler inputHandler = new NeighbourInputHandler(newConnection, peer, out, in, this.inputHandlerMutex, this.outputHandlerMutex);

                    NeighbourOutputHandler outputHandler = new NeighbourOutputHandler(newConnection, peer, out, in, this.inputHandlerMutex, this.outputHandlerMutex);

                    new Thread(inputHandler).start();
                    new Thread(outputHandler).start();

                    /* Listen for handshake message */
                    inputState = new WaitForHandshakeMessageState(true);
                    synchronized (inputHandlerMutex){
                        inputHandlerMutex.notify();
                    }

                    System.out.println("Got a peer connection! Spawning Handlers for a peer");
                }
            }catch(IOException e){
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
    }
}
