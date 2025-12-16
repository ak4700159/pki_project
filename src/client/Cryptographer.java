package client;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/*
* Message Format@1 : {Message Type}@{Message Content}@base64([Signature(Hash Function(Message Content))])
* Message Format@2 : {Message Type}@base64(Public Key(Message Content))
*   - 메시지 타입에 따라 암호화 상황이 달라짐, 메시지 길이에도 제한이 있음
*   - register type(To Server)  : 서버의 공개키로 자신의 공개키 암호화
*   - select, match type(From Server) : 서버의 공개키로 메시지 내용 복호화(서명된 인증서 검증)
*   - secure type(From peer) : 자신의 개인키로 복호화
*   - secure type(To peer) : 상대방의 공개키로 암호화
 * */

public class Cryptographer {
    private volatile PrivateKey myPrivateKey;    // 개인키
    private volatile PublicKey myPublicKey;      // 공개키
    private volatile PublicKey serverPublicKey;  // 서버 공개키
    private volatile PublicKey peerPublicKey;    // 상대 공개키

    public Cryptographer() {
        myPublicKey = null;
        myPrivateKey = null;
        serverPublicKey = null;
        peerPublicKey = null;

        try {
            serverPublicKey = loadServerPublicKey();
            myPrivateKey = loadMyPrivateKey();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            // 예외를 던지지 않음, 초기에는 초기화되어 있지 않을 가능성이 큼.
            System.out.println("Not initialize My Private Key. Need to generate key pair.");
        }
    }

    public String decrypt(String encryptedMessage) throws RuntimeException {
        if(myPrivateKey == null) {
            throw new RuntimeException("Not Setting My PrivateKey.");
        }
        Cipher cipher;
        String decryptedText;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, myPrivateKey);
            byte[] base64Decrypted = Base64.getDecoder().decode(encryptedMessage);
            decryptedText = new String(cipher.doFinal(base64Decrypted), StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return decryptedText;
    }

    // 메시지 검증
    public boolean verifyMessage(String message, String base64Signature, boolean fromPeer) {
        PublicKey publicKey = fromPeer ? peerPublicKey : serverPublicKey;
        if (publicKey == null) {
            throw new RuntimeException("PublicKey not initialized");
        }
        try {
            byte[] signatureBytes = Base64.getDecoder().decode(base64Signature);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            return signature.verify(signatureBytes);
        } catch (Exception e) {
            throw new RuntimeException("Verification failed", e);
        }
    }

    public String encrypt(String message, boolean toPeer) throws RuntimeException {
        if ((toPeer ? peerPublicKey : serverPublicKey) == null) throw new RuntimeException("PublicKey not set");
        if (myPrivateKey == null) throw new RuntimeException("PrivateKey not set");
        Cipher cipher;
        String encryptedText;
        try {
            cipher = Cipher.getInstance("RSA");
            if(toPeer) {
                cipher.init(Cipher.ENCRYPT_MODE, peerPublicKey);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            }
            encryptedText = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return encryptedText;
    }

    // base64 인코딩된 공개키 반환
    public String createKeyPair() throws RuntimeException {
        SecureRandom secureRandom = new SecureRandom(); // random number generator(RNG) 알고리즘 사용
        KeyPairGenerator keyPairGenerator; // RSA(1024, 2048), DiffieHellman(1024)
        try {
            keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyPairGenerator.initialize(2048, secureRandom);
        KeyPair keyPair = keyPairGenerator.generateKeyPair(); // generate key pair
        // 생성된 키쌍, 공개키 개인키에 할당
        myPublicKey = keyPair.getPublic();
        myPrivateKey = keyPair.getPrivate();
        // base64 인코딩을 통해 ASCII 문자열로 변환
        String publicKeyStr = Base64.getEncoder().encodeToString(myPublicKey.getEncoded());   // Base64 인코딩
        String privateKeyStr = Base64.getEncoder().encodeToString(myPrivateKey.getEncoded()); // Base64 인코딩
        // 비밀키는 로컬에 저장
        try {
            writePrivateKeyToFile(privateKeyStr);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException(e);
        }
        return publicKeyStr;
    }

    public void receivePeerPublicKey(String strPublicKey) throws RuntimeException {
        // 전달 받은 상대방의 공개키 등록
        try {
            byte[] decoded = Base64.getDecoder().decode(strPublicKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            peerPublicKey = keyFactory.generatePublic(keySpec);
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private PublicKey loadServerPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        // 디코딩된 Server Public key(편의상 구현)
        String base64ServerPublicKey;
        try {
            base64ServerPublicKey = new String(Files.readAllBytes(Paths.get(System.getenv("SERVER_PRIVATE_KEY_PATH"))));
        } catch (IOException e) {
            throw new IOException(e);
        } catch (NullPointerException e) {
            throw new NullPointerException("Not register Environment SERVER_PRIVATE_KEY_PATH");
        }
        byte[] decoded = Base64.getDecoder().decode(base64ServerPublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decoded);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public static PrivateKey loadMyPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException, IOException {
        String base64MyKey;
        try {
            base64MyKey = new String(Files.readAllBytes(Paths.get(System.getenv("PRIVATE_KEY_PATH"))));
        } catch (IOException e) {
            throw new IOException(e);
        } catch (NullPointerException e) {
            throw new NullPointerException("Not register Environment PRIVATE_KEY_PATH");
        }
        byte[] decodedKey = Base64.getDecoder().decode(base64MyKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(decodedKey);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private void writePrivateKeyToFile(String base64Key) throws IOException, NullPointerException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(System.getenv("PRIVATE_KEY_PATH")))) {
            writer.write(base64Key);
        } catch (IOException e) {
            throw new IOException(e);
        } catch (NullPointerException e) {
            throw new NullPointerException("write failed. Not register Environment PRIVATE_KEY_PATH");
        }
    }
}
