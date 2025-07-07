package gov.jordan.istd.helper;

import com.payneteasy.tlv.BerTag;
import com.payneteasy.tlv.BerTlvBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class QRGeneratorHelper {
    public String generateQrCode(String sellerName, String vatRegistrationNumber, String timeStamp, String invoiceTotal, String vatTotal, String hashedXml, byte[] publicKey, String signature, byte[] certificateSignature) {
        BerTlvBuilder berTlvBuilder = new BerTlvBuilder();
        berTlvBuilder.addText(new BerTag(1), sellerName, StandardCharsets.UTF_8)
                .addText(new BerTag(2), vatRegistrationNumber)
                .addText(new BerTag(3), timeStamp)
                .addText(new BerTag(4), invoiceTotal)
                .addText(new BerTag(5), vatTotal)
                .addText(new BerTag(6), hashedXml)
                .addBytes(new BerTag(7), signature.getBytes())
                .addBytes(new BerTag(8), publicKey)
                .addBytes(new BerTag(9), certificateSignature);

        byte[] bytes = berTlvBuilder.buildArray();
        return Base64.getEncoder().encodeToString(bytes);
    }
}
