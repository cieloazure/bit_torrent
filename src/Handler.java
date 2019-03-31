import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

class Handler{
    protected Socket connection;
    protected ObjectInputStream inputStream;
    protected ObjectOutputStream outputStream;
    protected PeerInfo myPeerInfo;
    protected AtomicReference<PeerState> inputStateRef;
    protected BlockingQueue<PeerState> outputStateQueue;
    protected Object inputMutex;
    protected Object outputMutex;
    protected int whichHandler;
    protected volatile int theirPeerId;

    public Handler(Socket connection, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, BlockingQueue<PeerState> outputMessageQueue, ObjectInputStream inputStream, ObjectOutputStream outputStream, Object inputMutex, Object outputMutex){
        this.connection = connection;
        this.myPeerInfo = myPeerInfo;
        this.inputStateRef = inputStateRef;
        this.outputStateQueue = outputMessageQueue;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.inputMutex = inputMutex;
        this.outputMutex = outputMutex;
    }

    public void setState(PeerState whichState, boolean isForInputState){
        if(isForInputState){
            synchronized (inputMutex){
                inputStateRef.set(whichState);
                inputMutex.notifyAll();
            }
        }else{
            synchronized (outputMutex){
                outputStateQueue.offer(whichState);
                inputStateRef.set(null);
                outputMutex.notifyAll();
            }
        }
    }

    public String getHostName() {
        return this.connection.getInetAddress().getHostName();
    }

    public int getPortNumber(){
        return this.connection.getPort();
    }

    public int getTheirPeerId(){ return this.theirPeerId; }

    public void setTheirPeerId(int theirPeerId){ this.theirPeerId = theirPeerId; }

    public void setWhichHandler(int whichHandler) {
        this.whichHandler = whichHandler;
    }

    public NeighbourInputHandler getInputHandler(){
        NeighbourInputHandler nih = new NeighbourInputHandler(this.connection, this.myPeerInfo, this.inputStateRef, this.outputStateQueue, this.inputStream, this.outputStream, this.inputMutex, this.outputMutex);
        nih.setWhichHandler(1);
        return nih;
    }

    public NeighbourOutputHandler getOutputHandler(){
        NeighbourOutputHandler noh = new NeighbourOutputHandler(this.connection, this.myPeerInfo, this.inputStateRef, this.outputStateQueue, this.inputStream, this.outputStream, this.inputMutex, this.outputMutex);
        noh.setWhichHandler(0);
        return noh;
    }
}