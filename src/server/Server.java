package server;

import java.net.*;
import java.io.*;
import java.util.concurrent.*;

public class Server {
    public void run(String[] args) throws IOException {
        int port = 1234;
        ServerSocket serverSocket = new ServerSocket(port);
        System.out.println("서버: 포트 " + port + " 에서 대기 중...");

        Socket socket = serverSocket.accept();
        System.out.println("클라이언트 연결됨: " + socket.getRemoteSocketAddress());

        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

        ExecutorService pool = Executors.newFixedThreadPool(2);

        // 1) 읽기 스레드: 클라이언트 -> 서버 메시지 수신
        pool.submit(() -> {
            String line;
            try {
                while ((line = in.readLine()) != null) {
                    System.out.println("클라이언트: " + line);
                }
            } catch (IOException e) {
                System.err.println("읽기 오류: " + e.getMessage());
            }
        });

        // 2) 쓰기 스레드: 서버 콘솔 -> 클라이언트로 메시지 전송
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
        serverSocket.close();
        // 서버를 종료하거나 소켓 닫을 로직은 필요 시 추가
    }
}
