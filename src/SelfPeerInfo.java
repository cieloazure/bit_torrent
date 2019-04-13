import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Timestamp;
import java.util.BitSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;


public class SelfPeerInfo extends PeerInfo {
    protected ScheduledExecutorService schExec;
    private Boolean hasFile;
    private Map<Integer, byte[]> fileChunks;
    private BitSet requestedbitField;
    private Logger logger;
    private Logger stdOutputLogger;
    private CommonConfig commonConfig;
    private boolean toStdOutput;
    private ConcurrentHashMap<Integer, Integer> requestedPieces; //to track the requested pieces
    private ServerSocket listener;
    private boolean keepWorking;

    public SelfPeerInfo(PeerInfo.Builder b,
                        Boolean hasFile,
                        Map<Integer, byte[]> fileChunks,
                        Logger logger,
                        BitSet requestedPieces,
                        CommonConfig commonConfig) {
        super(b);
        this.hasFile = hasFile;
        this.fileChunks = fileChunks;
        this.logger = logger;
        this.stdOutputLogger = Logger.getAnonymousLogger();
        this.stdOutputLogger.addHandler(new StreamHandler(System.out, new SimpleFormatter()));
        this.toStdOutput = false;
        this.requestedPieces = new ConcurrentHashMap<>();
        for (int i = requestedPieces.nextSetBit(0); i >= 0; i = requestedPieces.nextSetBit(i + 1)) {
            this.setRequestPiecesIndex(i, 1);
        }
        this.commonConfig = commonConfig;
        this.keepWorking = true;
    }

    public static String ts() {
        return "" + new Timestamp(new java.util.Date().getTime());
    }

    public BitSet getRequestedPieces() {

        BitSet reqPieces = new BitSet();
        for (Integer key : this.requestedPieces.keySet()) {
            if (this.requestedPieces.get(key) == 1) {
                reqPieces.set(key);
            }
        }
        return reqPieces;
    }

    public void setRequestPiecesIndex(int index, int value) {
        this.requestedPieces.put(index, value);
    }

    public void setSchExec(ScheduledExecutorService schExec) {
        this.schExec = schExec;
    }

    public void killAllPeriodicTasks() {
        schExec.shutdown();
    }

    public Logger getLogger() {
        return logger;
    }

    public byte[] getFileChunk(int index) {
        System.out.println("Index is " + index);
        return this.fileChunks.get(index);
    }

    public void setFileChunkIndex(int index, byte[] chunk) {
        this.fileChunks.putIfAbsent(index, chunk);
    }

    public void enableStdOutputLogging() {
        this.toStdOutput = true;
    }

    public void log(String message) {
        this.logger.info(message);
        if (this.toStdOutput) {

//            this.stdOutputLogger.info(message);
            System.out.println(ts() + " : " + message);
        }
    }

    public void combineFileChunks() {
        File dir = new File("peer_" + peerID.toString() + "/");
        dir.mkdirs();
        File f = new File("peer_" + peerID.toString() + "/" + commonConfig.getFileName());
        TreeSet<Integer> pieceIndexes = new TreeSet<>(fileChunks.keySet());

        try {
            FileOutputStream outputStream = new FileOutputStream(f);
            for (Integer piece : pieceIndexes) {
                outputStream.write(fileChunks.get(piece));
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    public CommonConfig getCommonConfig(){
        return this.commonConfig;
    }

    public void setListener(ServerSocket listener) {
        this.listener = listener;
    }

    public void interruptListener(){
        try {
            this.listener.close();
        }
        catch (Exception e){

        }

    }

    public void setKeepWorking(boolean keepWorking) {
        this.keepWorking = keepWorking;
    }
    public boolean getKeepWorking(){
        return this.keepWorking;
    }
}
