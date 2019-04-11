
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Class that holds the periodic tasks for selecting K preferred neighbours and 1 optimistically unchoked neighbour
 */

public class PeriodicTasks {
    SelfPeerInfo myPeerInfo;
    private ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo;
    private Integer peerCount;
    private List<Integer> kPreferred = Collections.synchronizedList(new ArrayList<>());
    private int optimisticallyUnchokedPeerID;

    PeriodicTasks(SelfPeerInfo myPeerInfo, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo, Integer peerCount) {
        this.myPeerInfo = myPeerInfo;
        this.neighbourInfo = neighbourInfo;
        this.peerCount = peerCount;
        this.optimisticallyUnchokedPeerID = 0;
    }

    /**
     * Function to select the top K preferred neighbors to unchoke
     */
    public void selectTopK() {
        try {

            // This TreeMap stores the mapping of download rate to an ArrayList of peerIDs
            // Using array list so as to deal with the corner case of two peers having the same download rate
            Map<Double, ArrayList<Integer>> downloadRateToPeerId = new TreeMap<>();

            int totalPieces = (int)(this.myPeerInfo.getCommonConfig().getFileSize()/this.myPeerInfo.getCommonConfig().getPieceSize());
            int peersWithCompleteFile = 0;
            boolean canShutdown = false;

            for (Integer key : neighbourInfo.keySet()) {

                if (neighbourInfo.get(key).isInterested()) {

                    if (!downloadRateToPeerId.containsKey(neighbourInfo.get(key).getDownloadSpeed())) {
                        downloadRateToPeerId.put(neighbourInfo.get(key).getDownloadSpeed(), new ArrayList<Integer>());
                        downloadRateToPeerId.get(neighbourInfo.get(key).getDownloadSpeed()).add(key);
                    } else {
                        downloadRateToPeerId.get(neighbourInfo.get(key).getDownloadSpeed()).add(key);
                    }
                }
                //if the Peer itself has received all pieces, check if the neighbors have received all pieces.
                if(this.myPeerInfo.getBitField().cardinality() == totalPieces) {
                    //if neighbour peer has all pieces
                    if(neighbourInfo.get(key).getBitField() != null) {
                        if(neighbourInfo.get(key).getBitField().cardinality() == totalPieces)
                            peersWithCompleteFile ++;
                    }
                }

            }

            if(peersWithCompleteFile == neighbourInfo.size()) {
                this.myPeerInfo.interruptListener();
                myPeerInfo.setKeepWorking(false);
                for (Integer key : neighbourInfo.keySet()) {
                    neighbourInfo.get(key).getContext().setState(new ExpectedToSendFailedMessageState(), false, false);
                    neighbourInfo.get(key).getContext().closeConnection();
                }
                this.myPeerInfo.killAllPeriodicTasks();

            }


            if (downloadRateToPeerId.size() == 0) {
//                myPeerInfo.log("DEBUG: downloadRateToPeerId is empty");
            }
            Set<Map.Entry<Double, ArrayList<Integer>>> set = downloadRateToPeerId.entrySet();
            Set<Integer> selectedK = new HashSet<>();

            int count = 0;
            int emptyCount = 0;
            // Building the set of top K preferred neighbours
            // Will have to further segregate this set into two different sets(those that need to be choked and those that
            // need to be unchoked)
            int arrayListSize = 0;
            Random rand = new Random();
            while (!(count == peerCount || emptyCount == downloadRateToPeerId.size())) {
                emptyCount = 0;
                for (Map.Entry<Double, ArrayList<Integer>> me : set) {
                    arrayListSize = downloadRateToPeerId.get(me.getKey()).size();
                    if (arrayListSize > 0) {
                        selectedK.add(me.getValue().remove(rand.nextInt(arrayListSize)));
                        count++;
                        if (count == peerCount) {
                            break;
                        }
                    } else {
                        emptyCount++;
                        continue;
                    }

                    if (emptyCount == downloadRateToPeerId.size()) {
                        break;
                    }

                }
            }
            kPreferred.clear();
            for (Integer temp : selectedK) {
                kPreferred.add(temp);
                // Check for choked or unchoked state here and take action accordingly
                // The below code can be uncommented and used once  isChoked() function is in place and
                // the handling of choked unchoked messages is done
                System.out.println("Check the status of the piece " + neighbourInfo.get(temp).getNeighbourState());
                if (neighbourInfo.get(temp).isChoked()) {
//                    myPeerInfo.log("DEBUG: Unchoking " + temp);
                    neighbourInfo.get(temp).setContextState(new ExpectedToSendUnchokeMessageState(neighbourInfo), false, false);
                    // This is where Sharmilee's function to UNCHOKE will be called
                } else if (neighbourInfo.get(temp).isUnChoked()) {
                    if (temp != optimisticallyUnchokedPeerID) {
//                        myPeerInfo.log("DEBUG: Choking " + temp);
                        neighbourInfo.get(temp).setContextState(new ExpectedToSendChokeMessageState(neighbourInfo), false, false);
                    }
//
                }

            }
            myPeerInfo.log("Peer ["+myPeerInfo.getPeerID()+"] has the preferred neighbors " + selectedK);
            System.out.println(selectedK);

        } catch (Exception e) {
            System.out.println("Some issue in K select");
            e.printStackTrace();

        }

    }

    /**
     * Function to select the optimistically unchoked neighbour
     */
    public void selectOptimisticallyUnchoked() {
        try {
            ArrayList<Integer> optUnchokedPool = new ArrayList<>();
            Random rand = new Random();
            int optUnchokedPeer = 0;
            for (Integer key : neighbourInfo.keySet()) {
                if (neighbourInfo.get(key).isChokedAndInterested()) {
                    optUnchokedPool.add(neighbourInfo.get(key).getPeerID());
                }
            }
            if (optUnchokedPool.size() > 0) {
                optUnchokedPeer = optUnchokedPool.get(rand.nextInt(optUnchokedPool.size()));

                if (!kPreferred.contains(optimisticallyUnchokedPeerID)) {
                    System.out.println("DEBUG: Choke this->" + optimisticallyUnchokedPeerID);
                    neighbourInfo.get(optimisticallyUnchokedPeerID).setContextState(new ExpectedToSendChokeMessageState(neighbourInfo), false, false);
                }

                //change the optimistically unchoked peer
                optimisticallyUnchokedPeerID = optUnchokedPeer;

                // Unchoke the peer with this peer id
                System.out.println("DEBUG: " + optUnchokedPeer + ": Unchoke me! " + optimisticallyUnchokedPeerID);
                myPeerInfo.log("Peer ["+myPeerInfo.getPeerID()+"] has the optimistically unchoked neighbor [" + optimisticallyUnchokedPeerID + "]");
                neighbourInfo.get(optimisticallyUnchokedPeerID).setContextState(new ExpectedToSendUnchokeMessageState(neighbourInfo), false, false);

            } else {
                myPeerInfo.log("DEBUG: optUnchokedPool is empty");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


    }

    /**
     * This function triggers the threads that periodically perform the following two actions-
     * Choose k preferred neighbours
     * Choose optimistically unchoked neighbour
     *
     * @param topKinterval    : How often do we need to reset our k preferred neighbours list?
     * @param optUnchokedInt: How often do we need to select an optimistically unchoked neighbour
     */
    public void startScheduledExecution(int topKinterval, int optUnchokedInt) {

        // ScheduledExecutorService object which spawns the threads to execute periodic tasks like
        // selectKtopNeighbors and selectOptUnchNeighbor
        // TODO: Should there be a thread for the termination check as well?
        // TODO: (which periodically checks if everyone has the file and then triggers a graceful shutdown)

        ScheduledExecutorService schExec = Executors.newScheduledThreadPool(2);
        myPeerInfo.setSchExec(schExec);

        Runnable selectTopK = () -> {
            selectTopK();
        };
        Runnable selectOptUnchoked = () -> {
            selectOptimisticallyUnchoked();
        };

        schExec.scheduleAtFixedRate(selectTopK,
                2,
                topKinterval,
                TimeUnit.SECONDS);

        schExec.scheduleAtFixedRate(selectOptUnchoked,
                2,
                optUnchokedInt,
                TimeUnit.SECONDS);

    }

}

