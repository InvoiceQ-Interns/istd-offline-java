package gov.jordan.istd.security;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;

import java.security.Security;
import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;

import java.util.Base64;
public class SecurityUtils {
    private final static Logger log=Logger.getLogger("SecurityUtils");

    static {
        Security.addProvider(new BouncyCastleProvider());
    }
    private static final String ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final byte[] KEY_BYTES = "0123456789abcdef0123456789abcdef".getBytes(StandardCharsets.UTF_8);
    private static final byte[] IV_BYTES = "abcdef9876543210".getBytes(StandardCharsets.UTF_8);

    public static String encrypt(String plainText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM, new BouncyCastleProvider());
            SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV_BYTES);

            cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));

            return Base64.getEncoder().encodeToString(encrypted);
        }catch (Exception e){
//            log.warn("failed to encrypt data");
            return plainText;
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            Cipher cipher = Cipher.getInstance(ALGORITHM, new BouncyCastleProvider());
            SecretKeySpec keySpec = new SecretKeySpec(KEY_BYTES, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV_BYTES);

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decoded = Base64.getDecoder().decode(encryptedText);
            byte[] decrypted = cipher.doFinal(decoded);

            return new String(decrypted, StandardCharsets.UTF_8);
        }catch (Exception e){
//            log.warn("failed to decrypt data");
            return encryptedText;
        }
    }
}
