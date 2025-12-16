package server;

import java.io.*;
import java.net.Socket;

public class ClientHandler implements Runnable {

    private final Socket socket;
    private final Cryptographer cryptographer;
    private final SharedState state;
    private String clientId;

    public ClientHandler(Socket socket,
                         Cryptographer cryptographer,
                         SharedState state) {
        this.socket = socket;
        this.cryptographer = cryptographer;
        this.state = state;
    }

    @Override
    public void run() {
        try (
                BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                PrintWriter out = new PrintWriter(socket.getOutputStream(), true)
        ) {
            Adaptor adaptor = new Adaptor(cryptographer, state);
            // 최초 클라이언트로부터 전달받은 메시지를 바탕으로 클라이언트 정보 주입
            Reader reader = new Reader(adaptor, in, out);
            reader.start();
            System.out.println("Reader Start!");
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
            try {
                String initMessage = in.readLine();
                clientId = adaptor.receiveFromClient(initMessage, null, out);
                // ClientInfo 등록
                ClientInfo info = state.getClientInfo(clientId);
                if(info == null) {
                    ClientInfo newInfo = new ClientInfo();
                    newInfo.setOut(out);
                    state.clientInfoTable.put(clientId, newInfo);
                } else {
                    info.setOut(out);
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            String line;
            try {
                // 서버가 종료되거나 상대방이 나간 경우 오류 발생(IOException)
                // 블락킹 방식으로 문자열이 전달될 대까지 대기
                while ((line = in.readLine()) != null) {
                    System.out.println("Received Message : " + line);
                    adaptor.receiveFromClient(line, clientId, out);
                }
            } catch (IOException e) {
                System.out.println("Reader Error : " + e);
            } catch (Exception e) {
                System.out.println("Exception : " + e.getMessage());
            }
        }
    }

}

