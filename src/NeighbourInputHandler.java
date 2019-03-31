import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class NeighbourInputHandler extends Handler implements Runnable{

    public NeighbourInputHandler(Socket connection, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, BlockingQueue<PeerState> outputStateQueue, ObjectInputStream inputStream, ObjectOutputStream outputStream, Object inputMutex, Object outputMutex) {
        super(connection, myPeerInfo, inputStateRef, outputStateQueue, inputStream, outputStream, inputMutex, outputMutex);
    }

    @Override
    public void run(){
        while(true){
            try{
                synchronized (this.inputMutex){
                    while(this.inputStateRef.get() == null){
                        this.inputMutex.wait();
                    }
                    PeerState inputState = inputStateRef.get();
                    inputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
