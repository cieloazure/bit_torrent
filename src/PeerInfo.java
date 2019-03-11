import java.util.BitSet;

public class PeerInfo {

    public  int peerID;
    public  String hostName;

    public  int portNumber;
    public  boolean hasFile;
    public  BitSet bitField;

    public PeerInfo(int peerID, String hostName, int portNumber, boolean hasFile, BitSet bitField){
        this.peerID = peerID;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.hasFile = hasFile;
        this.bitField = bitField;
    }
}
