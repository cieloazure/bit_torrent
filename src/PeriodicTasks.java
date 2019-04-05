import javax.xml.bind.SchemaOutputResolver;
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
    private int optimisticallyUnchoked;

    PeriodicTasks(SelfPeerInfo myPeerInfo, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo, Integer peerCount){
        this.myPeerInfo = myPeerInfo;
        this.neighbourInfo = neighbourInfo;
        this.peerCount = peerCount;
        this.optimisticallyUnchoked = 0;
    }

    /**
     * Function to select the top K preferred neighbors to unchoke
     */
    public void selectTopK(){
        try {

            // This TreeMap stores the mapping of download rate to an ArrayList of peerIDs
            // Using array list so as to deal with the corner case of two peers having the same download rate
            Map<Double, ArrayList<Integer>> downloadRateToPeerId = new TreeMap<>();
            for (Integer key : neighbourInfo.keySet()) {
//                if neighbourInfo.get(key).isInterested() {
                    if (!downloadRateToPeerId.containsKey(neighbourInfo.get(key).getDownloadSpeed())) {
                        downloadRateToPeerId.put(neighbourInfo.get(key).getDownloadSpeed(), new ArrayList<Integer>());
                        downloadRateToPeerId.get(neighbourInfo.get(key).getDownloadSpeed()).add(key);
                    } else {
                        downloadRateToPeerId.get(neighbourInfo.get(key).getDownloadSpeed()).add(key);
                    }
//                }
            }

            Set<Map.Entry<Double, ArrayList<Integer>>> set = downloadRateToPeerId.entrySet();
            Set<Integer> selectedK = new HashSet<>();

            int count=0;
            int emptyCount = 0;
            // Building the set of top K preferred neighbours
            // Will have to further segregate this set into two different sets(those that need to be choked and those that
            // need to be unchoked)
            while (!(count == peerCount || emptyCount == downloadRateToPeerId.size())) {
                emptyCount = 0;
                for (Map.Entry<Double, ArrayList<Integer>> me : set) {
                    if (downloadRateToPeerId.get(me.getKey()).size()>0){
                        selectedK.add(me.getValue().remove(0));
                        count++;
                        if (count == peerCount) {
                            break;
                        }
                    }
                    else {
                        emptyCount++;
                        continue;
                    }

                    if(emptyCount == downloadRateToPeerId.size()){
                        break;
                    }

                }
            }
            kPreferred.clear();
            for (Integer temp : selectedK) {
                kPreferred.add(temp);
                System.out.println(temp);
                // Check for choked or unchoked state here and take action accordingly
                // The below code can be uncommented and used once  isChoked() function is in place and
                // the handling of choked unchoked messages is done
//                if (neighbourInfo.get(temp).isChoked()){
//
//                }
//                else{
//
//                }

            }
            System.out.println("kprefkfunc");
            System.out.println(kPreferred);
            System.out.println(selectedK);

        }catch (Exception e){
            System.out.println("Some issue in K select");
            e.printStackTrace();

        }

    }
    /**
     * Function to select the optimistically unchoked neighbour
     */
    public void selectOptimisticallyUnchoked(){
        try{
            ArrayList<Integer> optUnchokedPool = new ArrayList<>();
            Random rand = new Random();
            int optUnchokedPeer = 0;
            for (Integer key : neighbourInfo.keySet()) {
//            Uncomment the if block when the two functions isInterested and isChoked() are in place
//                if (neighbourInfo.get(key).isInterested() && neighbourInfo.get(key).isChoked()) {
                optUnchokedPool.add(neighbourInfo.get(key).getPeerID());
//                }
            }
            if (optUnchokedPool.size()>0){
                do {
                    optUnchokedPeer = optUnchokedPool.get(rand.nextInt(optUnchokedPool.size()));
                }while(optUnchokedPeer==optimisticallyUnchoked);
                if(!kPreferred.contains(optimisticallyUnchoked)){
                    System.out.println("OPTCHK: Choke this->"+optimisticallyUnchoked+" MOFO");
                }
                optimisticallyUnchoked = optUnchokedPeer;

                // Unchoke the peer with this peer id
                System.out.println(optUnchokedPeer+ ": Unchoke me! "+ optimisticallyUnchoked);

            }
        }
        catch (Exception e){
            e.printStackTrace();
        }


    }

    /**
     * This function triggers the threads that periodically perform the following two actions-
     * Choose k preferred neighbours
     * Choose optimistically unchoked neighbour
     * @param topKinterval : How often do we need to reset our k preferred neighbours list?
     * @param optUnchokedInt: How often do we need to select an optimistically unchoked neighbour
     */
    public void startScheduledExecution(int topKinterval, int optUnchokedInt){

        // ScheduledExecutorService object which spawns the threads to execute periodic tasks like
        // selectKtopNeighbors and selectOptUnchNeighbor
        // TODO: Should there be a thread for the termination check as well?
        // TODO: (which periodically checks if everyone has the file and then triggers a graceful shutdown)

        ScheduledExecutorService schExec = Executors.newScheduledThreadPool(2);
        myPeerInfo.setSchExec(schExec);

        Runnable selectTopK = ()->{
            selectTopK();
        };
        Runnable selectOptUnchoked = ()->{
            selectOptimisticallyUnchoked();
        };

        schExec.scheduleAtFixedRate(selectTopK,
                topKinterval,
                optUnchokedInt,
                TimeUnit.SECONDS);

        schExec.scheduleAtFixedRate(selectOptUnchoked,
                topKinterval,
                optUnchokedInt,
                TimeUnit.SECONDS);

    }

}

