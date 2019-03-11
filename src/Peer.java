import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.BitSet;
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
            makeConnections(peerID, neighbourConnections);
        }
    }

    public static void makeConnections(int peerID, List<Socket> neighbourConnections){
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
                System.out.println("Connected with "+neighbourHostName + " at " + neighbourPortNumber);
                Socket conn = new Socket(neighbourHostName, neighbourPortNumber);
                neighbourConnections.add(conn);

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
                    System.out.println("Listening for conenctions....at "+ peer.hostName + ":" + peer.portNumber);
                    new Thread(new NeighbourInputHandler(listener.accept(), this.peer)).start();
                    System.out.println("Got a peer connection! Spawning NeighbourInputHandler for a peer");
                }
            }catch(IOException e){
                System.out.println(e.getMessage());
            }
        }
    }

    public static class NeighbourInputHandler implements Runnable{
        public Socket connection;
        public ObjectOutputStream out;
        public ObjectInputStream in;
        public PeerState currentState;
        public PeerInfo peer;

        public NeighbourInputHandler(Socket connection, PeerInfo peer){
            this.connection = connection;
            this.currentState = new WaitForHandshakeMessageState();
            this.peer = peer;
        }

        public void setState(PeerState newState){
            this.currentState = newState;
        }

        public void handleMessage(ObjectInputStream input, ObjectOutputStream output){
            this.currentState.handleMessage(this, this.peer, input, output);
        }

        @Override
        public void run() {
            try {
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                while(true){
                    handleMessage(in, out);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
