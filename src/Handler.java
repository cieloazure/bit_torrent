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
    protected PeerInfo myPeerInfo;
    protected AtomicReference<PeerState> inputStateRef;
    protected AtomicReference<PeerState> outputStateRef;
    protected Object inputMutex;
    protected Object outputMutex;
    protected int whichHandler;

    public Handler(Socket connection, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, AtomicReference<PeerState> outputStateRef, ObjectInputStream inputStream, ObjectOutputStream outputStream, Object inputMutex, Object outputMutex){
        this.connection = connection;
        this.myPeerInfo = myPeerInfo;
        this.inputStateRef = inputStateRef;
        this.outputStateRef = outputStateRef;
        this.inputStream = inputStream;
        this.outputStream = outputStream;
        this.inputMutex = inputMutex;
        this.outputMutex = outputMutex;
    }

    public void setState(PeerState whichState, boolean isForInputState){
        if(isForInputState){
            inputStateRef.set(whichState);
            outputStateRef.set(null);
            synchronized (inputMutex){
                inputMutex.notifyAll();
            }
        }else{
            outputStateRef.set(whichState);
            inputStateRef.set(null);
            synchronized (outputMutex){
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

    public void setWhichHandler(int whichHandler) {
        this.whichHandler = whichHandler;
    }

    public NeighbourInputHandler getInputHandler(){
        NeighbourInputHandler nih = new NeighbourInputHandler(this.connection, this.myPeerInfo, this.inputStateRef, this.outputStateRef, this.inputStream, this.outputStream, this.inputMutex, this.outputMutex);
        nih.setWhichHandler(1);
        return nih;
    }

    public NeighbourOutputHandler getOutputHandler(){
        NeighbourOutputHandler noh = new NeighbourOutputHandler(this.connection, this.myPeerInfo, this.inputStateRef, this.outputStateRef, this.inputStream, this.outputStream, this.inputMutex, this.outputMutex);
        noh.setWhichHandler(0);
        return noh;
    }
}