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
import java.util.concurrent.atomic.AtomicReference;

/*
* Message Format@1 : {Message Type}@base64([Signature(Hash Function(Message Content))@Message Content])
* Message Format@2 : {Message Type}@base64(Public Key(Message Content))
*   - 메시지 타입에 따라 암호화 상황이 달라짐, 메시지 길이에도 제한이 있음
*   - register type(To Server)  : 서버의 공개키로 자신의 공개키 암호화
*   - select, match type(From Server) : 서버의 공개키로 메시지 내용 복호화(서명된 인증서 검증)
*   - secure type(From peer) : 자신의 개인키로 복호화
*   - secure type(To peer) : 상대방의 공개키로 암호화
 * */

public class Cryptographer {
    private final AtomicReference<PrivateKey> myPrivateKey;    // 개인키
    private final AtomicReference<PublicKey> myPublicKey;      // 공개키
    private final AtomicReference<PublicKey> serverPublicKey;  // 서버 공개키
    private final AtomicReference<PublicKey> peerPublicKey;    // 상대 공개키

    public Cryptographer() {
        myPublicKey = new AtomicReference<PublicKey>(null);
        myPrivateKey = new AtomicReference<PrivateKey>(null);
        serverPublicKey = new AtomicReference<PublicKey>(null);
        peerPublicKey = new AtomicReference<PublicKey>(null);

        try {
            serverPublicKey.set(loadServerPublicKey());
            myPrivateKey.set(loadMyPrivateKey());
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            // 예외를 던지지 않음, 초기에는 초기화되어 있지 않을 가능성이 큼.
            System.out.println("Not initialize My Private Key. Need to generate key pair.");
        }
    }

    public String decrypt(String encryptedMessage) throws RuntimeException {
        if(myPrivateKey.get() == null) {
            throw new RuntimeException("Not Setting My PrivateKey.");
        }
        Cipher cipher;
        String decryptedText;
        try {
            cipher = Cipher.getInstance("RSA");
            // 클라이언트 개인키로 복호화
            cipher.init(Cipher.DECRYPT_MODE, myPrivateKey.get());
            byte[] base64Decrypted = Base64.getDecoder().decode(encryptedMessage);
            decryptedText = new String(cipher.doFinal(base64Decrypted), StandardCharsets.UTF_8);
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return decryptedText;
    }

    // 메시지 검증
    public boolean verifyMessage(String message, String base64Signature, boolean fromPeer) {
        PublicKey publicKey = fromPeer ? peerPublicKey.get() : serverPublicKey.get();
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
        if ((toPeer ? peerPublicKey.get() : serverPublicKey.get()) == null) throw new RuntimeException("PublicKey not set");
        if (myPrivateKey.get() == null) throw new RuntimeException("PrivateKey not set");
        Cipher cipher;
        String encryptedText;
        try {
            cipher = Cipher.getInstance("RSA");
            if(toPeer) {
                cipher.init(Cipher.ENCRYPT_MODE, peerPublicKey.get());
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, serverPublicKey.get());
            }
            encryptedText = Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes(StandardCharsets.UTF_8)));
        } catch (IllegalBlockSizeException | BadPaddingException | InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new RuntimeException(e);
        }
        return encryptedText;
    }

    // 메시지 서명(서버 파트에서 구현)
//    public String signMessage(String message) {
//        PrivateKey privateKey = myPrivateKey.get();
//        if (privateKey == null) {
//            throw new RuntimeException("PrivateKey not initialized");
//        }
//
//        try {
//            // SHA256 알고리즘으로 해싱된 값을 자신의 비밀키로 암호화
//            Signature signature = Signature.getInstance("SHA256withRSA");
//            // 비밀키 주입
//            signature.initSign(privateKey);
//            // 메시지 주입(바이트 단위)
//            signature.update(message.getBytes(StandardCharsets.UTF_8));
//            // 전자서명 추출
//            byte[] signedBytes = signature.sign();
//            // 전자서명 base64 인코딩
//            return Base64.getEncoder().encodeToString(signedBytes);
//        } catch (Exception e) {
//            throw new RuntimeException("Signing failed", e);
//        }
//    }

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
        myPublicKey.set(keyPair.getPublic());
        myPrivateKey.set(keyPair.getPrivate());
        // base64 인코딩을 통해 ASCII 문자열로 변환
        String publicKeyStr = Base64.getEncoder().encodeToString(myPublicKey.get().getEncoded());   // Base64 인코딩
        String privateKeyStr = Base64.getEncoder().encodeToString(myPrivateKey.get().getEncoded()); // Base64 인코딩
        // 비밀키는 로컬에 저장
        try {
            writePrivateKeyToFile(privateKeyStr);
        } catch (IOException | NullPointerException e) {
            throw new RuntimeException(e);
        }
        return publicKeyStr;
    }

    public void receivePeerPublicKey(String base64PeerKey) throws RuntimeException {
        // 전달 받은 상대방의 공개키 등록
        try {
            byte[] decodedKey = Base64.getDecoder().decode(base64PeerKey);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(decodedKey);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            peerPublicKey.set(keyFactory.generatePublic(keySpec));
        } catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }

    private PublicKey loadServerPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        // 디코딩된 Server Public key(편의상 구현)
        byte[] decoded = Base64.getDecoder().decode((
                "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEArIZ1HYktICf/tGvC1dfy\n" +
                "X6mFAclnw4tFCHXZAMEiNm/SF2mIDWJfo05rMUgfZ6SDGlfidvB0vEOwNvfdFaDV\n" +
                "ke03o9XbbXooWi8y+KTkSXn80FsFjzh8jHHIoQl5vMAcOKMInkzoyuZ1verDojz/\n" +
                "OyNFh3syrgvblJq0hz12voz8J53Y/HR3CvZVRAYFjvEUz/p9AYecOzrMTKa3Q9pu\n" +
                "PN+XUaPS18Zrynu/KSNEjk4t1yxJ6zeCxp1bvPK4z1IKu10w6IOsZHXurEN5Jrfw\n" +
                "PpDN6wi4s159zFFNyXCT1xgS67RmUbW5ogSJDCJ6cnjz9v2Ok7yflAYVHUNLdZ4L\n" +
                "YQIDAQAB").replace("\n", ""));
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
            throw new NullPointerException("Not register Environment PRIVATE_KEY_PATH");
        }
    }
}
