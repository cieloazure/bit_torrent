import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

class Handler {
    protected DataInputStream inputStream;
    protected DataOutputStream outputStream;
    protected SelfPeerInfo myPeerInfo;
    protected AtomicReference<PeerState> inputStateRef;
    protected BlockingQueue<PeerState> outputStateRef;
    protected Object inputMutex;
    protected Object outputMutex;
    protected int whichHandler;
    protected int theirPeerId;

    public Handler(SelfPeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, BlockingQueue<PeerState> outputMessageQueue, DataInputStream inputStream, DataOutputStream outputStream, Object inputMutex, Object outputMutex, int theirPeerId) {
        this.myPeerInfo = myPeerInfo;
        this.inputStateRef = inputStateRef;
        this.outputStateRef = outputMessageQueue;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.inputMutex = inputMutex;
        this.outputMutex = outputMutex;
        this.theirPeerId = theirPeerId;
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

    public int getTheirPeerId() {
        return this.theirPeerId;
    }

    public void setWhichHandler(int whichHandler) {
        this.whichHandler = whichHandler;
    }

    public NeighbourInputHandler getInputHandler() {
        NeighbourInputHandler nih = new NeighbourInputHandler(this.myPeerInfo, this.inputStateRef, this.outputStateRef, this.inputStream, this.outputStream, this.inputMutex, this.outputMutex, this.theirPeerId);
        nih.setWhichHandler(1);
        return nih;
    }

    public NeighbourOutputHandler getOutputHandler() {
        NeighbourOutputHandler noh = new NeighbourOutputHandler(this.myPeerInfo, this.inputStateRef, this.outputStateRef, this.inputStream, this.outputStream, this.inputMutex, this.outputMutex, this.theirPeerId);
        noh.setWhichHandler(0);
        return noh;
    }
}