package gov.jordan.istd.utils;

import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter;
import org.bouncycastle.openssl.jcajce.JceOpenSSLPKCS8DecryptorProviderBuilder;
import org.bouncycastle.operator.InputDecryptorProvider;
import org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo;

import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

public class PrivateKeyUtil {

    public static PrivateKey loadPrivateKey(String privateKeyContent, String password) throws Exception {
        if (privateKeyContent.contains("-----BEGIN ENCRYPTED PRIVATE KEY-----")) {
            return loadEncryptedPKCS8PrivateKey(privateKeyContent, password);
        } else if (privateKeyContent.contains("-----BEGIN PRIVATE KEY-----")) {
            return loadUnencryptedPKCS8PrivateKey(privateKeyContent);
        } else if (privateKeyContent.contains("-----BEGIN EC PRIVATE KEY-----")) {
            return loadECPrivateKey(privateKeyContent);
        } else if (privateKeyContent.contains("-----BEGIN RSA PRIVATE KEY-----")) {
            return loadRSAPrivateKey(privateKeyContent);
        } else {
            return loadPrivateKeyFromBase64(privateKeyContent, password);
        }
    }

    private static PrivateKey loadEncryptedPKCS8PrivateKey(String privateKeyContent, String password) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(privateKeyContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) object;
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .build(password.toCharArray());
                return converter.getPrivateKey(encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider));
            } else {
                throw new Exception("Expected encrypted private key but found different format");
            }
        }
    }

    private static PrivateKey loadUnencryptedPKCS8PrivateKey(String privateKeyContent) throws Exception {
        String cleanKey = privateKeyContent
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        byte[] keyBytes = Base64.getDecoder().decode(cleanKey);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);

        try {
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            return keyFactory.generatePrivate(keySpec);
        } catch (Exception e) {
            KeyFactory keyFactory = KeyFactory.getInstance("EC");
            return keyFactory.generatePrivate(keySpec);
        }
    }

    private static PrivateKey loadECPrivateKey(String privateKeyContent) throws Exception {
        String cleanKey = privateKeyContent
                .replace("-----BEGIN EC PRIVATE KEY-----", "")
                .replace("-----END EC PRIVATE KEY-----", "")
                .replaceAll("\\s", "");

        return ECDSAUtil.getPrivateKey(cleanKey);
    }

    private static PrivateKey loadRSAPrivateKey(String privateKeyContent) throws Exception {
        try (PEMParser pemParser = new PEMParser(new StringReader(privateKeyContent))) {
            Object object = pemParser.readObject();
            JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

            if (object instanceof org.bouncycastle.openssl.PEMKeyPair) {
                org.bouncycastle.openssl.PEMKeyPair keyPair = (org.bouncycastle.openssl.PEMKeyPair) object;
                return converter.getPrivateKey(keyPair.getPrivateKeyInfo());
            } else if (object instanceof org.bouncycastle.pkcs.PKCS8EncryptedPrivateKeyInfo) {
                throw new Exception("Encrypted RSA private key detected - use password parameter");
            } else {
                return converter.getPrivateKey((PrivateKeyInfo) object);
            }
        }
    }

    private static PrivateKey loadPrivateKeyFromBase64(String privateKeyBase64, String password) throws Exception {
        byte[] privateKeyBytes = Base64.getDecoder().decode(privateKeyBase64);

        // Try to parse as PEM first
        String pemContent = new String(privateKeyBytes, StandardCharsets.UTF_8);
        if (pemContent.contains("-----BEGIN")) {
            return loadPrivateKey(pemContent, password);
        }

        // Try parsing as raw unencrypted PKCS#8 DER data first (most common for new CSR generation)
        try {
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(privateKeyBytes);

            // Try RSA first (most likely for new CSR keys)
            try {
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePrivate(keySpec);
            } catch (Exception rsaException) {
                // Try EC as fallback
                try {
                    KeyFactory keyFactory = KeyFactory.getInstance("EC");
                    return keyFactory.generatePrivate(keySpec);
                } catch (Exception ecException) {
                    // Continue to try encrypted parsing
                }
            }
        } catch (Exception unencryptedException) {
            // Continue to try encrypted parsing
        }

        // Try parsing as encrypted PKCS#8 DER data
        try {
            PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = new PKCS8EncryptedPrivateKeyInfo(privateKeyBytes);
            if (password != null && !password.isEmpty()) {
                InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                        .build(password.toCharArray());
                JcaPEMKeyConverter converter = new JcaPEMKeyConverter();
                return converter.getPrivateKey(encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider));
            } else {
                throw new Exception("Encrypted private key requires password");
            }
        } catch (Exception encryptedException) {
            // Try PEM parsing as last resort
            try (PEMParser pemParser = new PEMParser(new StringReader(pemContent))) {
                Object object = pemParser.readObject();
                if (object != null) {
                    JcaPEMKeyConverter converter = new JcaPEMKeyConverter();

                    if (object instanceof PKCS8EncryptedPrivateKeyInfo) {
                        PKCS8EncryptedPrivateKeyInfo encryptedPrivateKeyInfo = (PKCS8EncryptedPrivateKeyInfo) object;
                        if (password != null && !password.isEmpty()) {
                            InputDecryptorProvider decryptorProvider = new JceOpenSSLPKCS8DecryptorProviderBuilder()
                                    .build(password.toCharArray());
                            return converter.getPrivateKey(encryptedPrivateKeyInfo.decryptPrivateKeyInfo(decryptorProvider));
                        } else {
                            throw new Exception("Encrypted private key requires password");
                        }
                    } else {
                        return converter.getPrivateKey((PrivateKeyInfo) object);
                    }
                }
            } catch (Exception pemException) {
                throw new Exception("Unable to parse private key. Unencrypted PKCS#8 error, Encrypted PKCS#8 error: " + encryptedException.getMessage() +
                                  ", PEM error: " + pemException.getMessage());
            }
        }

        throw new Exception("Unable to determine private key format after all attempts");
    }
}
