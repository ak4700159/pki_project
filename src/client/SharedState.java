package client;

// Writer와 Reader가 공유하는 상태 변수
public class SharedState {
    private MessageType recentMessageType;

    public SharedState() {
        recentMessageType = MessageType.WAIT;
    }

    public synchronized void set(MessageType t) {
        recentMessageType = t;
        if(recentMessageType == MessageType.SECURE) {
            notifyAll(); // 상태 변경 알림
        }
    }

    public synchronized MessageType get() {
        return recentMessageType;
    }

    public synchronized void check() {
        while(recentMessageType != MessageType.SECURE){
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

