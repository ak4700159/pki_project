package server;

import java.io.PrintWriter;

public class ClientInfo{
    // 공개키
    private String publicKey;
    // 최근 클라이언트가 보낸 메시지 타입
    private MessageType recentMessageType;
    // 클라이언트 output 소켓
    private PrintWriter out;

    public ClientInfo() {
        this.publicKey = null;
        this.recentMessageType = null;
        this.out = null;
    }

    public void setOut(PrintWriter out) {
        this.out = out;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public void setRecentMessageType(MessageType recentMessageType) {
        this.recentMessageType = recentMessageType;
    }

    public PrintWriter getOut() { return out; }

    public MessageType getRecentMessageType() {
        return recentMessageType;
    }

    public String getPublicKey() {
        return publicKey;
    }
}