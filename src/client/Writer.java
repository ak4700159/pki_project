package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Writer extends Thread{
    private final Adaptor adaptor;
    private final SharedState sharedState;
    private final PrintWriter out;

    public Writer(SharedState state, Adaptor adaptor, PrintWriter out){
        this.sharedState = state;
        this.adaptor = adaptor;
        this.out = out;
    }

    @Override
    public void run() {
        // 최초로 서버에 자신을 식별할 수 있는 이름과 통신하고자 하는 상대방의 이름을 전달 (서버의 공개키로 암호화)
        sharedState.check();
        try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
            String input;
            while ((input = console.readLine()) != null) {

                out.println(input);
            }
        } catch (IOException e) {
            System.err.println("쓰기 오류: " + e.getMessage());
        }
    }
}
