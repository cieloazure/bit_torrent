import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class Handler{
    protected Socket connection;
    protected ObjectInputStream inputStream;
    protected ObjectOutputStream outputStream;
    protected Lock peerInputLock;
    protected Lock peerOutputLock;
    protected PeerInfo myPeerInfo;
    protected AtomicReference<PeerState> inputStateRef;
    protected Condition inputStateIsNotNull;
    protected AtomicReference<PeerState> outputStateRef;
    protected Condition outputStateIsNotNull;
    protected int whichHandler;

    public Handler(Socket connection, Lock peerInputLock, Lock peerOutputLock, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, Condition inputStateIsNotNull, AtomicReference<PeerState> outputStateRef, Condition outputStateIsNotNull, ObjectInputStream inputStream, ObjectOutputStream outputStream){
        this.connection = connection;
        this.peerInputLock = peerInputLock;
        this.peerOutputLock = peerOutputLock;
        this.myPeerInfo = myPeerInfo;
        this.inputStateRef = inputStateRef;
        this.inputStateIsNotNull = inputStateIsNotNull;
        this.outputStateRef = outputStateRef;
        this.outputStateIsNotNull = outputStateIsNotNull;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
    }

    public void setState(PeerState whichState, boolean isForInputState){
        if(isForInputState){
            inputStateRef.set(whichState);
            outputStateRef.set(null);
            try{
                this.peerInputLock.lock();
                inputStateIsNotNull.signalAll();
            }finally {
                this.peerInputLock.unlock();
            }
        }else{
            outputStateRef.set(whichState);
            inputStateRef.set(null);
            try{
                this.peerOutputLock.lock();
                outputStateIsNotNull.signalAll();
            }finally {
                this.peerOutputLock.unlock();
            }
        }
    }

    public String getHostName() {
        return this.connection.getInetAddress().getHostName();
    }

    public int getPortNumber(){
        return this.connection.getPort();
    }

    public void setWhichHandler(int whichHandler) {
        this.whichHandler = whichHandler;
    }

    public NeighbourInputHandler getInputHandler(){
        NeighbourInputHandler nih = new NeighbourInputHandler(this.connection, this.peerInputLock,this.peerOutputLock, this.myPeerInfo, this.inputStateRef, this.inputStateIsNotNull, this.outputStateRef, this.outputStateIsNotNull, this.inputStream, this.outputStream);
        nih.setWhichHandler(1);
        return nih;
    }

    public NeighbourOutputHandler getOutputHandler(){
        NeighbourOutputHandler noh = new NeighbourOutputHandler(this.connection, this.peerOutputLock,this.peerOutputLock, this.myPeerInfo, this.inputStateRef, this.inputStateIsNotNull, this.outputStateRef, this.outputStateIsNotNull, this.inputStream, this.outputStream);
        noh.setWhichHandler(0);
        return noh;
    }
}