public class peerProcess {
    public static void main(String[] args){
        int peerID = Integer.parseInt(args[0]);
        Peer.start(peerID);
    }
}
