import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class Handler {
    protected Socket connection;
    protected DataInputStream inputStream;
    protected DataOutputStream outputStream;
    protected SelfPeerInfo myPeerInfo;
    protected AtomicReference<PeerState> inputStateRef;
    protected AtomicReference<PeerState> lastStateRef;
    protected BlockingQueue<PeerState> outputStateRef;
    protected Object inputMutex;
    protected Object outputMutex;
    protected int whichHandler;
    protected int theirPeerId;

    public Handler(Socket connection,
                   SelfPeerInfo myPeerInfo,
                   AtomicReference<PeerState> inputStateRef,
                   BlockingQueue<PeerState> outputMessageQueue,
                   DataInputStream inputStream, DataOutputStream outputStream,
                   Object inputMutex, Object outputMutex,
                   int theirPeerId) {
        this.connection = connection;
        this.myPeerInfo = myPeerInfo;
        this.inputStateRef = inputStateRef;
        this.outputStateRef = outputMessageQueue;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.inputMutex = inputMutex;
        this.outputMutex = outputMutex;
        this.theirPeerId = theirPeerId;
        this.lastStateRef = new AtomicReference<>();
    }

    public void setState(PeerState whichState, boolean isForInputState, boolean setOtherStateAsNull) {
        if (isForInputState) {
            synchronized (inputMutex) {
                inputStateRef.set(whichState);
                if (setOtherStateAsNull) {
                    outputStateRef.clear();
                }
                inputMutex.notifyAll();
            }
        } else {
            synchronized (outputMutex) {
                if (whichState != null) {
                    outputStateRef.offer(whichState);
                } else {
                    outputStateRef.clear();
                }
                if (setOtherStateAsNull) {
                    inputStateRef.set(null);
                }
                outputMutex.notifyAll();
            }
        }
    }

    public PeerState getLastStateRef() {
        return this.lastStateRef.get();
    }

    public void setLastStateRef(PeerState whichState) {
        this.lastStateRef.set(whichState);
    }

    public String getHostName() {
        return this.connection.getInetAddress().getHostName();
    }

    public int getPortNumber() {
        return this.connection.getPort();
    }

    public int getTheirPeerId() {
        return this.theirPeerId;
    }

    public void setWhichHandler(int whichHandler) {
        this.whichHandler = whichHandler;
    }

    public NeighbourInputHandler getInputHandler(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo) {
        NeighbourInputHandler nih = new NeighbourInputHandler(this.connection, this.myPeerInfo,
                this.inputStateRef, this.outputStateRef,
                this.inputStream, this.outputStream,
                this.inputMutex, this.outputMutex,
                this.theirPeerId, neighbourInfo);
        nih.setWhichHandler(1);
        return nih;
    }

    public NeighbourOutputHandler getOutputHandler(ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo) {
        NeighbourOutputHandler noh = new NeighbourOutputHandler(this.connection, this.myPeerInfo,
                this.inputStateRef, this.outputStateRef,
                this.inputStream, this.outputStream,
                this.inputMutex, this.outputMutex,
                this.theirPeerId, neighbourInfo);
        noh.setWhichHandler(0);
        return noh;
    }

    public void closeConnection() {
        try {
            this.connection.close();
        } catch (Exception e) {

        }

    }
}