package server;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
*   -
 * */

/*
private key, public key Load
| 구분         | X509EncodedKeySpec  | PKCS8EncodedKeySpec  |
| ----------   | ------------------- | -------------------- |
| 대상 키      | **공개키 (PublicKey)** | **개인키 (PrivateKey)** |
| 표준          | **X.509**           | **PKCS #8**          |
| Java 복원 결과 | `PublicKey`         | `PrivateKey`         |
| 파일 확장자    | `.pem`, `.crt`      | `.key`, `.pem`       |
 */
public class Cryptographer {
    private PublicKey serverPublicKey;
    private PrivateKey serverPrivateKey;

    public Cryptographer() {
        serverPublicKey = null;
        serverPrivateKey = null;
        try {
            serverPrivateKey = loadServerPrivateKey();
            serverPublicKey = loadServerPublicKey();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (RuntimeException e) {
            // 예외를 던지지 않음, 초기에는 초기화되어 있지 않을 가능성이 큼.
            System.out.println("Server Private Key, Public Key Load Error. Please try again after debugging.");
            System.exit(1);
        }
    }

    public String decrypt(String encryptedMessage) throws RuntimeException {
        if(serverPrivateKey == null) {
            throw new RuntimeException("Not Setting Server PrivateKey.");
        }
        Cipher cipher;
        String decryptedText;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, serverPrivateKey);
            byte[] base64Decrypted = Base64.getDecoder().decode(encryptedMessage);
            decryptedText = new String(cipher.doFinal(base64Decrypted), StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return decryptedText;
    }

    public String encrypt(String message) throws RuntimeException {
        if (serverPublicKey == null) throw new RuntimeException("PublicKey not set");
        Cipher cipher;
        String encryptedText;
        try {
            cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey);
            encryptedText = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return encryptedText;
    }

    // 메시지 서명
    public String signMessage(String message) {
        PrivateKey privateKey = serverPrivateKey;
        if (privateKey == null) {
            throw new RuntimeException("PrivateKey not initialized");
        }
        try {
            // SHA256 알고리즘으로 해싱된 값을 자신의 비밀키로 암호화
            Signature signature = Signature.getInstance("SHA256withRSA");
            // 비밀키 주입
            signature.initSign(privateKey);
            // 메시지 주입(바이트 단위)
            signature.update(message.getBytes(StandardCharsets.UTF_8));
            // 전자서명 추출
            byte[] signedBytes = signature.sign();
            // 전자서명 base64 인코딩
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            throw new RuntimeException("Signing failed", e);
        }
    }

    private PrivateKey loadServerPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String base64PrivateKey;
        try {
            base64PrivateKey = new String(Files.readAllBytes(Paths.get(System.getenv("PRIVATE_KEY_PATH"))));
            base64PrivateKey= base64PrivateKey.replaceAll("-----BEGIN (.*)-----", "")
                                            .replaceAll("-----END (.*)-----", "")
                                            .replaceAll("\\s", "");
        } catch (IOException e) {
            System.out.println("Private Key Not Initialized");
            throw new RuntimeException(e);
        }
        System.out.println("Loading Server Private Key : ");
        System.out.println(base64PrivateKey);
        byte[] keyBytes = Base64.getDecoder().decode(base64PrivateKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePrivate(keySpec);
    }

    private PublicKey loadServerPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String base64PublicKey;
        try {
            base64PublicKey = new String(Files.readAllBytes(Paths.get(System.getenv("PUBLIC_KEY_PATH"))));
            base64PublicKey= base64PublicKey.replaceAll("-----BEGIN (.*)-----", "")
                    .replaceAll("-----END (.*)-----", "")
                    .replaceAll("\\s", "");
        } catch (IOException e) {
            System.out.println("Public Key Not Initialized");
            throw new RuntimeException(e);
        }
        System.out.println("Loading Server Public Key : ");
        System.out.println(base64PublicKey);
        byte[] keyBytes = Base64.getDecoder().decode(base64PublicKey);
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        return keyFactory.generatePublic(keySpec);
    }

    public void saveClientPublicKey(String identityKey, String base64PublicKey) throws IOException, NullPointerException {
        // 파일명: {identity_key}.pem
        String normalized = base64PublicKey.replaceAll("\\s+", "");
        // 64자 단위 줄바꿈
        StringBuilder pemBody = new StringBuilder();
        for (int i = 0; i < normalized.length(); i += 64) {
            pemBody.append(normalized, i, Math.min(i + 64, normalized.length()));
            pemBody.append("\n");
        }
        String pem = "-----BEGIN PUBLIC KEY-----\n"
                + pemBody
                + "-----END PUBLIC KEY-----\n";
        Path savedDir = Path.of(System.getenv("REPOSITORY_DIRECTORY"));
        Path file = savedDir.resolve(identityKey+".pem");
        Files.writeString(file, pem);
    }
}
