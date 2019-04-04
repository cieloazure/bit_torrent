public class NeighbourPeerInfo extends PeerInfo{
    // In context of when Peer is a neighbour
    private Double downloadSpeed;
    private NeighbourState neighbourState;
    private Handler context;
    private int requestedPieceIndex;

    public NeighbourPeerInfo(PeerInfo.Builder b, Handler context) {
        super(b);
        this.context = context;
        downloadSpeed = 0.0;
        neighbourState = NeighbourState.UNKNOWN;
        requestedPieceIndex = -1;
    }

    public int getRequestedPieceIndex() {
        return requestedPieceIndex;
    }

    public void setRequestedPieceIndex(int requestedPieceIndex) {
        this.requestedPieceIndex = requestedPieceIndex;
    }

    public void setNeighbourState(NeighbourState neighbourState) {
        this.neighbourState = neighbourState;
    }

    public void setDownloadSpeed(Double downloadSpeed) {
        this.downloadSpeed = downloadSpeed;
    }

    public void setContext(Handler context) {
        this.context = context;
    }

    public void setContextState(PeerState newState, boolean isForInputState, boolean setOtherStateAsNull){
        this.context.setState(newState, isForInputState, setOtherStateAsNull);
    }
}
