package server;

import java.io.IOException;
import java.io.PrintWriter;

public class Adaptor {
    private final Cryptographer cryptographer;
    private final SharedState state;

    public Adaptor(Cryptographer cryptographer, SharedState state) {
        this.cryptographer = cryptographer;
        this.state = state;
    }

    /* 클라이언트로부터 받을 수 있는 Message Type = @register, @init, @secure, @select
    * */
    public String receiveFromClient(String line, String clientId, PrintWriter out) throws RuntimeException {
        try {
            String strMessageType = line.split("@")[0];
            System.out.println("Extract Message Type : " + strMessageType);
            MessageType type = extractMessageType(strMessageType);
            // 등록메시지를 보낸 경우
            if(type.equals(MessageType.REGISTER)){
                try {
                    System.out.println( "["+clientId+"] REGISTER");
                    String base64PublicKey = line.split("@")[1];
                    String clientPublicKey = cryptographer.decrypt(base64PublicKey);
                    state.setPublicKey(clientId, clientPublicKey);
                    cryptographer.saveClientPublicKey(clientId, clientPublicKey);
                    interact(clientId, out, state.getClientInfo(state.getDesired(clientId)).getOut());
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else if(type.equals(MessageType.INIT)){
                String initMessage = cryptographer.decrypt(line.split("@")[1]);
                System.out.println( "INIT " + initMessage);
                String newClientId = initMessage.split("@")[0];
                String targetClientId = initMessage.split("@")[1];
                state.connectFirst(newClientId, targetClientId);
                // 처음 접속한 경우 공개키 등록 요청
                if(state.getClientInfo(newClientId) == null) {
                    sendToClient("", MessageType.REGISTER, out);
                    // 상대방이 접속하지 않은 상황
                } else if(state.getClientInfo(state.getDesired(newClientId)) == null) {
                    sendToClient("", MessageType.WAIT, out);
                } else {
                    interact(newClientId, out, state.getClientInfo(state.getDesired(newClientId)).getOut());
                }
                return newClientId;
            } else if(type.equals(MessageType.SECURE)){
                System.out.println( "[" + clientId + "] -> " + "[" + state.getDesired(clientId) + "] SECURE");
                // 클라이언트 간 공개키로 암호화된 메시지 전송
                sendToClient(line.split("@")[1], MessageType.SECURE, state.getClientInfo(state.getDesired(clientId)).getOut());
            } else if(type.equals(MessageType.SELECT)){
                System.out.println( "["+clientId+"] SELECT");
                String response = cryptographer.decrypt(line.split("@")[1]);
                // yes 또는 no 라고 답변하지 않은 경우.
                if(!(response.equals("yes") || response.equals("no"))) {
                    sendToClient(state.getPublicKey(clientId), MessageType.SELECT, out);
                    // 요청 수락
                } else if(response.equals("yes")) {
                    sendToClient(state.getPublicKey(state.getDesired(clientId)), MessageType.MATCH, out);
                    sendToClient(state.getPublicKey(clientId), MessageType.MATCH, state.getClientInfo(state.getDesired(clientId)).getOut());
                    // 요청 거절
                } else {
                    sendToClient("", MessageType.WAIT, out);
                    sendToClient("", MessageType.REJECT, state.getClientInfo(state.getDesired(clientId)).getOut());
                }
            } else {
                System.out.println( "["+clientId+"] WRONG ... ?");
                throw new RuntimeException("Received Wrong Message Type...");
            }
        } catch (Exception e) {
            throw new RuntimeException("Received Message Error");
        }
        return clientId;
    }

    /* 클라이언트로 보낼 수 있는 Message Type = @register, @wait, @match, @secure, @select, @reject
     * */
    public void sendToClient(String message, MessageType type, PrintWriter out) throws RuntimeException {
        if(type.equals(MessageType.REGISTER)) {
            String encryptedMessage = cryptographer.encrypt(message);
            out.println("register@" + encryptedMessage);
        } else if(type.equals(MessageType.WAIT)) {
            out.println("wait@");
        } else if(type.equals(MessageType.SELECT)) {
            out.println("select@");
        } else if(type.equals(MessageType.MATCH)) {
            String signatureMessage = cryptographer.signMessage(message);
            out.println("match@" + message + "@" + signatureMessage);
        } else if(type.equals(MessageType.SECURE)) {
            out.println("secure@" + message);
        } else if(type.equals(MessageType.REJECT)) {
            out.println("reject@");
        } else {
            throw new RuntimeException("Received Wrong Message Type...");
        }
    }

    private void interact(String clientId, PrintWriter out, PrintWriter targetOut) {
        // 상대방 접속 X
        if(state.checkMatch(clientId) == null) {
            sendToClient("", MessageType.WAIT, out);
        }
        // 상대방이 접속했고 채팅하기를 원할 경우.
        if(state.checkMatch(clientId)) {
            sendToClient(state.getPublicKey(state.getDesired(clientId)), MessageType.MATCH, out);
            sendToClient(state.getPublicKey(clientId), MessageType.MATCH, state.getClientInfo(state.getDesired(clientId)).getOut());
        }
        // 상대방이 접속은 했지만 다른 대화 상대를 지목한 경우.
        else {
            sendToClient("", MessageType.WAIT, out);
            MessageType recentMessageType = state.clientInfoTable.get(state.desire.get(clientId)).getRecentMessageType();
            if(recentMessageType.equals(MessageType.WAIT)) {
                sendToClient("", MessageType.WAIT, targetOut);
            }
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
        }
        return MessageType.WRONG;
    }
}
