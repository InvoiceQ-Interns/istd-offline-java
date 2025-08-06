    package gov.jordan.istd.helper;
    
    import gov.jordan.istd.dto.DigitalSignature;
    import org.apache.log4j.Logger;
    
    import java.nio.charset.StandardCharsets;
    import java.security.PrivateKey;
    import java.security.Signature;
    import java.util.Base64;
    
    public class DigitalSignatureHelper {
        private final Logger log=Logger.getLogger("DigitalSignatureHelper");
        public DigitalSignature getDigitalSignature(PrivateKey privateKey, String invoiceHash) {
            byte[] xmlHashingBytes = Base64.getDecoder().decode(invoiceHash.getBytes(StandardCharsets.UTF_8));
            byte[] digitalSignatureBytes = signECDSA(privateKey, xmlHashingBytes);
            return new DigitalSignature(Base64.getEncoder().encodeToString(digitalSignatureBytes), xmlHashingBytes);
        }
        private byte[] signECDSA(PrivateKey privateKey, byte[] messageHash) {
            try {
                Signature signature = Signature.getInstance("SHA256withECDSA");
                signature.initSign(privateKey);
                signature.update(messageHash);
                return signature.sign();
            } catch (Exception e) {
                log.error("something went wrong while signing xml document",e);
                return null;
            }
        }
    }
