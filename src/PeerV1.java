import java.io.*;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class PeerV1 {
    private static final String CONFIG_DIR = "config";
    private static final String COMMON_CONFIGURATION_FILE = "Common.cfg";
    private static final String PEER_INFO_CONFIGURATION_FILE = "PeerInfo.cfg";
    private static int numOfPreferredNeighbours;
    private static int unchokingInterval;
    private static int optimisticUnchokingInterval;
    private static String fileName;
    private static long fileSize;
    private static long pieceSize;

    private static int peerID;
    private static String hostName;
    private static int portNumber;
    private static boolean hasFile;

    private static BitSet bitField;
    private static List<byte[]> fileChunks;

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
    }

    private static void parseCommonConfigFile() {
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

    private static void parsePeerInfoConfigFile(int peerID) {
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
}
