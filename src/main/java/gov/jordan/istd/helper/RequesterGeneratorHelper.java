package gov.jordan.istd.helper;

import gov.jordan.istd.utils.XmlUtil;
import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class RequesterGeneratorHelper {
    Logger log = Logger.getLogger("RequesterGeneratorHelper");

    public String generateEInvoiceRequest(String invoiceHash, String uuid, String singedXml) {
        return String.format("{\"invoiceHash\":\"%s\",\"uuid\":\"%s\",\"invoice\":\"%s\"}",
                invoiceHash, uuid, Base64.getEncoder().encodeToString(singedXml.getBytes(StandardCharsets.UTF_8)));
    }

    public String generateEInvoiceRequest(String singedXml) {
        try {
            Document document = XmlUtil.transform(singedXml);
            String uuidValue = "";
            NodeList uuidNodeList = XmlUtil.evaluateXpath(document, "/Invoice/UUID");
            if (uuidNodeList != null) {
                uuidValue = uuidNodeList.item(0).getFirstChild().getNodeValue();
            }

            NodeList invoiceHashNodeList = XmlUtil.evaluateXpath(document, "/Invoice/UBLExtensions/UBLExtension/ExtensionContent/UBLDocumentSignatures/SignatureInformation/Signature/SignedInfo/Reference/DigestValue");
            String invoiceHashValue = "";
            if (invoiceHashNodeList != null && invoiceHashNodeList.getLength() > 0) {
                invoiceHashValue = invoiceHashNodeList.item(0).getFirstChild().getNodeValue();
            }
            return generateEInvoiceRequest(invoiceHashValue, uuidValue, singedXml);
        } catch (Exception e) {
            log.error("failed to get invoice data ", e);
        }
        return null;
    }
}
