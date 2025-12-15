package client;

import java.io.BufferedReader;
import java.io.IOException;

public class Reader extends Thread{
    private final Adaptor adaptor;
    private final SharedState sharedState;
    private final BufferedReader in;

    public Reader(SharedState state, Adaptor adaptor, BufferedReader in) {
        this.sharedState = state;
        this.adaptor = adaptor;
        this.in = in;
    }

    @Override
    public void run() {
        String line;
        try {
            // 서버가 종료되거나 상대방이 나간 경우 오류 발생(IOException)
            // 블락킹 방식으로 문자열이 전달될 대까지 대기
            while ((line = in.readLine()) != null) {
                // MATCH 타입 메시지를 전달받은 경우 true, 그 외 false
                if(adaptor.receiveFromServer(line)) {
                    // Writer 스레드 깨우기
                    sharedState.set(MessageType.SECURE);
                }
            }
        } catch (IOException e) {
            System.err.println("읽기 오류: " + e.getMessage());
        }
    }
}
