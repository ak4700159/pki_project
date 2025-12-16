package server;

public class Message {
    final MessageType type;
    final String message;

    Message(MessageType type, String message) {
        this.type = type;
        this.message = message;
    }
}
