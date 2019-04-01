import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

class NeighbourOutputHandler extends Handler implements Runnable{
    public NeighbourOutputHandler(Socket connection, PeerInfo myPeerInfo, AtomicReference<PeerState> inputStateRef, AtomicReference<PeerState> outputStateRef, DataInputStream inputStream, DataOutputStream outputStream, Object inputMutex, Object outputMutex) {
        super(connection, myPeerInfo, inputStateRef, outputStateRef, inputStream, outputStream, inputMutex, outputMutex);
    }

    @Override
    public void run(){
        while(true){
            try{
                synchronized (this.outputMutex){
                    while(this.outputStateRef.get() == null){
                        this.outputMutex.wait();
                    }
                    PeerState outputState = this.outputStateRef.get();
                    outputState.handleMessage(this, this.myPeerInfo, this.inputStream, this.outputStream);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}