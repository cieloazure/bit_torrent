import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.io.ByteArrayOutputStream;
//import org.apache.commons.io.IOUtils;

class ActualMessage implements Message, Serializable {
    private static final long serialVersionUID = 42L;

    public static final MessageType[] messageValues = MessageType.values();
    public int messageLength;
    public MessageType messageType;
    public byte[] payload;
    public boolean isValid;

    public MessageType getMessageType() {
        return messageType;
    }

    public ActualMessage(MessageType messageType){
        this.messageType = messageType;
        this.messageLength = 1;
    }

    public ActualMessage(MessageType messageType, byte[] payload){
        this.messageType = messageType;
        this.messageLength = 1;
        setPayload(payload);
    }

    public void setPayload(byte[] payload) {
        this.payload = payload;
        this.messageLength += payload.length;
    }

    private void writeObject(ObjectOutputStream out)
            throws IOException {
        byte[] result = serialize();
        System.out.println("Actual message, write object:" + result.length);
        out.write(result);
    }

    private void readObject(ObjectInputStream in)
            throws IOException, ClassNotFoundException{

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        int bytesRead;
        byte[] buffer = new byte[1000];
        while((bytesRead = in.read(buffer)) != -1) {
            outputStream.write(buffer,0,bytesRead);
        }
        byte[] message = outputStream.toByteArray();
//        byte[] bytes = IOUtils.toByteArray(in);

//        byte[] message = in.readAllBytes();
        System.out.println("Actual message, read object:" + message.length);
        deserialize(message);
    }

    @Override
    public byte[] serialize() {
        byte[] result = new byte[this.messageLength + 4];
        byte[] messageLengthBytes = ByteBuffer.allocate(4).putInt(this.messageLength).array();
        int i = 0;
        for(int j = 0; j < messageLengthBytes.length; j++){
            result[i] = messageLengthBytes[j];
            i++;
        }
        result[i] = (byte)(char)this.messageType.ordinal();

        for(int j = 0; j < this.payload.length; j++){
            result[i] = this.payload[j];
            i++;
        }
        return result;
    }

    @Override
    public void deserialize(byte[] message) {
        byte[] messageLengthByteArr = Arrays.copyOfRange(message, 0, 4);
        ByteBuffer messageLengthBuffer = ByteBuffer.wrap(messageLengthByteArr);
        this.messageLength = messageLengthBuffer.getInt(); // 2
        System.out.println(this.messageLength);
        if(messageLength < 0){
            this.isValid = false;
            return;
        }
        int messageTypeInt = (int)message[4];
        if(messageTypeInt < 0 || messageTypeInt > messageValues.length){
            this.isValid = false;
            return;
        }
        this.messageType = messageValues[(int)message[4]];
        this.payload = new byte[this.messageLength - 1]; // 1
        for(int i = 5, j = 0; i < message.length; i++, j++){
            this.payload[j] = message[i];
        }
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    @Override
    public Object getReplyObject(PeerInfo p) {
        if(this.messageType == MessageType.BITFIELD){
            return new ActualMessage(MessageType.BITFIELD, p.getBitField().toByteArray());
        }
        return null;
    }
}
