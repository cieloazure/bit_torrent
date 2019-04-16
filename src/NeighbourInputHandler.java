import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

class NeighbourInputHandler extends Handler implements Runnable {

    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo;

    public NeighbourInputHandler(Socket connection,
                                 SelfPeerInfo myPeerInfo,
                                 AtomicReference<PeerState> inputStateRef,
                                 BlockingQueue<PeerState> outputStateRef,
                                 DataInputStream inputStream,DataOutputStream outputStream,
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
                synchronized (this.inputMutex) {
                    while (this.inputStateRef.get() == null) {
                        this.inputMutex.wait();
                    }
                    if(myPeerInfo.getKeepWorking()){
                        PeerState inputState = inputStateRef.get();
                        inputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
                    }

                }
            } catch (InterruptedException e) {
                System.out.println("Input handler close");
                e.printStackTrace();
            }
        }
        System.out.println("Input handler close after while");
    }
}
