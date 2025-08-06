package gov.jordan.istd.helper;

import gov.jordan.istd.dto.DigitalSignature;
import gov.jordan.istd.dto.EInvoiceSigningResults;
import gov.jordan.istd.loader.AppResources;
import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.dom4j.*;
import org.dom4j.io.SAXReader;
import org.xml.sax.SAXException;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Security;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class SigningHelper {
    private final Logger log = Logger.getLogger("SigningHelper");
    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");
    private final HashingHelper hashingHelper;
    private final DigitalSignatureHelper digitalSignatureHelper;
    private final QRGeneratorHelper qrGeneratorHelper;
    private final AppResources appResources;

    public SigningHelper() {
        hashingHelper = new HashingHelper();
        digitalSignatureHelper = new DigitalSignatureHelper();
        qrGeneratorHelper = new QRGeneratorHelper();
        appResources = new AppResources();
    }

    public EInvoiceSigningResults signEInvoice(String xmlDocument, PrivateKey privateKey, String certificateAsString) {
        try {
            String invoiceHash = hashingHelper.getInvoiceHash(xmlDocument, appResources);
            System.out.println( "Invoice Hash: " + invoiceHash);
            Security.addProvider(new BouncyCastleProvider());
            certificateAsString = certificateAsString.replace("-----BEGIN CERTIFICATE-----", "").replace("-----END CERTIFICATE-----", "").replace("\n", "").replace("\r", "");
            byte[] certificateBytes = certificateAsString.getBytes(StandardCharsets.UTF_8);
            final Base64.Decoder decoder = Base64.getMimeDecoder();
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(decoder.decode(certificateBytes));
            byte[] certificateBytesCopy = Arrays.copyOf(certificateBytes, certificateBytes.length);
            String certificateCopy = new String(certificateBytesCopy);
            CertificateFactory certificatefactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificatefactory.generateCertificate(byteArrayInputStream);
            DigitalSignature digitalSignature = digitalSignatureHelper.getDigitalSignature(privateKey, invoiceHash);
            System.out.println("Digital Signature: " + digitalSignature.getDigitalSignature());
            xmlDocument = transformXML(xmlDocument);
            Document document = getXmlDocument(xmlDocument);
            Map<String, String> nameSpacesMap = getNameSpacesMap();
            String certificateHashing = encodeBase64(
                    bytesToHex(hashStringToBytes(certificateAsString.getBytes(StandardCharsets.UTF_8)))
                            .getBytes(StandardCharsets.UTF_8));
            String signedPropertiesHashing = populateSignedSignatureProperties(document, nameSpacesMap,
                    certificateHashing, getCurrentTimestamp(), certificate.getIssuerDN().getName(),
                    certificate.getSerialNumber().toString());

            populateUBLExtensions(document, nameSpacesMap, digitalSignature.getDigitalSignature(),
                    signedPropertiesHashing, encodeBase64(digitalSignature.getXmlHashing()),
                    certificateCopy);

            String qrCode = populateQRCode(document, nameSpacesMap,
                    certificate, digitalSignature.getDigitalSignature(),
                    invoiceHash);

            String uuid = readUUID(xmlDocument);
            return new EInvoiceSigningResults(invoiceHash, digitalSignature.getDigitalSignature(), qrCode, document.asXML(), uuid);
        } catch (Exception e) {
            log.error("Something went wrong while signing the invoice", e);
        }
        return null;
    }

    private Map<String, String> getNameSpacesMap() {
        Map<String, String> nameSpaces = new HashMap<>();
        nameSpaces.put("cac", "urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2");
        nameSpaces.put("cbc", "urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2");
        nameSpaces.put("ext", "urn:oasis:names:specification:ubl:schema:xsd:CommonExtensionComponents-2");
        nameSpaces.put("sig", "urn:oasis:names:specification:ubl:schema:xsd:CommonSignatureComponents-2");
        nameSpaces.put("sac", "urn:oasis:names:specification:ubl:schema:xsd:SignatureAggregateComponents-2");
        nameSpaces.put("sbc", "urn:oasis:names:specification:ubl:schema:xsd:SignatureBasicComponents-2");
        nameSpaces.put("ds", "http://www.w3.org/2000/09/xmldsig#");
        nameSpaces.put("xades", "http://uri.etsi.org/01903/v1.3.2#");
        return nameSpaces;
    }

    private synchronized String transformXML(String xmlDocument) throws TransformerException {
        xmlDocument = transformXml(xmlDocument, appResources.getRemoveElementXslTransformer());
        xmlDocument = transformXml(xmlDocument, appResources.getAddUBLElementTransformer());
        xmlDocument = xmlDocument.replace("UBL-TO-BE-REPLACED", appResources.getUblXml());
        xmlDocument = transformXml(xmlDocument, appResources.getAddQRElementTransformer());
        xmlDocument = xmlDocument.replace("QR-TO-BE-REPLACED", appResources.getQrXml());
        xmlDocument = transformXml(xmlDocument, appResources.getAddSignatureElementTransformer());
        xmlDocument = xmlDocument.replace("SIGN-TO-BE-REPLACED", appResources.getSignatureXml());
        return xmlDocument;
    }

    private synchronized String transformXml(String xmlDocument, Transformer transformer) throws TransformerException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        StreamResult xmlOutput = new StreamResult(bos);
        StringReader stringReader = new StringReader(xmlDocument);
        StreamSource streamSource = new StreamSource(stringReader);
        transformer.transform(streamSource, xmlOutput);
        return bos.toString(StandardCharsets.UTF_8);
    }

    private Document getXmlDocument(String xmlDocument) throws SAXException, DocumentException {
        SAXReader xmlReader = new SAXReader();
        Document doc = xmlReader.read(new ByteArrayInputStream(xmlDocument.getBytes(StandardCharsets.UTF_8)));
        xmlReader.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        xmlReader.setFeature("http://xml.org/sax/features/external-general-entities", false);
        xmlReader.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
        xmlReader.setFeature("http://xml.org/sax/features/namespaces", true);
        return doc;
    }

    private String getNodeXmlValue(Document document, Map<String, String> nameSpaces) {
        XPath xpath = DocumentHelper.createXPath("/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties");
        xpath.setNamespaceURIs(nameSpaces);
        Node node = xpath.selectSingleNode(document);
        return node != null ? node.asXML() : null;
    }

    private String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(255 & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    String encodeBase64(byte[] stringTobBeEncoded) {
        return Base64.getEncoder().encodeToString(stringTobBeEncoded);
    }

    private byte[] hashStringToBytes(byte[] toBeHashed) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        return digest.digest(toBeHashed);
    }

    private String populateSignedSignatureProperties(Document document, Map<String, String> nameSpacesMap, String publicKeyHashing, String signatureTimestamp, String x509IssuerName, String serialNumber) throws NoSuchAlgorithmException {
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningCertificate/xades:Cert/xades:CertDigest/ds:DigestValue", publicKeyHashing);
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningTime", signatureTimestamp);
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningCertificate/xades:Cert/xades:IssuerSerial/ds:X509IssuerName", x509IssuerName);
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:Object/xades:QualifyingProperties/xades:SignedProperties/xades:SignedSignatureProperties/xades:SigningCertificate/xades:Cert/xades:IssuerSerial/ds:X509SerialNumber", serialNumber);
        String signedSignatureElement = getNodeXmlValue(document, nameSpacesMap);
        assert signedSignatureElement != null;
        return encodeBase64(bytesToHex(hashStringToBytes(signedSignatureElement.getBytes(StandardCharsets.UTF_8))).getBytes(StandardCharsets.UTF_8));
    }

    private void populateXmlAttributeValue(Document document, Map<String, String> nameSpaces, String attributeXpath, String newValue) {
        XPath xpath = DocumentHelper.createXPath(attributeXpath);
        xpath.setNamespaceURIs(nameSpaces);
        List<Node> nodes = xpath.selectNodes(document);
        nodes.stream().map(node -> (Element) node)
                .forEach((element) -> element.setText(newValue));
    }

    private void populateUBLExtensions(Document document, Map<String, String> nameSpacesMap, String digitalSignature, String signedPropertiesHashing, String xmlHashing, String certificate) {
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:SignatureValue", digitalSignature);
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:KeyInfo/ds:X509Data/ds:X509Certificate", certificate);
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:SignedInfo/ds:Reference[@URI='#xadesSignedProperties']/ds:DigestValue", signedPropertiesHashing);
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/ext:UBLExtensions/ext:UBLExtension/ext:ExtensionContent/sig:UBLDocumentSignatures/sac:SignatureInformation/ds:Signature/ds:SignedInfo/ds:Reference[@Id='invoiceSignedData']/ds:DigestValue", xmlHashing);
    }

    private String getCurrentTimestamp() {
        LocalDateTime localDateTime = LocalDateTime.now();
        return dateTimeFormatter.format(localDateTime);
    }

    private String populateQRCode(Document document, Map<String, String> nameSpacesMap, X509Certificate certificate, String signature, String hashedXml) throws ParseException {
        String timeStamp, sellerName = getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyLegalEntity/cbc:RegistrationName");
        String vatRegistrationNumber = getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:AccountingSupplierParty/cac:Party/cac:PartyTaxScheme/cbc:CompanyID");
        String invoiceTotal = getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:LegalMonetaryTotal/cbc:PayableAmount");
        String vatTotal = getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cac:TaxTotal/cbc:TaxAmount");
        String issueDate = getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cbc:IssueDate");
        String issueTime = getNodeXmlTextValue(document, nameSpacesMap, "/Invoice/cbc:IssueTime");

        if (issueTime == null) {
            issueTime = "00:00:00";
            log.warn("IssueTime element missing from invoice, using default time: " + issueTime);
        }

        if (issueTime.endsWith("Z")) {
            issueTime = issueTime.replace("Z", "");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            Date dateTimeFormat = sdf.parse(issueDate + "T" + issueTime);
            SimpleDateFormat ksaSdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT + 3"));
            timeStamp = ksaSdf.format(dateTimeFormat);
        } else {
            String stringDateTime = issueDate + "T" + issueTime;
            LocalDateTime dateTimeFormat = LocalDateTime.parse(stringDateTime);
            timeStamp = dateTimeFormatter.format(dateTimeFormat);
        }

        log.info("Final Timestamp: [" + timeStamp + "]");

        String qrCode = qrGeneratorHelper.generateQrCode(sellerName, vatRegistrationNumber, timeStamp, invoiceTotal, vatTotal, hashedXml, certificate
                .getPublicKey().getEncoded(), signature, certificate
                .getSignature());
        populateXmlAttributeValue(document, nameSpacesMap, "/Invoice/cac:AdditionalDocumentReference[cbc:ID='QR']/cac:Attachment/cbc:EmbeddedDocumentBinaryObject", qrCode);
        return qrCode;
    }

    private String getNodeXmlTextValue(Document document, Map<String, String> nameSpaces, String attributeXpath) {
        XPath xpath = DocumentHelper.createXPath(attributeXpath);
        xpath.setNamespaceURIs(nameSpaces);
        Node node = xpath.selectSingleNode(document);
        if (node == null) {
            log.warn("XML node not found for path: " + attributeXpath);
            return null;
        }
        return node.getText();
    }

    public String readUUID(String xmlDocument) throws Exception {
        Document document = getXmlDocument(xmlDocument);
        return getNodeXmlTextValue(document, getNameSpacesMap(), "/Invoice/cbc:UUID");
    }
}
