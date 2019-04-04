import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;

public class SelfPeerInfo extends PeerInfo {
    private Boolean hasFile;
    private List<byte[]> fileChunks;
    private BitSet requestedbitField;
    private Logger logger;
    private ArrayList<Integer> peerAddressToID;

    public SelfPeerInfo(PeerInfo.Builder b, Boolean hasFile, List<byte[]> fileChunks, Logger logger, ArrayList<Integer> peerAddressToID) {
        super(b);
        this.hasFile = hasFile;
        this.fileChunks = fileChunks;
        this.logger = logger;
        this.peerAddressToID = peerAddressToID;
    }

    public Logger getLogger() {
        return logger;
    }

    public byte[] getFileChunk(int index) {
        return fileChunks.get(index);
    }
}
