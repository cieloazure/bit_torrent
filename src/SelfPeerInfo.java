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
    protected BitSet requestedPieces; //to track the requested pieces

    public SelfPeerInfo(PeerInfo.Builder b, Boolean hasFile, List<byte[]> fileChunks, Logger logger, ArrayList<Integer> peerAddressToID, BitSet requestedPieces) {
        super(b);
        this.hasFile = hasFile;
        this.fileChunks = fileChunks;
        this.logger = logger;
        this.peerAddressToID = peerAddressToID;
        this.requestedPieces = requestedPieces;
    }

    public void setRequestedPieces(BitSet requestedPieces) {
        this.requestedPieces = requestedPieces;
    }

    public BitSet getRequestedPieces() {
        return this.requestedPieces;
    }

    public void setRequestPiecesIndex(int index) {
        this.requestedPieces.set(index);
    }

    public Logger getLogger() {
        return logger;
    }

    public byte[] getFileChunk(int index) {
        return fileChunks.get(index);
    }
}
