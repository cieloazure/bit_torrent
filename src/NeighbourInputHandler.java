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
        System.out.println("Started thread");
        while(true){
            try{
                peerInputLock.lock();
                System.out.println("Waiting for input mutex!");
                while(inputStateRef.get() == null){
                    inputStateIsNotNull.await();
                }
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
