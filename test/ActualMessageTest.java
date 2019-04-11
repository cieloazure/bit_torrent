import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

class ActualMessageTest {

    @Test
    void serialize() throws IOException, ClassNotFoundException {
        BitSet b = new BitSet();
        for (int i = 0; i < 16; i++) {
            b.set(i);
        }
        ActualMessage message = new ActualMessage(MessageType.BITFIELD, b.toByteArray());
        byte[] serializedOutput = message.serialize();

        ActualMessage message1 = new ActualMessage(MessageType.BITFIELD);
        message1.deserialize(serializedOutput);
    }

    @Test
    void serialize2() throws IOException, ClassNotFoundException {
        BitSet b = new BitSet();
        for (int i = 0; i < 16; i++) {
            b.set(i);
        }
        ActualMessage message = new ActualMessage(MessageType.BITFIELD, b.toByteArray());
        byte[] serializedOutput = message.serialize();

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        DataOutputStream dos = new DataOutputStream(bos);
        dos.write(serializedOutput);

        byte[] streamOutput = bos.toByteArray();
        assertArrayEquals(serializedOutput, streamOutput);
    }
}