import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

public class SelfPeerInfo extends PeerInfo {
    private Boolean hasFile;
    private List<byte[]> fileChunks;
    private BitSet requestedbitField;
    private Logger logger;
    private Logger stdOutputLogger;
    private boolean toStdOutput;
    private BitSet requestedPieces; //to track the requested pieces
    protected ScheduledExecutorService schExec;

    public SelfPeerInfo(PeerInfo.Builder b, Boolean hasFile, List<byte[]> fileChunks, Logger logger, BitSet requestedPieces) {
        super(b);
        this.hasFile = hasFile;
        this.fileChunks = fileChunks;
        this.logger = logger;
        this.stdOutputLogger = Logger.getAnonymousLogger();
        this.stdOutputLogger.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
        this.toStdOutput = false;
        this.requestedPieces = requestedPieces;
    }

    public void setRequestedPieces(BitSet requestedPieces) {
        this.requestedPieces = requestedPieces;
    }

    public BitSet getRequestedPieces() {
        return this.requestedPieces;
    }

    public void setRequestPiecesIndex(int index) {
        this.requestedPieces.set(index);
    }

    public void setSchExec(ScheduledExecutorService schExec){
        this.schExec = schExec;
    }
    public void killAllPeriodicTasks(){
        schExec.shutdown();
    }

    public Logger getLogger() {
        return logger;
    }

    public byte[] getFileChunk(int index) {
        return fileChunks.get(index);
    }

    public void enableStdOutputLogging(){
        this.toStdOutput = true;
    }

    public void log(String message){
        this.logger.info(message);
        if(this.toStdOutput){
            this.stdOutputLogger.info(message);
        }
    }
}
