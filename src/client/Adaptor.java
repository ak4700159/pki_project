package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

public class Adaptor {
    private final Cryptographer cryptographer;
    private final PrintWriter out;

    public Adaptor(Cryptographer cryptographer, PrintWriter out) {
        this.cryptographer = cryptographer;
        this.out = out;
    }

    /* 서버로부터 받을 수 있는 Message Type = @register, @wait, @match, @select, @secure(Reader)
    * register@ :                   공개키 등록 메시지
    * wait@ :                       채팅 상대방이 접속하지 않은 경우, 대기 메시지
    * match@{the other public key}@{signature} :채팅 상대방과 매칭된 경우
    * select@{identity key} :       내가 원하지 않은 상대방으로부터 채팅 요청이 온 경우
    * secure@{message} :            암호화된 채널에서 통신
    * reject@ :                     요청한 상대방과 대화 거절
    * */
    public boolean receiveFromServer(String line) throws RuntimeException {
        String strMessageType = line.split("@")[0];
        MessageType type = extractMessageType(strMessageType);
        if(type.equals(MessageType.REGISTER)){
            sendToServer(null, MessageType.REGISTER);
        } else if(type.equals(MessageType.WAIT)){
            System.out.println("Waiting for client...");
            // 대기
        } else if(type.equals(MessageType.MATCH)){
            String originalMessage = line.split("@")[1];
            String base64Signature = line.split("@")[2];
            // 서버로부터 전달받은 상대방의 인증서 검증
            if(cryptographer.verifyMessage(originalMessage, base64Signature, false)) {
                cryptographer.receivePeerPublicKey(originalMessage);
                return true;
            } else {
                throw new RuntimeException("Server verification failed");
            }
        } else if(type.equals(MessageType.SELECT)){
            // yes or no 응답
            try (BufferedReader console = new BufferedReader(new InputStreamReader(System.in))) {
                String input;
                while ((input = console.readLine()) != null) {
                    input = input.trim();
                    if(input.equalsIgnoreCase("yes") || input.equalsIgnoreCase("no")) {
                        sendToServer(input, MessageType.SELECT);
                        break;
                    }
                }
            } catch (IOException e) {
                System.err.println("쓰기 오류: " + e.getMessage());
            }
        } else if(type.equals(MessageType.REJECT)){
            System.out.println("Rejected your request");
            System.exit(1);
        } else if(type.equals(MessageType.SECURE)){
            // 메시지 해석 후 출력
            String decryptedMessage = cryptographer.decrypt(line.split("@")[1]);
            System.out.println("Message decrypted: " + decryptedMessage);
        } else {
            throw new RuntimeException("Received Wrong Message Type...");
        }
        return false;
    }

    /* 서버에 보낼 수 있는 Message Type = @register, @init, @select, @secure(Writer)
     * init@{my identity key}@{the other client key} : 서버
     * register@{public key} : 등록 메시지
     * select@{response} : 결정 응답
     * secure@{message} : 암호화된 채널에서 통신
     * */
    public void sendToServer(String message, MessageType type) throws RuntimeException {
        if(type.equals(MessageType.REGISTER)){
            String strMyPublicKey = cryptographer.createKeyPair();
            String encryptedMyPublicKey = cryptographer.encrypt(strMyPublicKey, false);
            out.println("register@" + encryptedMyPublicKey);
        } else if(type.equals(MessageType.INIT)){
            String encryptedMessage = cryptographer.encrypt(message, false);
            out.println("init@" + encryptedMessage);
        } else if(type.equals(MessageType.SELECT)){
            String encryptedMessage = cryptographer.encrypt(message, false);
            out.println("select@" + encryptedMessage);
        }else if(type.equals(MessageType.SECURE)){
            String encryptedMessage = cryptographer.encrypt(message, true);
            out.println("secure@" + encryptedMessage);
        } else {
            throw new RuntimeException("Received Wrong Message Type...");
        }
    }

    private MessageType extractMessageType(String type) {
        switch (type) {
            case "register" -> {
                return MessageType.REGISTER;
            }
            case "init" -> {
                return MessageType.INIT;
            }
            case "wait" -> {
                return MessageType.WAIT;
            }
            case "match" -> {
                return MessageType.MATCH;
            }
            case "select" -> {
                return MessageType.SELECT;
            }
            case "secure" -> {
                return MessageType.SECURE;
            }
            case "reject" -> {
                return MessageType.REJECT;
            }
        }
        return MessageType.WRONG;
    }
}
