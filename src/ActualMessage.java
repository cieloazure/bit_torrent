import java.nio.ByteBuffer;
import java.util.Arrays;

class ActualMessage implements Message{
    private static final MessageType[] messageValues = MessageType.values();
    private int messageLength;
    private MessageType messageType;
    private byte[] payload;

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
        byte[] messageLengthByteArr = Arrays.copyOfRange(message, 0, 3);
        ByteBuffer messageLengthBuffer = ByteBuffer.wrap(messageLengthByteArr);
        this.messageLength = messageLengthBuffer.getInt();
        this.messageType = messageValues[(int)message[4]];
        this.payload = new byte[this.messageLength - 2];
        for(int i = 5, j = 0; i < message.length; i++, j++){
            this.payload[j] = message[i];
        }
    }
}
