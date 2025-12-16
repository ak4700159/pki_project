package server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

public class SharedState {
    // <User1 Identity, User2 Identity>, User1이 통신하기를 원하는 사용자가 User2라는 의미
    ConcurrentHashMap<String, String> desire;
    // <User Identity, ClientInfo>, User에 대한 ClientInfo
    ConcurrentHashMap<String, ClientInfo> clientInfoTable;

    public SharedState() {
        desire = new ConcurrentHashMap<>();
        clientInfoTable = new ConcurrentHashMap<>();
        loadPublicKeys();
    }

    public ClientInfo getClientInfo(String id) {
        return clientInfoTable.get(id);
    }

    public String getDesired(String id) {
        return desire.get(id);
    }

    public String getPublicKey(String id) {
        return clientInfoTable.get(id).getPublicKey();
    }

    public MessageType getRecentMessageType(String id) {
        return clientInfoTable.get(id).getRecentMessageType();
    }

    public void connectFirst(String sourceId, String targetId) {
        desire.put(sourceId, targetId);
        if(clientInfoTable.containsKey(targetId)){
            clientInfoTable.put(sourceId, new ClientInfo());
        }
    }

    // 자신과 상대방이 서로를 기다리고 있는 확인
    public Boolean checkMatch(String clientId) {
        String targetId = desire.get(clientId);
        if(desire.get(targetId) == null) {
            return null;
        }
        return Objects.equals(desire.get(targetId), clientId);
    }

    public void setRecentMessage() {

    }

    public void setPublicKey(String clientId, String publicKey) {
        clientInfoTable.get(clientId).setPublicKey(publicKey);
    }

    // 공유 자원 초기화
    private void loadPublicKeys() {
        Path dir = Path.of(System.getenv("REPOSITORY_DIRECTORY"));
        if (!Files.isDirectory(dir)) {
            throw new IllegalStateException("REPOSITORY_DIRECTORY is not a directory");
        }

        try (var stream = Files.list(dir)) {
            stream
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String clientId = path.getFileName().toString();
                        String base64Key = Files.readString(path)
                                .replaceAll("\\s+", "");
                        ClientInfo info = new ClientInfo();
                        info.setPublicKey(base64Key);
                        info.setRecentMessageType(MessageType.NONE);
                        clientInfoTable.put(clientId, info);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to load public key from file: " + path, e);
                    }
                });
        } catch (IOException e) {
            throw new RuntimeException("Failed to read repository directory", e);
        } catch (RuntimeException e) {
            throw new RuntimeException("Failed to load repository directory", e);
        }
    }
}
