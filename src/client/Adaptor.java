package client;

import java.io.PrintWriter;

public class Adaptor {
    private final Cryptographer cryptographer;
    private final PrintWriter out;

    public Adaptor(Cryptographer cryptographer, PrintWriter out) {
        this.cryptographer = cryptographer;
        this.out = out;
    }

    /* 서버로부터 받을 수 있는 Message Type = @register, @wait, @match, @select, @secure(Reader)
    * @register :
    * @wait :
    * @match :
    * @select :
    * @secure :
    * @reject :
    * */
    public void receiveFromServer(String line) throws RuntimeException {
        String strMessageType = line.split("@")[0];
        MessageType type = extractMessageType(strMessageType);
        if(type.equals(MessageType.REGISTER)){
            String strMyPublicKey = cryptographer.createKeyPair();
            String encryptedMyPublicKey = cryptographer.encrypt(strMyPublicKey, false);
            out.println("register@" + encryptedMyPublicKey);
        } else if(type.equals(MessageType.WAIT)){

        } else if(type.equals(MessageType.MATCH)){

        } else if(type.equals(MessageType.SELECT)){

        } else if(type.equals(MessageType.REJECT)){

        } else if(type.equals(MessageType.SECURE)){

        } else {
            throw new RuntimeException("Received Wrong Message Type...");
        }
    }

    /* 서버에 보낼 수 있는 Message Type = @register, @init, @select, @secure(Writer)
     * @init :
     * @register :
     * @select :
     * @secure :
     * */
    public Message sendToServer(String message) throws RuntimeException {

        return new Message(MessageType.WAIT,"", false);
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
