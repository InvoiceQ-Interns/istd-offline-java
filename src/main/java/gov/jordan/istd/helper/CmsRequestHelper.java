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

import javax.security.auth.x500.X500Principal;
import java.security.*;
import java.util.Base64;

public class CmsRequestHelper {

    public static CsrResponseDto createCsr(CsrConfigDto config) throws Exception {
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
        byte[] privateKeyBytes = keyPair.getPrivate().getEncoded();
        byte[] publicKeyBytes = convertPublicKeyToPem(keyPair.getPublic().getEncoded());

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