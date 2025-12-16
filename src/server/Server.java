package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {

    private static final int THREAD_POOL_SIZE = 20;

    public void execute(String[] args) throws IOException {
        if (args.length != 1) {
            System.err.println("Usage: java server <port>");
            throw new IllegalArgumentException("Wrong number of arguments");
        }
        int port;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            throw new NumberFormatException("Port must be a number");
        }
        // 스레드풀 생성
        ExecutorService pool = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        SharedState sharedState = new SharedState();
        Cryptographer cryptographer = new Cryptographer();
        // 서버 소켓 Open
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server Start! port: " + port);
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getRemoteSocketAddress());
                pool.submit(new ClientHandler(clientSocket, cryptographer, sharedState));
            }
        } finally {
            pool.shutdown();
        }
    }
}
