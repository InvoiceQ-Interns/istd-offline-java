package gov.jordan.istd.utils;


import org.apache.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.*;
import java.io.StringReader;
import java.io.StringWriter;

public class XmlUtil {

    private final static Logger log=Logger.getLogger("XmlUtil");

    public static String transform(Document document) {
        try {
            Transformer transformer = TransformerFactory.newInstance().newTransformer();
            StringWriter writer = new StringWriter();
            transformer.transform(new DOMSource(document), new StreamResult(writer));
            return writer.getBuffer().toString();
        } catch (Exception e) {
            log.error("something went wrong while transforming xml document, error: " + e.getMessage() ,e);
            return null;
        }
    }

    public static Document transform(String document) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            return factory.newDocumentBuilder().parse(new InputSource(new StringReader(document)));
        } catch (Exception e) {
            log.error("something went wrong while transforming xml document, error: " + e.getMessage() ,e);
            return null;
        }
    }

    public static NodeList evaluateXpath(Document document, String xpathExpression) {
        try {
            XPath xpath = XPathFactory.newInstance().newXPath();
            XPathExpression expr = xpath.compile(xpathExpression);
            return (NodeList) expr.evaluate(document.getDocumentElement(), XPathConstants.NODESET);
        } catch (Exception e) {
            log.error("something went wrong while transforming xml document, error: " + e.getMessage() ,e);
            return null;
        }
    }
}