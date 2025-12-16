package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class Writer extends Thread{
    private final Adaptor adaptor;
    private final SharedState sharedState;
    private final String clientId;
    private final String targetId;

    public Writer(SharedState state, Adaptor adaptor, String clientId, String targetId){
        this.sharedState = state;
        this.adaptor = adaptor;
        this.clientId = clientId;
        this.targetId = targetId;
    }

    @Override
    public void run() {
        adaptor.sendToServer(clientId + "@" + targetId, MessageType.INIT);
        sharedState.check();
        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            while ((input = console.readLine()) != null) {
                adaptor.sendToServer(input, MessageType.SECURE);
            }
        } catch (IOException e) {
            System.err.println("쓰기 오류: " + e.getMessage());
        }
    }
}
