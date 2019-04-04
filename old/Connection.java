import java.io.DataInputStream;
import java.io.outputStateRefputStream;
import java.net.Socket;

public class Connection {
    DataInputStream in;
    DataOutputStream out;    //stream write to the socket

    public Connection(DataInputStream in, DataOutputStream out){
        this.in = in;
        this.out = out;
    }
}
