import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Peer {
    List<Connection> myConnectedPeers;
    int mySocket;

    public Peer(int mySocket){
        this.mySocket = mySocket;
        myConnectedPeers = new ArrayList<>();
    }

    public static void main(String[] args) {
        Peer p = new Peer(Integer.parseInt(args[0]));
        Listener l = new Listener(p);
        Thread listenerThread = new Thread(l);
        listenerThread.start();

        while(true){
            System.out.println("on main thread");

            System.out.println("Menu");
            System.out.println("1. Enter peer port");
            System.out.println("2. Conversation with a peer");

            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            try {
                String message = bufferedReader.readLine();
                switch (message) {
                    case "1":
                        int port = Integer.parseInt(bufferedReader.readLine());
                        Socket requestSocket = new Socket("localhost", port );
                        System.out.println("Connected to localhost in port " + port);

                        ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
                        out.flush();
                        ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
                        p.myConnectedPeers.add(new Connection(in, out));
                        break;
                    case "2":
                        System.out.println("Enter peer #");
                        int peer = Integer.parseInt(bufferedReader.readLine());
                        Connection p1 = p.myConnectedPeers.get(peer);
                        while (true) {
                            System.out.print("Hello, please input a sentence: Type end to stop ");
                            //read a sentence from the standard input
                            message = bufferedReader.readLine();
                            if(message.equals("end")){
                                break;
                            }
                            //Send the sentence to the server
//                    sendMessage(out, in, message);

                            Thread t = new Thread(new PeerOutputHandler(p1.out, p1.in, message));
                            t.start();
                            t.join();
                        }

                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

        }


//        if(args.length > 1){
//            try {
//                Socket requestSocket = new Socket("localhost", Integer.parseInt(args[1]));
//                System.out.println("Connected to localhost in port "+Integer.parseInt(args[1]));
//
//                ObjectOutputStream out = new ObjectOutputStream(requestSocket.getOutputStream());
//                out.flush();
//                ObjectInputStream in = new ObjectInputStream(requestSocket.getInputStream());
//
//                //get Input from standard input
//                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
//                String message = null;
//                while(true){
//                    System.out.print("Hello, please input a sentence: ");
//                    //read a sentence from the standard input
//                    message = bufferedReader.readLine();
//                    //Send the sentence to the server
////                    sendMessage(out, in, message);
//
//                    Thread t = new Thread(new PeerOutputHandler(out, in, message));
//                    t.start();
//                    t.join();
//                }
//
//            } catch (IOException e) {
//                e.printStackTrace();
//            } catch (InterruptedException e) {
//                e.printStackTrace();
//            }
//        }

    }

    private static class Listener implements Runnable{
        public Peer host;

        public Listener(Peer host){ this.host = host; }

        @Override
        public void run() {
            try{
                ServerSocket listener = new ServerSocket(this.host.mySocket);
                while(true){
                    new Thread(new PeerInputHandler(this.host, listener.accept())).start();
                    System.out.println("Got a peer connection! Spawning PeerInputHandler for a peer");
                }
            }catch(IOException e){
                System.out.println(e.getMessage());
            }
        }
    }

    private static class PeerInputHandler implements Runnable{

        public Peer host;
        public Socket connection;
        public ObjectOutputStream out;
        public ObjectInputStream in;
        public String message;
        public String MESSAGE;

        PeerInputHandler(Peer host, Socket connection){
            this.host = host;
            this.connection = connection;
        }

        public void run() {
            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                this.host.myConnectedPeers.add(new Connection(in, out));
                try{
                    while(true)
                    {
                        System.out.println("waiting for input from peer");
                        //receive the message sent from the client
                        message = (String)in.readObject();
                        //show the message to the user
                        System.out.println("Receive message: " + message + " from peer");
                        //Capitalize all letters in the message
                        // TODO: Bittorrent protocol over here
                        MESSAGE = message.toUpperCase();
                        //send MESSAGE back to the client
                        sendMessage(MESSAGE);
                    }
                }
                catch(ClassNotFoundException classnot){
                    System.err.println("Data received in unknown format");
                }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client ");
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client ");
                }
            }
        }

        //send a message to the output stream
        public void sendMessage(String msg)
        {
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client ");
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

    }


    private static class PeerOutputHandler implements  Runnable{
        private ObjectOutputStream out;
        private ObjectInputStream in ;
        String msg;

        PeerOutputHandler(ObjectOutputStream out, ObjectInputStream in, String msg){
            this.out = out;
            this.in = in;
            this.msg = msg;
        }

        public void run(){
            try{
                out.writeObject(msg);
                out.flush();
                System.out.println("Send message: " + msg + " to Client ");
                // TODO: Bitcoin protocol here
                //Receive the upperCase sentence from the server
                String MESSAGE = (String)in.readObject();
                //show the message to the user
                System.out.println("Receive message: " + MESSAGE);
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    //send a message to the output stream
//    public static void sendMessage(ObjectOutputStream out, ObjectInputStream in, String msg)
//    {
//        try{
//            out.writeObject(msg);
//            out.flush();
//            System.out.println("Send message: " + msg + " to Client ");
//        }
//        catch(IOException ioException){
//            ioException.printStackTrace();
//        }
//    }
}
