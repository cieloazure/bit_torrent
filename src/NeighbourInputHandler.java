import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

class NeighbourInputHandler extends Handler implements Runnable {

    public NeighbourInputHandler(SelfPeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, BlockingQueue<PeerState> outputStateRef, DataInputStream inputStream, DataOutputStream outputStream, Object inputMutex, Object outputMutex, int theirPeerId) {
        super(myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex, theirPeerId);
    }

    @Override
    public void run() {
        System.out.println("Input Thread started for " + this.theirPeerId);
        while (true) {
            try {
                synchronized (this.inputMutex) {
                    while (this.inputStateRef.get() == null) {
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
