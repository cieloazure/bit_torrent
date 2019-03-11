import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

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

    public static void main(String[] args){
        // Parse common config file
        parseCommonConfigFile();

        // Parse peer info file
        peerID = Integer.parseInt(args[0]);
        parsePeerInfoConfigFile(peerID);

        // Set the bitField array for the peer
        bitField = new BitSet();
        if(hasFile){
            int pieces = (int)Math.ceil(fileSize/pieceSize);
            for(int i = 0; i < pieces; i++){
                bitField.set(i);
            }
            fileChunks = splitFileIntoChunks(fileName, fileSize, pieceSize);
        }

        // Start the listener process to listen for new connections
        // Connect to peers in PeerInfo.cfg
        PeerInfo peer = new PeerInfo(peerID, hostName, portNumber, hasFile, bitField);
        ConnectionListener listener = new ConnectionListener(peer);
        Thread listenerThread = new Thread(listener);
        listenerThread.start();

        // Make connections to other peers yourself
        if(!hasFile){
            neighbourConnections = new ArrayList<>();
            makeConnections(peerID, neighbourConnections, peer);
        }
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
                Socket newConnection = new Socket(neighbourHostName, neighbourPortNumber);
                ObjectOutputStream out = new ObjectOutputStream(newConnection.getOutputStream());
                ObjectInputStream inp = new ObjectInputStream(newConnection.getInputStream());

                PeerState currentInputState = null;
                Object mutexObject2 = new Object();
                NeighbourInputHandler inputHandler = new NeighbourInputHandler(newConnection, peer, out, inp, currentInputState, mutexObject2);


                Object mutexObject = new Object();
                PeerState currentOutputState = new ExpectedToSendHandshakeMessageState();
                NeighbourOutputHandler outputHandler = new NeighbourOutputHandler(newConnection, peer, mutexObject, out, inp, currentOutputState, currentInputState);

                new Thread(inputHandler).start();
                new Thread(outputHandler).start();

//                synchronized (mutexObject){
//                    mutexObject.notify();
//                }


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

    public static class ConnectionListener implements Runnable{
        PeerInfo peer;

        public ConnectionListener(PeerInfo peer){
            this.peer = peer;
        }

        @Override
        public void run() {
            try{
                ServerSocket listener = new ServerSocket(this.peer.portNumber);
                while(true){
                    System.out.println("Listening for conenctions....at "+ this.peer.hostName + ":" + this.peer.portNumber);

                    Socket newConnection = listener.accept();
                    ObjectInputStream in = new ObjectInputStream(newConnection.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(newConnection.getOutputStream());

                    PeerState currentInputState = new WaitForHandshakeMessageState(true);

                    Object mutexObject2 = new Object();
                    NeighbourInputHandler inputHandler = new NeighbourInputHandler(newConnection, this.peer, out, in, currentInputState, mutexObject2);

                    Object mutexObject = new Object();
                    PeerState currentOutputState = null;

                    NeighbourOutputHandler outputHandler = new NeighbourOutputHandler(newConnection, this.peer, mutexObject, out, in, currentInputState, currentOutputState);

                    new Thread(inputHandler).start();
                    new Thread(outputHandler).start();


                    synchronized (mutexObject2){
                        System.out.println("Opening input");
                        mutexObject2.notify();
                    }

                    System.out.println("Got a peer connection! Spawning Handlers for a peer");
                }
            }catch(IOException e){
                e.printStackTrace();
                System.out.println(e.getMessage());
            }
        }
    }

    public abstract static class Handler{
        public abstract void setState(PeerState state);
    }

    public static class NeighbourOutputHandler extends Handler implements  Runnable{
        public Socket connection;
        public ObjectOutputStream out;
        public ObjectInputStream in;
        public PeerInfo peer;
        public Object mutex;
        public PeerState currentState;
        public PeerState currentInputState;
        public Object inputMutex;

        public NeighbourOutputHandler(Socket connection, PeerInfo peer, Object mutex, ObjectOutputStream out, ObjectInputStream in, PeerState currentState, PeerState currentInputState, Object inputMutex){
            this.connection = connection;
            this.peer = peer;
            this.mutex = mutex;
            this.inputMutex = inputMutex;
            this.out = out;
            this.in = in;
            this.currentState = currentState;
            this.currentInputState = currentInputState;
        }

        public void setState(PeerState newState){
            this.currentInputState = newState;
            synchronized (this.inputMutex){
                this.inputMutex.notify();
            }
        }

        public void setHandlerState(PeerState newState){
            this.currentState = newState;
        }

        public void handleMessage(ObjectInputStream input, ObjectOutputStream output){
            this.currentState.handleMessage(this, this.peer, input, output);
        }

        @Override
        public void run() {
            while(true){
                synchronized (mutex){
                    try{
                        // wait for a output message
                        System.out.println("Waiting for output");
                        mutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }

                    System.out.println("Waiting done! Handling message");
                    // send that message on ObjectOutputStream
                    handleMessage(in, out);
                }
            }
        }

    }

    public static class NeighbourInputHandler extends Handler implements Runnable{
        public Socket connection;
        public ObjectOutputStream out;
        public ObjectInputStream in;
        public PeerState currentState;
        public PeerInfo peer;
        public Object mutex;

        public NeighbourInputHandler(Socket connection, PeerInfo peer, ObjectOutputStream out, ObjectInputStream in, PeerState currentState, Object mutex){
            this.connection = connection;
            this.currentState = currentState;
            this.peer = peer;
            this.out = out;
            this.in = in;
            this.mutex = mutex;
        }

        public void setState(PeerState newState){
            this.currentState = newState;
        }

        public void handleMessage(ObjectInputStream input, ObjectOutputStream output){
            this.currentState.handleMessage(this, this.peer, input, output);
        }

        @Override
        public void run() {
            while(true){
                synchronized (mutex){
                    try{
                        // wait for a output message
                        System.out.println("Waiting for input");
                        mutex.wait();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                        break;
                    }

                    System.out.println("Waiting done! Waiting for input");
                    // send that message on ObjectOutputStream
                    handleMessage(in, out);
                }

            }
        }
    }
}
