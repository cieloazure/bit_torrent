import javax.xml.bind.SchemaOutputResolver;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class PeriodicTasks {
    SelfPeerInfo myPeerInfo;
    ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo;
    PeriodicTasks(SelfPeerInfo myPeerInfo, ConcurrentHashMap<Integer, NeighbourPeerInfo> neighbourInfo){
        this.myPeerInfo = myPeerInfo;
        this.neighbourInfo = neighbourInfo;
    }
    public void selectTopK(){
        System.out.println("Timer done. check for top k");

    }

    public void selectOptimisticallyUnchoked(){
        System.out.println("Timer done. check for opt unch");
    }

    public void startScheduledExecution(int topKinterval, int optUnchokedInt){
        // ScheduledExecutorService object which spawns the threads to execute periodic tasks like
        // selectKtopNeighbors and selectOptUnchNeighbor
        // TODO: Should there be a thread for the termination check as well?
        // TODO: (which periodically checks if everyone has the file and then triggers a graceful shutdown)

        ScheduledExecutorService schExec = Executors.newScheduledThreadPool(2);
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

