import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class PeerInfo {

    private  String hostName;
    private  Integer portNumber;
    private  Integer peerID;
    private  Boolean hasFile;
    private  BitSet bitField;
    private  List<byte[]> fileChunks;
    private AtomicReference<PeerState> inputStateRef;
    private BlockingQueue<PeerState> outputStateQueue;
    private Socket connection;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;
    private Object inputMutex;
    private Object outputMutex;
    private Double downloadingSpeed;
    private Logger logger;
    private ArrayList<Integer> peerAddressToID;

    public static class Builder{
        private  String hostName;
        private  Integer portNumber;
        private  Integer peerID;
        private  Boolean hasFile;
        private  BitSet bitField;
        private  List<byte[]> fileChunks;
        private AtomicReference<PeerState> inputStateRef;
        private BlockingQueue<PeerState> outputStateQueue;
        private Socket connection;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;
        private Object inputMutex;
        private Object outputMutex;
        private Logger logger;
        private ArrayList<Integer> peerAddressToID;

        public Builder(){
        }

        public Builder(String hostName, Integer portNumber){
            this.hostName = hostName;
            this.portNumber = portNumber;
        }


        public Builder withInputHandlerVars(Object inputMutex, AtomicReference<PeerState> inputStateRef){
            this.inputStateRef = inputStateRef;
            this.inputMutex = inputMutex;
            return this;
        }

        public Builder withOutputHandlerVars(Object outputMutex, BlockingQueue<PeerState> outputStateQueue){
            this.outputMutex = outputMutex;
            this.outputStateQueue = outputStateQueue;
            return this;
        }

        public Builder withSocketAndItsStreams(Socket connection, ObjectInputStream inputStream, ObjectOutputStream outputStream){
            this.connection = connection;
            this.inputStream = inputStream;
            this.outputStream = outputStream;
            return this;
        }

        public Builder withHostNameAndPortNumber(String hostName, Integer portNumber){
            this.hostName = hostName;
            this.portNumber = portNumber;
            return this;
        }

        public Builder withPeerID(Integer peerID){
            this.peerID = peerID;
            return this;
        }

        public Builder withHasFile(Boolean hasFile){
            this.hasFile = hasFile;
            return this;
        }

        public Builder withBitFieldAndFileChunks(BitSet bitField, List<byte[]> fileChunks){
            this.bitField = bitField;
            this.fileChunks = fileChunks;
            return this;
        }
        public Builder withLogger(Logger logger){
            this.logger = logger;
            return this;
        }
        public Builder withAddressToIDList(ArrayList<Integer> peerAddressToID){
            this.peerAddressToID = peerAddressToID;
            return this;
        }

        public PeerInfo build(){
           return new PeerInfo(this);
        }
    }

    private PeerInfo(Builder b){
        this.hostName = b.hostName;
        this.portNumber = b.portNumber;
        this.peerID = b.peerID;
        this.hasFile = b.hasFile;
        this.bitField = b.bitField;
        this.fileChunks = b.fileChunks;
        this.inputMutex = b.inputMutex;
        this.inputStateRef = b.inputStateRef;
        this.outputMutex = b.outputMutex;
        this.outputStateQueue = b.outputStateQueue;
        this.connection = b.connection;
        this.inputStream = b.inputStream;
        this.outputStream = b.outputStream;
        this.downloadingSpeed = 0.0;
        this.logger = b.logger;
        this.peerAddressToID = b.peerAddressToID;
    }

    public Integer getPeerID() {
        return peerID;
    }

    public void setPeerID(Integer peerID) {
        this.peerID = peerID;
    }


    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public Integer getPortNumber() {
        return portNumber;
    }

    public void setPortNumber(Integer portNumber) {
        this.portNumber = portNumber;
    }

    public Boolean hasFile() {
        return hasFile;
    }

    public void setHasFile(Boolean hasFile) {
        this.hasFile = hasFile;
    }

    public BitSet getBitField() {
        return bitField;
    }

    public Logger getLogger(){return logger;}
    public ArrayList<Integer> getAddressToIDHash(){
        return peerAddressToID;
    }
    public byte[] getBitFieldByteArray(int defaultPieces){
        byte[] array = this.bitField.toByteArray();
        if(array.length == 0){
            int length = (defaultPieces + 7)/8;
            byte[] newArray = new byte[length];
            for(int i = 0; i < length; i++){
                newArray[i] = (int)0;
            }
            return newArray;
        }
        return array;
    }

    public void setBitField(BitSet bitField) {
        this.bitField = bitField;
    }

    public List<byte[]> getFileChunks() {
        return fileChunks;
    }

    public void setFileChunks(List<byte[]> fileChunks) {
        this.fileChunks = fileChunks;
    }

    public Double getDownloadingSpeed() {
        return downloadingSpeed;
    }

}
