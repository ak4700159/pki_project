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

            Adaptor adaptor = new Adaptor(cryptographer, state);
            // 최초 클라이언트로부터 전달받은 메시지를 바탕으로 클라이언트 정보 주입
            String initMessage = in.readLine();
            this.clientId = adaptor.receiveFromClient(initMessage, null, out);
            this.targetId = state.getDesired(clientId);
            // ClientInfo 등록
            ClientInfo info = state.getClientInfo(clientId);
            info.setOut(out);
            Reader reader = new Reader(adaptor, in, out);
            reader.start();
            reader.join();
        } catch (IOException | InterruptedException e) {
            System.err.println("Client Bye : " + clientId);
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
        System.out.println("============ ClientHandler exited : " + clientId + " ============");
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
                    System.out.println("Received Message : " + line);
                    adaptor.receiveFromClient(line, clientId, out);
                }
            } catch (IOException e) {
                System.err.println("읽기 오류: " + e.getMessage());
            }
        }
    }

}

