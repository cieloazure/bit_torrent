import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

public class PeerInfo {

    private  String hostName;
    private  Integer portNumber;
    private  Integer peerID;
    private  Boolean hasFile;
    private  BitSet bitField;
    private  List<byte[]> fileChunks;
    private Lock inputLock;
    private Lock outputLock;
    private AtomicReference<PeerState> inputStateRef;
    private AtomicReference<PeerState> outputStateRef;
    private Condition inputStateIsNotNull;
    private Condition outputStateIsNotNull;
    private Socket connection;
    private ObjectInputStream inputStream;
    private ObjectOutputStream outputStream;



    public static class Builder{
        private  String hostName;
        private  Integer portNumber;
        private  Integer peerID;
        private  Boolean hasFile;
        private  BitSet bitField;
        private  List<byte[]> fileChunks;
        private Lock inputLock;
        private Lock outputLock;
        private AtomicReference<PeerState> inputStateRef;
        private AtomicReference<PeerState> outputStateRef;
        private Condition inputStateIsNotNull;
        private Condition outputStateIsNotNull;
        private Socket connection;
        private ObjectInputStream inputStream;
        private ObjectOutputStream outputStream;



        public Builder(){
        }

        public Builder(String hostName, Integer portNumber){
            this.hostName = hostName;
            this.portNumber = portNumber;
        }


        public Builder withInputHandlerVars(Lock inputLock, AtomicReference<PeerState> inputStateRef, Condition inputStateIsNotNull){
            this.inputLock = inputLock;
            this.inputStateRef = inputStateRef;
            this.inputStateIsNotNull = inputStateIsNotNull;
            return this;
        }

        public Builder withOutputHandlerVars(Lock outputLock, AtomicReference<PeerState> outputStateRef, Condition outputStateIsNotNull){
            this.outputLock = outputLock;
            this.outputStateRef = outputStateRef;
            this.outputStateIsNotNull = outputStateIsNotNull;
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

    public byte[] getBitFieldByteArray(int defaultPieces){
        byte[] array = this.bitField.toByteArray();
        System.out.println("bitfield size: "+array.length);
        if(array.length == 0){
            int length = (defaultPieces + 7)/8;
            byte[] newArray = new byte[length];
            for(int i = 0; i < length; i++){
                newArray[i] = (int)0;
            }
            System.out.println("default bitfield size: " + newArray.length);
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

    public void print(){
        System.out.println("PeerInfo object" + this);
    }
}
