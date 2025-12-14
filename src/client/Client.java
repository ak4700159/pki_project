package client;

import java.net.*;
import java.io.*;

public class Client {
    public void execute(String[] args) throws IOException {
        if(args.length != 4) {
            throw new IllegalArgumentException("Wrong number of arguments");
        }

        String ip = args[0];  // 서버 IP
        String userName = args[2];
        String targetName = args[3];
        int port;
        // 서버 포트 양식 확인
        try {
            port = Integer.parseInt(args[1]); // 서버 Port
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Wrong Number type");
        }

        // 서버와 소켓 통신
        Socket socket = new Socket(ip, port);
        // Server -> Client Channel(byte -> char -> Buffer)
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        // Client -> Server Channel(byte -> byte[])
        //      PrintWriter.autoFlush : println 호출시 자동 전송
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
        // Thread pool 생성, 고정된 Task 수를 가짐
        System.out.println("서버에 연결됨 : " + socket.getRemoteSocketAddress() + ", " + userName);

        SharedState state = new SharedState();
        // 1) 읽기 스레드: 서버 -> 클라이언트 메시지 수신
        Reader reader = new Reader(state, in);
        reader.start();

        // 2) 쓰기 스레드: 콘솔 -> 서버로 메시지 전송
        out.println(String.format("%s,%s", userName, targetName));
        Writer writer = new Writer(state, out);
        writer.start();

        // 두 쓰레드가 종료될 때까지 대기
        try {
            writer.join();
            reader.join();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        socket.close();
    }
}