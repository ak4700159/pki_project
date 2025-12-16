package server;

import java.io.*;
import java.net.Socket;
import java.util.UUID;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Cryptographer cryptographer;
    private final SharedState state;
    private String clientId;
    private String targetId;
    private boolean onWriter;

    public ClientHandler(Socket socket,
                         Cryptographer cryptographer,
                         SharedState state) {
        this.socket = socket;
        this.cryptographer = cryptographer;
        this.state = state;
        this.onWriter = false;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            System.out.println("ClientHandler 시작: " + clientId);
            // ClientInfo 등록
            ClientInfo info = state.getClientInfo(clientId);
            if(info == null) {
                ClientInfo newInfo = new ClientInfo();
                newInfo.setOut(out);
                state.clientInfoTable.put(clientId, newInfo);
            } else {
                info.setOut(out);
            }
            Adaptor adaptor = new Adaptor(cryptographer, state);
            // 최초 클라이언트로부터 전달받은 메시지를 바탕으로
            String initMessage = in.readLine();
            this.clientId = adaptor.receiveFromClient(initMessage, null, out);
            this.targetId = state.getDesired(clientId);
            Reader reader = new Reader(adaptor, in, out);
            reader.start();
            reader.join();
        } catch (IOException | InterruptedException e) {
            System.err.println("클라이언트 연결 종료: " + clientId);
        } finally {
            cleanup();
        }
    }

    private void cleanup() {
        try {
            socket.close();
        } catch (IOException ignored) {}
        state.clientInfoTable.remove(clientId);
        state.desire.remove(clientId);
        System.out.println("ClientHandler 종료: " + clientId);
    }

    class Reader extends Thread{
        private final Adaptor adaptor;
        private final BufferedReader in;
        private final PrintWriter out;

        public Reader(Adaptor adaptor, BufferedReader in, PrintWriter out) {
            this.adaptor = adaptor;
            this.in = in;
            this.out = out;
        }

        @Override
        public void run() {
            String line;
            try {
                // 서버가 종료되거나 상대방이 나간 경우 오류 발생(IOException)
                // 블락킹 방식으로 문자열이 전달될 대까지 대기
                while ((line = in.readLine()) != null) {
                    adaptor.receiveFromClient(line, clientId, out);
                }
            } catch (IOException e) {
                System.err.println("읽기 오류: " + e.getMessage());
            }
        }
    }

}

