package client;

// Writer와 Reader가 공유하는 상태 변수
public class SharedState {
    private MessageType recentMessageType;

    public SharedState() {
        recentMessageType = MessageType.WAIT;
    }

    public synchronized void set(MessageType t) {
        recentMessageType = t;
        notifyAll(); // 상태 변경 알림
    }

    public synchronized MessageType get() {
        return recentMessageType;
    }

    public synchronized void check() {
        while(recentMessageType == MessageType.WAIT){
            try {
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

