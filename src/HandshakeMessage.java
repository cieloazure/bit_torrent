import java.nio.ByteBuffer;
import java.util.Arrays;

class HandshakeMessage implements  Message{
    private static String header = "P2PFILESHARINGPROJ";
    private int peerID;
    private boolean isValid;

    public HandshakeMessage(int peerID){
        this.peerID = peerID;
    }

    public int getPeerID() {
        return peerID;
    }

    @Override
    public byte[] serialize() {
        int handshakeMessageSize = header.length() + 10 + 4;
        byte[] result = new byte[handshakeMessageSize];
        int i = 0;
        for(char c: header.toCharArray()){
            if(c != '\0'){
                result[i] = (byte)c;
                i++;
            }
        }

        for(int j = 0; j < 10; j++){
            result[i] = (byte)0;
            i++;
        }

        byte[] peerIDBytes = ByteBuffer.allocate(4).putInt(peerID).array();
        for(int j = 0; j < peerIDBytes.length; j++){
            result[i] = peerIDBytes[j];
            i++;
        }

        return result;
    }


    @Override
    public void deserialize(byte[] message) {
        StringBuilder s = new StringBuilder();
        for(int i = 0; i < header.length(); i++){
            s.append((char)message[i]);
        }
        if(!s.toString().equals(header)){
            this.isValid = false;
        }

        for(int i = header.length(); i < 10; i++){
            if((int)message[i] != 0){
                this.isValid = false;
            }
        }

        byte[] idByteArr = Arrays.copyOfRange(message, header.length() + 10, message.length);
        ByteBuffer pid = ByteBuffer.wrap(idByteArr);
        this.peerID = pid.getInt();
        this.isValid = true;
    }
}
