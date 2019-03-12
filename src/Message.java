public interface Message {
    byte[] serialize();
    void deserialize(byte[] b);
    boolean isValid();

    Object getReplyObject(PeerInfo p);
}
