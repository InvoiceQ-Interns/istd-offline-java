package gov.jordan.istd.helper;

import gov.jordan.istd.loader.AppResources;
import org.apache.log4j.Logger;
import org.apache.xml.security.Init;
import org.apache.xml.security.c14n.CanonicalizationException;
import org.apache.xml.security.c14n.Canonicalizer;
import org.apache.xml.security.c14n.InvalidCanonicalizerException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

public class HashingHelper {
    private final Logger log=Logger.getLogger("HashingHelper");
    public String getInvoiceHash(String xmlDocument, AppResources appResources) throws Exception {
        Transformer transformer = getTransformer(appResources);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        StreamResult xmlOutput = new StreamResult(byteArrayOutputStream);
        StringReader stringReader = new StringReader(xmlDocument);
        StreamSource streamSource = new StreamSource(stringReader);

        transformer.transform(streamSource, xmlOutput);

        String canonicalizedXml = canonicalizeXml(byteArrayOutputStream.toByteArray());
        byte[] hash = hashStringToBytes(canonicalizedXml);
        Base64.Encoder encoder = Base64.getEncoder();
        return encoder.encodeToString(hash);
    }
    private synchronized String canonicalizeXml(byte[] xmlDocument) throws InvalidCanonicalizerException, CanonicalizationException, ParserConfigurationException, IOException, SAXException {
        Init.init();
        Canonicalizer canon = Canonicalizer.getInstance("http://www.w3.org/2006/12/xml-c14n11");
        return new String(canon.canonicalize(xmlDocument));
    }

    private Transformer getTransformer(AppResources appResources){
        Transformer transformer = appResources.getInvoiceXslTransformer();
        transformer.setOutputProperty("encoding", "UTF-8");
        transformer.setOutputProperty("indent", "no");
        transformer.setOutputProperty("omit-xml-declaration", "yes");
        return transformer;
    }

    private byte[] hashStringToBytes(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            log.error("something went wrong while hashing xml document",e);
        }
        return null;
    }
}
