package client;

public class Message {
    final MessageType type;
    final String message;
    final boolean toPeer;

    Message(MessageType type, String message, boolean toPeer) {
        this.type = type;
        this.message = message;
        this.toPeer = toPeer;
    }
}
