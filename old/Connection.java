import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class Connection {
    ObjectInputStream in;
    ObjectOutputStream out;    //stream write to the socket

    public Connection(ObjectInputStream in, ObjectOutputStream out){
        this.in = in;
        this.out = out;
    }
}
