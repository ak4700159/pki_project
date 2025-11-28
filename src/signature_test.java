import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.security.*;
import java.util.Base64;


public class signature_test {
    static public void main(String[] args) {
        /* server code */
        /* Key 발급 */
        PublicKey publicKey = null;    // 공개키
        PrivateKey privateKey = null;  // 개인키

        SecureRandom secureRandom = new SecureRandom(); // random number generator(RNG) 알고리즘 사용

        try {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA"); // RSA(1024, 2048), DiffieHellman(1024)
            keyPairGenerator.initialize(1024, secureRandom);
            KeyPair keyPair = keyPairGenerator.generateKeyPair(); // generate key pair

            // 생성된 키쌍, 공개키 개인키에 할당
            publicKey = keyPair.getPublic();
            privateKey = keyPair.getPrivate();

            String publicKeyStr = Base64.getEncoder().encodeToString(publicKey.getEncoded());   // Base64 인코딩
            String privateKeyStr = Base64.getEncoder().encodeToString(privateKey.getEncoded()); // Base64 인코딩

            System.out.println("Public Key: " + publicKeyStr);
            System.out.println(publicKey.getAlgorithm());
            System.out.println(publicKey.getFormat()); // 공개키 표준
            System.out.println(publicKey.toString());

            System.out.println();

            System.out.println("Private Key: " + privateKeyStr);
            System.out.println(privateKey.getAlgorithm());
            System.out.println(privateKey.getFormat()); // 개인키 표준
            System.out.println(privateKey.toString());

            System.out.println("--------------------------------");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        /* 키발급 종료 */

        /* client code */
        // 간단한 암복화 시나리오 (전자서명 시나리오)
        String plainText = "hello world!";
        String encryptedText = "";
        String decryptedText = "";
        try {
            Cipher cipher = Cipher.getInstance("RSA");

            // 개인키 이용 전자서명
            cipher.init(Cipher.ENCRYPT_MODE, privateKey);
            encryptedText = Base64.getEncoder().encodeToString(cipher.doFinal(plainText.getBytes()));
            System.out.println("****encrypt****");
            System.out.println(encryptedText);
            System.out.println("****decrypt****");
            cipher.init(Cipher.DECRYPT_MODE, publicKey);

            // 공개키 이용 복호화
            decryptedText = new String(cipher.doFinal(Base64.getDecoder().decode(encryptedText.getBytes())));
            System.out.println(decryptedText);

        } catch (NoSuchAlgorithmException |
                 NoSuchPaddingException |
                 InvalidKeyException |
                 IllegalBlockSizeException |
                 BadPaddingException e) {
            e.printStackTrace();
        }
    }

}
