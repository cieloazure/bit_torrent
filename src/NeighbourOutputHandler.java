import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class NeighbourOutputHandler extends Handler implements Runnable{
    public NeighbourOutputHandler(Socket connection, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, AtomicReference<PeerState> outputStateRef, ObjectInputStream inputStream, ObjectOutputStream outputStream, Object inputMutex, Object outputMutex) {
        super(connection, myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex);
    }

    @Override
    public void run(){

        while(true){
            try{
                synchronized (this.outputMutex){
                    while(this.outputStateRef.get() == null){
                        System.out.println("Waiting on output");
                        this.outputMutex.wait();
                    }
                    System.out.println("Waiting on output...done!");
                    PeerState outputState = this.outputStateRef.get();
                    outputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}