import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class NeighbourOutputHandler extends Handler implements Runnable {
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo;

    public NeighbourOutputHandler(Socket connection,
                                  SelfPeerInfo myPeerInfo,
                                  AtomicReference<PeerState> inputStateRef,
                                  BlockingQueue<PeerState> outputStateRef,
                                  DataInputStream inputStream, DataOutputStream outputStream,
                                  Object inputMutex, Object outputMutex,
                                  int theirPeerId, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo) {
        super(connection, myPeerInfo,
                inputStateRef, outputStateRef,
                inputStream, outputStream,
                inputMutex, outputMutex,
                theirPeerId);
        this.neighbourInfo = neighbourInfo;
    }

    @Override
    public void run() {
        while (myPeerInfo.getKeepWorking()) {
            try {
                synchronized (this.outputMutex) {
                    while (this.outputStateRef.isEmpty()) {
                        this.outputMutex.wait();
                    }
                    if (myPeerInfo.getKeepWorking()) {
                        while (!this.outputStateRef.isEmpty()) {
                            PeerState outputState = this.outputStateRef.poll();
                            outputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
                        }
                    }
                }
            } catch (InterruptedException e) {
//                System.out.println("Output handler close");
                e.printStackTrace();
            }
        }
//        System.out.println("Output handler close after while");
    }
}