import java.io.*;
import java.net.Socket;
import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

public class Peer {
    private static final String CONFIG_DIR = "config";
    private static final String COMMON_CONFIGURATION_FILE = "Common.cfg";
    private static final String PEER_INFO_CONFIGURATION_FILE = "PeerInfo.cfg";

    /* Configuration variables */
    private static CommonConfig commonConfig;

    /* Self peer info variables */
    private static SelfPeerInfo myPeerInfo;

    public static void start(int peerID) {
        Logger logger = null;
        // Parse common config file
        CommonConfig.Builder configBuilder = new CommonConfig.Builder();
        parseCommonConfigFile(configBuilder);
        commonConfig = configBuilder.build();


        PeerInfo.Builder peerInfoBuilder = new PeerInfo.Builder();
        peerInfoBuilder.withCommonConfig(commonConfig);

        // Set peer ID
        // TODO: Check for NAN exception, terminate program in that case
        peerInfoBuilder.withPeerID(peerID);

        //Setting up the logger
        logger = setUpLogger(peerID, logger);
        peerInfoBuilder.withLogger(logger);

        // Parse peer info file
        parsePeerInfoConfigFile(peerID, commonConfig, peerInfoBuilder);

        // Build myPeerInfo object
        myPeerInfo = peerInfoBuilder.buildSelfPeerInfo();
        System.out.println("Requested pieces set to");
        System.out.println(myPeerInfo.getRequestedPieces());
        // Enable std output logging
        myPeerInfo.enableStdOutputLogging();

        // Peer connection object to start listening for new connections, send a message to any connection or create a connection to any peer
        PeerConnection connection = new PeerConnection(myPeerInfo);

        // Initialize the concurrent hash map from file
        connection.initializeNeighbourConnectionsInfo(CONFIG_DIR + '/' + PEER_INFO_CONFIGURATION_FILE);

        // Start listening for any new connections
        connection.startListenerThread();

        // Connect to peers in PeerInfo.cfg which appear above the current line by parsing the peer info config file again
        parsePeerInfoConfigToMakeConnections(connection);

        connection.startScheduledExecution(commonConfig.getUnchokingInterval(),
                commonConfig.getOptimisticUnchokingInterval(),
                commonConfig.getNumOfPreferredNeighbours());

    }

    private static void parsePeerInfoConfigToMakeConnections(PeerConnection connection) {
        try {

            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + PEER_INFO_CONFIGURATION_FILE));
            // read line
            String peerInfoFileLine = in.readLine();

            // split line
            String[] splitLine = peerInfoFileLine.split(" ");

            // Get variables from the line
            int linePeerId = Integer.parseInt(splitLine[0]);
            String neighbourHostName = splitLine[1];
            int neighbourPortNumber = Integer.parseInt(splitLine[2]);

            System.out.println("Calling the parsePeerInfoConfigToMakeConnections method with linePeerId " + linePeerId);
            while (linePeerId != myPeerInfo.getPeerID()) {
                myPeerInfo.log("linePeerId:myPeerInfo.getPeerID() " + linePeerId + " : " + myPeerInfo.getPeerID());
                // Log previous state
                myPeerInfo.log("[PEER:" + myPeerInfo.getPeerID() + "]Connecting to a peer " + linePeerId + "....");

                // Make a connection with the peer
                Socket newConnection = new Socket(neighbourHostName, neighbourPortNumber);
                newConnection.setReceiveBufferSize(myPeerInfo.getCommonConfig().RECEIVE_BUFFER_SIZE);

                // Log connection
                myPeerInfo.log("Peer [peerID " + myPeerInfo.getPeerID() + "] makes a connection to Peer[peer_ID " + linePeerId + "]");

                // Get input and output streams of the socket
                // !IMPORTANT NOTE!
                // Output stream needs to be created before input stream
                // Output stream needs to flushed to write the headers over the wire
                DataOutputStream outputStream = new DataOutputStream(newConnection.getOutputStream());
                outputStream.flush();
                DataInputStream inputStream = new DataInputStream(newConnection.getInputStream());

                // Write the peer id for connection
                outputStream.writeInt(myPeerInfo.getPeerID());

                // Spawn handlers for the new connection
                connection.handleNewConnection(newConnection, true, inputStream, outputStream, linePeerId);

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

    private static void parseCommonConfigFile(CommonConfig.Builder configBuilder) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + COMMON_CONFIGURATION_FILE));
            int numOfPreferredNeighbours = Integer.parseInt(in.readLine().split(" ")[1]);
            int unchokingInterval = Integer.parseInt(in.readLine().split(" ")[1]);
            int optimisticUnchokingInterval = Integer.parseInt(in.readLine().split(" ")[1]);

            String fileName = in.readLine().split(" ")[1].trim();
            long fileSize = Long.parseLong(in.readLine().split(" ")[1]);
            long pieceSize = Long.parseLong(in.readLine().split(" ")[1]);

            configBuilder
                    .withNumOfPreferredNeighboursAs(numOfPreferredNeighbours)
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
        try {
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + PEER_INFO_CONFIGURATION_FILE));
            String peerInfoFileLine = in.readLine();
            int linePeerId = Integer.parseInt(peerInfoFileLine.split(" ")[0]);
            while (linePeerId != peerID) {
                peerInfoFileLine = in.readLine();
                linePeerId = Integer.parseInt(peerInfoFileLine.split(" ")[0]);
            }
            String[] splitLine = peerInfoFileLine.split(" ");

            String hostName = splitLine[1];
            int portNumber = Integer.parseInt(splitLine[2]);
            builder.withHostName(hostName)
                    .withPortNumber(portNumber);

            boolean hasFile = Integer.parseInt(splitLine[3]) == 1;
            if (hasFile) {
                System.out.println("I have the file!");
            } else {
                System.out.println("I DO NOT have the file!");
            }

            builder.withHasFile(hasFile);


            int pieces = (int)Math.ceil(commonConfig.getFileSize() /(double)commonConfig.getPieceSize());
            BitSet bitField = new BitSet(pieces);
//            BitSet requestedPieces = new BitSet(pieces);
            if (hasFile) {
                for (int i = 0; i < pieces; i++) {
                    bitField.set(i);
                }
//                System.out.println("Tushar debug");
                System.out.println(commonConfig.getFileName());
                Map<Integer, byte[]> fileChunks = splitFileIntoChunks(commonConfig.getFileName(), commonConfig.getFileSize(), commonConfig.getPieceSize());
                builder.withBitField(bitField)
                        .withFileChunks(fileChunks);
            } else {
                builder.withBitField(bitField)
                        .withFileChunks(new HashMap<Integer, byte[]>());
            }

        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Peer Info configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Peer Info configuration file");
            e.printStackTrace();
        }
    }

    private static Map<Integer, byte[]> splitFileIntoChunks(String fileName, long fileSize, long pieceSize) {
        Map<Integer, byte[]> fileChunks = new HashMap<>();
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[(int) pieceSize];
            int i = 0;
            int bytesRead = 0;
            while ((bytesRead = fis.read(buffer)) > 0) {
                fileChunks.putIfAbsent(i, buffer);
                i++;
                fileSize -= bytesRead;
                if(fileSize > pieceSize){
                    buffer = new byte[(int)pieceSize];
                }else{
                    buffer = new byte[(int)fileSize];
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return fileChunks;
    }

    /**
     * Function to setup the peer specific logger
     *
     * @param peerID Peer ID of the peer whose logger we are setting up
     * @param logger Logger object to set up
     * @return logger
     */
    private static Logger setUpLogger(int peerID, Logger logger) {
        try {
            FileHandler fh;
            System.setProperty("java.util.logging.SimpleFormatter.format",
                    "[%1$tF %1$tT] %5$s %n");
            SimpleFormatter formatter = new SimpleFormatter();
            logger = Logger.getLogger("log_peer_" + peerID);
            logger.setUseParentHandlers(false);
            fh = new FileHandler("logs/log_peer_" + peerID + ".log", true);
            logger.addHandler(fh);
            fh.setFormatter(formatter);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return logger;
    }

}
