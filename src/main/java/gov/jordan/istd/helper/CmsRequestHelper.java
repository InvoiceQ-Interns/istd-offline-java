package gov.jordan.istd.helper;

import gov.jordan.istd.dto.CsrConfigDto;
import gov.jordan.istd.dto.CsrResponseDto;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.*;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.pkcs.PKCS10CertificationRequestBuilder;
import org.bouncycastle.pkcs.jcajce.JcaPKCS10CertificationRequestBuilder;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.util.Base64;

public class CmsRequestHelper {

    public static CsrResponseDto createCsr(CsrConfigDto config) throws Exception {
        if (config.getPassword() == null || config.getPassword().trim().isEmpty()) {
            throw new IllegalArgumentException("Password must not be null or empty.");
        }

        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(config.getKeySize());
        KeyPair keyPair = keyGen.generateKeyPair();

        X509Name x509Name = new X509Name(config.getSubjectDn());
        X500Principal subject = new X500Principal(x509Name.getEncoded());

        PKCS10CertificationRequestBuilder builder =
                new JcaPKCS10CertificationRequestBuilder(subject, keyPair.getPublic());

        ExtensionsGenerator extGen = new ExtensionsGenerator();

        addSubjectKeyIdentifier(extGen, keyPair.getPublic());

        if (config.getTemplateOid() != null && !config.getTemplateOid().isEmpty()) {
            addCertificateTemplateExtension(extGen, config.getTemplateOid(),
                    config.getMajorVersion(), config.getMinorVersion());
        }

        Extensions extensions = extGen.generate();
        builder.addAttribute(PKCSObjectIdentifiers.pkcs_9_at_extensionRequest, extensions);

        ContentSigner signer = new JcaContentSignerBuilder("SHA256withRSA").build(keyPair.getPrivate());
        PKCS10CertificationRequest csr = builder.build(signer);

        byte[] pkcs10Der = csr.getEncoded();

        byte[] privateKeyBytes = exportEncryptedPkcs8PrivateKey(keyPair.getPrivate(), config.getPassword());

        byte[] publicKeyBytes = exportPublicKeyWithPasswordProtection(keyPair.getPublic(), config.getPassword());

        return new CsrResponseDto(pkcs10Der, privateKeyBytes, publicKeyBytes);
    }

    private static void addSubjectKeyIdentifier(ExtensionsGenerator extGen, PublicKey publicKey) throws Exception {
        byte[] publicKeyBytes = publicKey.getEncoded();

        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] skiBytes = sha1.digest(publicKeyBytes);

        // Properly encode SKI as ASN.1 OCTET STRING
        DEROctetString skiOctetString = new DEROctetString(skiBytes);
        extGen.addExtension(new ASN1ObjectIdentifier("2.5.29.14"), false, skiOctetString.getEncoded());
    }

    private static void addCertificateTemplateExtension(ExtensionsGenerator extGen, String oid,
                                                        int majorVersion, int minorVersion) throws Exception {
        byte[] templateExtension = buildCertificateTemplateExtension(oid, majorVersion, minorVersion);

        extGen.addExtension(new ASN1ObjectIdentifier("1.3.6.1.4.1.311.21.7"),
                false, templateExtension);
    }

    private static byte[] buildCertificateTemplateExtension(String oid, int majorVersion, int minorVersion) throws Exception {
        ASN1EncodableVector templateVector = new ASN1EncodableVector();

        templateVector.add(new ASN1ObjectIdentifier(oid));

        templateVector.add(new ASN1Integer(majorVersion));

        templateVector.add(new ASN1Integer(minorVersion));

        DERSequence templateSequence = new DERSequence(templateVector);

        return templateSequence.getEncoded();
    }

    private static byte[] exportEncryptedPkcs8PrivateKey(PrivateKey privateKey, String password) throws Exception {
        try {
            SecretKeyFactory secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            byte[] salt = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(salt);

            PBEKeySpec pbeKeySpec = new PBEKeySpec(password.toCharArray(), salt, 100000, 256);
            SecretKey secretKey = secretKeyFactory.generateSecret(pbeKeySpec);

            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");

            byte[] iv = new byte[16];
            SecureRandom.getInstanceStrong().nextBytes(iv);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            SecretKeySpec aesKey = new SecretKeySpec(secretKey.getEncoded(), "AES");
            cipher.init(Cipher.ENCRYPT_MODE, aesKey, ivSpec);

            byte[] privateKeyBytes = privateKey.getEncoded();
            byte[] encryptedBytes = cipher.doFinal(privateKeyBytes);

            ASN1EncodableVector algorithmVector = new ASN1EncodableVector();

            algorithmVector.add(new ASN1ObjectIdentifier("1.2.840.113549.1.5.13"));

            ASN1EncodableVector pbes2Params = new ASN1EncodableVector();

            ASN1EncodableVector kdfVector = new ASN1EncodableVector();
            kdfVector.add(new ASN1ObjectIdentifier("1.2.840.113549.1.5.12"));

            ASN1EncodableVector pbkdf2Params = new ASN1EncodableVector();
            pbkdf2Params.add(new DEROctetString(salt));
            pbkdf2Params.add(new ASN1Integer(100000));
            pbkdf2Params.add(new ASN1Integer(32));

            ASN1EncodableVector hmacVector = new ASN1EncodableVector();
            hmacVector.add(new ASN1ObjectIdentifier("1.2.840.113549.2.9"));
            pbkdf2Params.add(new DERSequence(hmacVector));

            kdfVector.add(new DERSequence(pbkdf2Params));
            pbes2Params.add(new DERSequence(kdfVector));

            ASN1EncodableVector encSchemeVector = new ASN1EncodableVector();
            encSchemeVector.add(new ASN1ObjectIdentifier("2.16.840.1.101.3.4.1.42"));
            encSchemeVector.add(new DEROctetString(iv));
            pbes2Params.add(new DERSequence(encSchemeVector));

            algorithmVector.add(new DERSequence(pbes2Params));

            ASN1EncodableVector encPrivKeyVector = new ASN1EncodableVector();
            encPrivKeyVector.add(new DERSequence(algorithmVector));
            encPrivKeyVector.add(new DEROctetString(encryptedBytes));

            DERSequence encPrivKeyInfo = new DERSequence(encPrivKeyVector);

            return encPrivKeyInfo.getEncoded();

        } catch (Exception e) {
            throw new Exception("Failed to encrypt private key: " + e.getMessage(), e);
        }
    }

    private static byte[] exportPublicKeyWithPasswordProtection(PublicKey publicKey, String password) throws Exception {
        try {
            byte[] publicKeyBytes = publicKey.getEncoded();
            return convertPublicKeyToPem(publicKeyBytes);
        } catch (Exception e) {
            throw new Exception("Failed to format public key: " + e.getMessage(), e);
        }
    }

    private static byte[] convertPublicKeyToPem(byte[] publicKeyDer) {
        String base64PublicKey = Base64.getEncoder().encodeToString(publicKeyDer);
        String pemPublicKey = "-----BEGIN PUBLIC KEY-----\n" +
                insertLineBreaks(base64PublicKey, 64) +
                "\n-----END PUBLIC KEY-----\n";
        return pemPublicKey.getBytes();
    }

    private static String insertLineBreaks(String text, int lineLength) {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < text.length(); i += lineLength) {
            if (i + lineLength < text.length()) {
                stringBuilder.append(text.substring(i, i + lineLength)).append("\n");
            } else {
                stringBuilder.append(text.substring(i)).append("\n");
            }
        }
        return stringBuilder.toString();
    }
}