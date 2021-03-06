import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;

class NeighbourOutputHandler extends Handler implements Runnable {
    public NeighbourOutputHandler(Socket connection, SelfPeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, BlockingQueue<PeerState> outputStateRef, DataInputStream inputStream, DataOutputStream outputStream, Object inputMutex, Object outputMutex, int theirPeerId) {
        super(connection, myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex, theirPeerId);
    }

    @Override
    public void run() {
        while (true) {
            try {
                synchronized (this.outputMutex) {
                    while (this.outputStateRef.isEmpty()) {
                        this.outputMutex.wait();
                    }
                    while (!this.outputStateRef.isEmpty()) {
                        PeerState outputState = this.outputStateRef.poll();
                        outputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}