import javax.xml.bind.SchemaOutputResolver;

public class PeriodicTasks {
    SelfPeerInfo myPeerInfo;
    PeriodicTasks(SelfPeerInfo myPeerInfo){
        this.myPeerInfo = myPeerInfo;
    }
    public void selectTopK(){
        System.out.println("Timer done. check for top K");
    }

    public void selectOptimisticallyUnchocked(){
        System.out.println("Timer done. check for opt unch");
    }
}
