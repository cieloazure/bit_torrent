import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

class NeighbourOutputHandler extends Handler implements Runnable{
    public NeighbourOutputHandler(Socket connection, Lock peerInputLock, Lock peerOutputLock, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, Condition inputStateIsNotNull, AtomicReference<PeerState> outputStateRef, Condition outputStateIsNotNull, ObjectInputStream inputStream, ObjectOutputStream outputStream) {
        super(connection, peerInputLock, peerOutputLock, myPeerInfo, inputStateRef, inputStateIsNotNull, outputStateRef, outputStateIsNotNull, inputStream, outputStream);
    }

    @Override
    public void run(){
        System.out.println("Started thread");
        while(true){
            try{
                peerOutputLock.lock();
                System.out.println("Waiting for output mutex");
                while(outputStateRef.get() == null){
                    outputStateIsNotNull.await();
                }
                PeerState outputState = outputStateRef.get();
                outputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                peerOutputLock.unlock();
            }
        }
    }
}