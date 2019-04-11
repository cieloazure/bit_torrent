import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.Arrays;

class ActualMessage implements Message, Serializable {
    public static final MessageType[] messageValues = MessageType.values();
    private static final long serialVersionUID = 42L;
    public int messageLength;
    public MessageType messageType;


    public byte[] payload;
    public boolean isValid;

    public ActualMessage(MessageType messageType) {
        this.messageType = messageType;
        this.messageLength = 1;
    }

    public ActualMessage(MessageType messageType, byte[] payload) {
        this.messageType = messageType;
        this.messageLength = 1;
        setPayload(payload);
    }

    public ActualMessage(byte[] messageBytes) {
        deserialize(messageBytes);
    }

    public MessageType getMessageType() {
        return messageType;
    }

    public byte[] getPayload() {
        return payload;
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
        for (int j = 0; j < messageLengthBytes.length; j++) {
            result[i] = messageLengthBytes[j];
            i++;
        }
        result[i] = (byte) (char) this.messageType.ordinal();
        i++;

        if (this.payload != null) {
            for (int j = 0; j < this.payload.length; j++) {
                result[i] = this.payload[j];
                i++;
            }
        }
//        System.out.println("Serialize Message Length "+ this.messageLength);
//        System.out.println("Serialize Message type "+ this.messageType);
        return result;
    }

    @Override
    public void deserialize(byte[] message) {
        byte[] messageLengthByteArr = Arrays.copyOfRange(message, 0, 4);
        ByteBuffer messageLengthBuffer = ByteBuffer.allocate(4).wrap(messageLengthByteArr);
        this.messageLength = messageLengthBuffer.getInt(); // 2
        System.out.println("Message length:" + this.messageLength);
        if (messageLength < 0) {
            this.isValid = false;
            return;
        }
        int messageTypeInt = (int) message[4];
        System.out.println("Message type:" + messageTypeInt);
        if (messageTypeInt < 0 || messageTypeInt > messageValues.length) {
            this.isValid = false;
            return;
        }
        this.messageType = messageValues[(int) message[4]];
        this.payload = new byte[this.messageLength - 1]; // 1
        for (int i = 5, j = 0; i < message.length; i++, j++) {
            this.payload[j] = message[i];
        }
        this.isValid = true;
    }

    @Override
    public boolean isValid() {
        return isValid;
    }

    public int getMessageLength() {
        return messageLength;
    }
}