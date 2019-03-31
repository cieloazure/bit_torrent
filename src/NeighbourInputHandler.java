import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class NeighbourInputHandler extends Handler implements Runnable{

    public NeighbourInputHandler(Socket connection, Lock peerInputLock, Lock peerOutputLock, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, Condition inputStateIsNotNull, AtomicReference<PeerState> outputStateRef, Condition outputStateIsNotNull, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        super(connection, peerInputLock, peerOutputLock, myPeerInfo, inputStateRef, inputStateIsNotNull, outputStateRef, outputStateIsNotNull, inputStream, outputStream);
    }

    @Override
    public void run(){
        while(true){
            peerInputLock.lock();
            try{
                System.out.println("Waiting on input...");
                while(inputStateRef.get() == null){
                    inputStateIsNotNull.await();
                }
                System.out.println("Waiting done.....");
                PeerState inputState = inputStateRef.get();
                inputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                peerInputLock.unlock();
            }
        }
    }
}
