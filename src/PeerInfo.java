import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.logging.Logger;

public class PeerInfo {

    protected String hostName;
    protected Integer portNumber;
    protected Integer peerID;
    protected BitSet bitField;
    protected BitSet requestedPieces; //to track the requested pieces

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
        this.requestedPieces = b.requestedPieces;
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

    public void setRequestedPieces(BitSet requestedPieces) {
        this.requestedPieces = requestedPieces;
    }

    public BitSet getRequestedPieces() {
        return this.requestedPieces;
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

    public void setRequestPiecesIndex(int index) {
        this.requestedPieces.set(index);
    }

    public static class Builder {
        private String hostName;
        private Integer portNumber;
        private Integer peerID;
        private BitSet bitField;
        protected BitSet requestedPieces;
        private Boolean hasFile;
        private List<byte[]> fileChunks;
        private Logger logger;
        private ArrayList<Integer> peerAddressToID;
        private Handler context;

        public Builder() {

        }

        public Builder withHandlerContext(Handler context) {
            this.context = context;
            return this;
        }

        public Builder withAddressToIDList(ArrayList<Integer> peerAddressToID) {
            this.peerAddressToID = peerAddressToID;
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

        public Builder withBitField(BitSet bitField, BitSet requestedPieces) {
            this.bitField = bitField;
            //set the indexes corresponding of existing pieces in requestedPiece to true
            this.requestedPieces = (BitSet)this.bitField.clone();
            return this;
        }

        public Builder withHasFile(Boolean hasFile) {
            this.hasFile = hasFile;
            return this;
        }

        public Builder withFileChunks(List<byte[]> fileChunks) {
            this.fileChunks = fileChunks;
            return this;
        }

        public SelfPeerInfo buildSelfPeerInfo() {
            return new SelfPeerInfo(this, this.hasFile, this.fileChunks, this.logger, this.peerAddressToID);
        }

        public NeighbourPeerInfo buildNeighbourPeerInfo() {
            return new NeighbourPeerInfo(this, this.context);
        }
    }
}
