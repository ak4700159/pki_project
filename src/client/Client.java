package client;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class Client {
    public static void main(String[] args) throws IOException {
        String host = "localhost";  // 서버 호스트 IP
        int port = 1234;

        Socket socket = new Socket(host, port);
        System.out.println("서버에 연결됨: " + socket.getRemoteSocketAddress());

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        ExecutorService pool = Executors.newFixedThreadPool(2);

        // 1) 읽기 스레드: 서버 -> 클라이언트 메시지 수신
        pool.submit(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println("서버: " + line);
                }
            } catch (IOException e) {
                System.err.println("읽기 오류: " + e.getMessage());
            }
        });

        // 2) 쓰기 스레드: 콘솔 -> 서버로 메시지 전송
        pool.submit(() -> {
            try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                String input;
                while ((input = console.readLine()) != null) {
                    out.println(input);
                }
            } catch (IOException e) {
                System.err.println("쓰기 오류: " + e.getMessage());
            }
        });
    }
}