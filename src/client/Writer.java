package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Writer extends Thread{
    private final Adaptor adaptor;
    private final SharedState sharedState;

    public Writer(SharedState state, Adaptor adaptor){
        this.sharedState = state;
        this.adaptor = adaptor;
    }

    @Override
    public void run() {
        adaptor.sendToServer("kim@lee", MessageType.INIT);
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
