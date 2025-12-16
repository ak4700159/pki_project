package client;

// Writer와 Reader가 공유하는 상태 변수
public class SharedState {
    private MessageType recentMessageType;

    public SharedState() {
        recentMessageType = MessageType.WAIT;
    }

    public synchronized void set(MessageType t) {
        recentMessageType = t;
        if(recentMessageType == MessageType.SECURE || recentMessageType == MessageType.WRONG) {
            notifyAll(); // 상태 변경 알림
        }
    }

    public synchronized void check() throws RuntimeException{
        while(recentMessageType != MessageType.SECURE){
            try {
                if(MessageType.WRONG == recentMessageType){
                    throw new RuntimeException("Wrong MessageType");
                }
                wait();
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
    }
}

