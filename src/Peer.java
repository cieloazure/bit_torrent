import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
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

    public static void main(String[] args) {
        Logger logger = null;
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
        logger = setUpLogger(peerID, logger);
        peerInfoBuilder.withLogger(logger);
        // Parse peer info file
        parsePeerInfoConfigFile(peerID, commonConfig, peerInfoBuilder);
        buildAddressToPeerIDHash(peerID, peerInfoBuilder);
        // Build myPeerInfo object
        myPeerInfo = peerInfoBuilder.buildSelfPeerInfo();

        // Peer connection object to start listening for new connections, send a message to any connection or create a connection to any peer
        PeerConnection connection = new PeerConnection(myPeerInfo);

        // Start listening for any new connections
        connection.startListenerThread();

        // Connect to peers in PeerInfo.cfg which appear above the current line by parsing the peer info config file again
        parsePeerInfoConfigToMakeConnections(connection);
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

            while (linePeerId != myPeerInfo.getPeerID()) {
                System.out.println("adding log entry");
                myPeerInfo.getLogger().info("Peer [peerID " + myPeerInfo.getPeerID() + "] makes a connection to Peer[peer_ID " + linePeerId + "]");
                // Make a connection with the peer
                Socket newConnection = new Socket(neighbourHostName, neighbourPortNumber);
                System.out.println("[PEER:" + myPeerInfo.getPeerID() + "]Connecting to a peer " + linePeerId + "....");
                // Spawn handlers for the new connection
                connection.handleNewConnection(newConnection, true);

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
            builder.withHasFile(hasFile);


            int pieces = (int) Math.ceil(commonConfig.getFileSize() / commonConfig.getPieceSize());
            BitSet bitField = new BitSet(pieces);
            if (hasFile) {
                for (int i = 0; i < pieces; i++) {
                    bitField.set(i);
                }
                System.out.println("Tushar debug");
                System.out.println(commonConfig.getFileName());
                List<byte[]> fileChunks = splitFileIntoChunks(commonConfig.getFileName(), commonConfig.getFileSize(), commonConfig.getPieceSize());
                builder.withBitField(bitField)
                        .withFileChunks(fileChunks);
            } else {
                builder.withBitField(bitField)
                        .withFileChunks(null);
            }
        } catch (FileNotFoundException e) {
            System.err.println("[ERROR]: Peer Info configuration file not found");
            e.printStackTrace();
        } catch (IOException e) {
            System.err.println("[ERROR]: Error parsing Peer Info configuration file");
            e.printStackTrace();
        }
    }

    private static List<byte[]> splitFileIntoChunks(String fileName, long fileSize, long pieceSize) {
        List<byte[]> chunks = new ArrayList<>();
        try {
            File f = new File(fileName);
            FileInputStream fis = new FileInputStream(f);
            int pieces = (int) Math.ceil(fileSize / pieceSize);
            for (int i = 0; i < pieces; i++) {
                byte[] buffer = new byte[(int) pieceSize];
                while (fis.read(buffer) > 0) {
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

    /**
     * Function to setup the peer specific logger
     *
     * @param peerID
     * @return
     */
    private static Logger setUpLogger(int peerID, Logger logger) {
        try {
            FileHandler fh;
            System.setProperty("java.util.logging.SimpleFormatter.format",
                    "[%1$tF %1$tT] %5$s %n");
            SimpleFormatter formatter = new SimpleFormatter();
            logger = Logger.getLogger("log_peer_" + peerID);
            logger.setUseParentHandlers(false);
            fh = new FileHandler("logs/log_peer_" + peerID + ".log");
            logger.addHandler(fh);
            fh.setFormatter(formatter);
            System.out.println("Setup log");

        } catch (Exception e) {
            e.printStackTrace();
        }
        return logger;
    }

    private static void buildAddressToPeerIDHash(int ownerPeerID, PeerInfo.Builder builder) {
        try {
            BufferedReader in = new BufferedReader(new FileReader(CONFIG_DIR + "/" + PEER_INFO_CONFIGURATION_FILE));
            ArrayList<Integer> addressToID = new ArrayList<>();
            String line = in.readLine();
            int linePeerID = Integer.parseInt(line.split(" ")[0]);
            while (true) {
                if (linePeerID != ownerPeerID) {
                    String[] splitLine = line.split(" ");
                    addressToID.add(Integer.parseInt(splitLine[0]));
                    // read next line

                }
                line = in.readLine();
                if (line == null) {
                    break;
                } else {
                    linePeerID = Integer.parseInt(line.split(" ")[0]);
                }

            }
            builder.withAddressToIDList(addressToID);

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
