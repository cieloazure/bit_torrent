import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class NeighbourOutputHandler extends Handler implements Runnable{
    public NeighbourOutputHandler(Socket connection, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, BlockingQueue<PeerState> outputStateQueue, ObjectInputStream inputStream, ObjectOutputStream outputStream, Object inputMutex, Object outputMutex) {
        super(connection, myPeerInfo, inputStateRef, outputStateQueue, inputStream, outputStream, inputMutex, outputMutex);
    }

    @Override
    public void run(){
        while(true){
            try{
                synchronized (this.outputMutex){
                    while(this.outputStateQueue.isEmpty()){
                        this.outputMutex.wait();
                    }
                    while(!this.outputStateQueue.isEmpty()){
                        PeerState outputState = this.outputStateQueue.poll();
                        outputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}