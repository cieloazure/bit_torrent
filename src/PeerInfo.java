import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

public class PeerInfo {

    protected String hostName;
    protected Integer portNumber;
    protected Integer peerID;
    protected BitSet bitField;

    public PeerInfo(String hostName, Integer portNumber, Integer peerID, BitSet bitField) {
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.peerID = peerID;
        this.bitField = bitField;
    }

    public PeerInfo(Builder b) {
        this.hostName = b.hostName;
        this.portNumber = b.portNumber;
        this.peerID = b.peerID;
        this.bitField = b.bitField;
    }

    public Integer getPeerID() {
        return peerID;
    }

    public String getHostName() {
        return hostName;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public BitSet getBitField() {
        return bitField;
    }

    public void setBitField(BitSet bitField) {
        this.bitField = bitField;
    }

    public byte[] getBitFieldByteArray(int defaultPieces) {
        byte[] array = this.bitField.toByteArray();
        if (array.length == 0) {
            int length = (defaultPieces + 7) / 8;
            byte[] newArray = new byte[length];
            for (int i = 0; i < length; i++) {
                newArray[i] = (int) 0;
            }
            return newArray;
        }
        return array;
    }

    public void setBitFieldIndex(int index) {
        this.bitField.set(index);
    }

    public static class Builder {
        private String hostName;
        private Integer portNumber;
        private Integer peerID;
        private BitSet bitField;
        private Boolean hasFile;
        protected BitSet requestedPieces;
        private Map<Integer, byte[]> fileChunks;
        private Logger logger;
        private ArrayList<Integer> peerAddressToID;
        private Handler context;

        public Builder() {

        }

        public Builder withHandlerContext(Handler context) {
            this.context = context;
            return this;
        }

        public Builder withLogger(Logger logger) {
            this.logger = logger;
            return this;
        }

        public Builder withHostName(String hostName) {
            this.hostName = hostName;
            return this;
        }

        public Builder withPortNumber(Integer portNumber) {
            this.portNumber = portNumber;
            return this;
        }

        public Builder withPeerID(Integer peerID) {
            this.peerID = peerID;
            return this;
        }

        public Builder withBitField(BitSet bitField) {
            this.bitField = bitField;
            this.requestedPieces = (BitSet)this.bitField.clone();
            return this;
        }

        public Builder withHasFile(Boolean hasFile) {
            this.hasFile = hasFile;
            return this;
        }

        public Builder withFileChunks(Map<Integer, byte[]> fileChunks) {
            this.fileChunks = fileChunks;
            return this;
        }

        public SelfPeerInfo buildSelfPeerInfo() {
            return new SelfPeerInfo(this, this.hasFile, this.fileChunks, this.logger, this.requestedPieces);
        }

        public NeighbourPeerInfo buildNeighbourPeerInfo() {
            return new NeighbourPeerInfo(this, this.context);
        }
    }
}
