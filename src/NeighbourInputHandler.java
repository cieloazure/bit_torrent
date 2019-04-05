import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

class NeighbourInputHandler extends Handler implements Runnable {

    public NeighbourInputHandler(Socket connection, SelfPeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, BlockingQueue<PeerState> outputStateRef, DataInputStream inputStream, DataOutputStream outputStream, Object inputMutex, Object outputMutex, int theirPeerId) {
        super(connection, myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex, theirPeerId);
    }

    @Override
    public void run() {
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
