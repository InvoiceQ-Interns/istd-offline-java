package gov.jordan.istd.loader;

import org.xml.sax.SAXException;
import javax.xml.XMLConstants;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.validation.SchemaFactory;
import java.io.BufferedReader;
import java.io.File;
import java.util.stream.Collectors;

public class AppResources {
    private final AppResourceLoader resourceLoader;
    private Transformer invoiceXslTransformer;
    private Transformer removeElementXslTransformer;
    private Transformer addUBLElementTransformer;

    private Transformer addQRElementTransformer;

    private Transformer addSignatureElementTransformer;
    private String ublXml;
    private String qrXml;
    private String signatureXml;
    public AppResources() {
        this.resourceLoader = new AppResourceLoader();
        setTransformers();
        setXmlsValues();
    }

    private void setXmlsValues() {
        ublXml = new BufferedReader(resourceLoader.getInputStreamReader("xml/ubl.xml")).lines().collect(Collectors.joining("\n"));
        qrXml = new BufferedReader(resourceLoader.getInputStreamReader("xml/qr.xml")).lines().collect(Collectors.joining("\n"));
        signatureXml = new BufferedReader(resourceLoader.getInputStreamReader("xml/signature.xml")).lines().collect(Collectors.joining("\n"));
    }


    private void setTransformers() {
        try {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            this.invoiceXslTransformer = transformerFactory.newTransformer(resourceLoader.getStreamResource("invoice.xsl"));
            this.removeElementXslTransformer = transformerFactory.newTransformer(resourceLoader.getStreamResource("xslt/removeElements.xsl"));
            touchTransformer(removeElementXslTransformer);
            this.addUBLElementTransformer = transformerFactory.newTransformer(resourceLoader.getStreamResource("xslt/addUBLElement.xsl"));
            touchTransformer(addUBLElementTransformer);
            this.addQRElementTransformer = transformerFactory.newTransformer(resourceLoader.getStreamResource("xslt/addQRElement.xsl"));
            touchTransformer(addQRElementTransformer);
            this.addSignatureElementTransformer = transformerFactory.newTransformer(resourceLoader.getStreamResource("xslt/addSignatureElement.xsl"));
            touchTransformer(addSignatureElementTransformer);

        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    private void touchTransformer(Transformer transformer) {
        transformer.setOutputProperty("encoding", "UTF-8");
        transformer.setOutputProperty("indent", "no");
    }


    public Transformer getInvoiceXslTransformer() {
        return invoiceXslTransformer;
    }

    public Transformer getRemoveElementXslTransformer() {
        return removeElementXslTransformer;
    }

    public Transformer getAddUBLElementTransformer() {
        return addUBLElementTransformer;
    }

    public Transformer getAddQRElementTransformer() {
        return addQRElementTransformer;
    }

    public Transformer getAddSignatureElementTransformer() {
        return addSignatureElementTransformer;
    }

    public String getUblXml() {
        return ublXml;
    }

    public String getQrXml() {
        return qrXml;
    }

    public String getSignatureXml() {
        return signatureXml;
    }

}
