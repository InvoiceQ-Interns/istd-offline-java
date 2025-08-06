package gov.jordan.istd.helper;

import com.payneteasy.tlv.BerTag;
import com.payneteasy.tlv.BerTlvBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class QRGeneratorHelper {
    public String generateQrCode(String sellerName, String vatRegistrationNumber, String timeStamp, String invoiceTotal, String vatTotal, String hashedXml, byte[] publicKey, String signature, byte[] certificateSignature) {

        BerTlvBuilder berTlvBuilder = new BerTlvBuilder();

        // Decode the Base64 signature properly
        byte[] signatureBytes;
        try {
            signatureBytes = Base64.getDecoder().decode(signature);
        } catch (Exception e) {
            // If signature is not Base64 encoded, use as is
            signatureBytes = signature.getBytes(StandardCharsets.UTF_8);
        }

        // Build the BER-TLV structure with correct tag ordering
        berTlvBuilder.addText(new BerTag(1), sellerName, StandardCharsets.UTF_8)
                .addText(new BerTag(2), vatRegistrationNumber, StandardCharsets.UTF_8)
                .addText(new BerTag(3), timeStamp, StandardCharsets.UTF_8)
                .addText(new BerTag(4), invoiceTotal, StandardCharsets.UTF_8)
                .addText(new BerTag(5), vatTotal, StandardCharsets.UTF_8)
                .addText(new BerTag(6), hashedXml, StandardCharsets.UTF_8)
                .addBytes(new BerTag(7), signatureBytes)
                .addBytes(new BerTag(8), publicKey)
                .addBytes(new BerTag(9), certificateSignature);

        byte[] bytes = berTlvBuilder.buildArray();
        return Base64.getEncoder().encodeToString(bytes);
    }
}
