package gov.jordan.istd.processor.impl;

import gov.jordan.istd.dto.EInvoiceSigningResults;
import gov.jordan.istd.helper.SigningHelper;
import gov.jordan.istd.io.ReaderHelper;
import gov.jordan.istd.io.WriterHelper;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.security.SecurityUtils;
import gov.jordan.istd.utils.ECDSAUtil;
import org.apache.commons.lang3.StringUtils;

import java.security.PrivateKey;
import java.util.Objects;

public class InvoiceSignProcessor extends ActionProcessor {
    private final SigningHelper signingHelper = new SigningHelper();
    private String xmlPath = "";
    private String privateKeyPath = "";
    private String certificatePath = "";
    private String outputFile = "";
    private PrivateKey privateKey;
    private String xmlFile;
    private String certificateStr;
    private EInvoiceSigningResults signingResults;

    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 4) {
            log.info("Usage: java -jar fotara-sdk.jar invoice-sign <xml-path> <private-key-path> <certificate-path> <output-file>");
            return false;
        }
        xmlPath = args[0];
        privateKeyPath = args[1];
        certificatePath = args[2];
        outputFile = args[3];
        return true;
    }

    @Override
    protected boolean validateArgs() {
        if(StringUtils.isBlank(outputFile)){
            log.info("Invalid output path");
            return false;
        }
        if (!readXmlFile()) return false;
        if (!readPrivateKey()) return false;
        return readCertificate();
    }

    @Override
    protected boolean process() {
        signingResults = signingHelper.signEInvoice(xmlFile, privateKey, certificateStr);
        return Objects.nonNull(signingResults) && StringUtils.isNotBlank(signingResults.getSignedXml());
    }

    @Override
    protected boolean output() {
        log.info(String.format("\ninvoice UUID [%s]\ninvoice Hash [%s]\n invoice QR Code: [%s]\n",signingResults.getInvoiceUUID(), signingResults.getInvoiceHash(), signingResults.getQrCode()));
        return WriterHelper.writeFile(outputFile,signingResults.getSignedXml());
    }

    private boolean readCertificate() {
        certificateStr = ReaderHelper.readFileAsString(certificatePath);
        if(StringUtils.isBlank(certificateStr)){
            log.info(String.format("Certificate file [%s] is empty", certificatePath));
            return false;
        }
        certificateStr= SecurityUtils.decrypt(certificateStr);
        return true;
    }

    private boolean readPrivateKey() {
        String privateKeyFile = ReaderHelper.readFileAsString(privateKeyPath);
        if (StringUtils.isBlank(privateKeyFile)) {
            log.info(String.format("Private key file [%s] is empty", privateKeyPath));
            return false;
        }
        try {
            privateKeyFile=SecurityUtils.decrypt(privateKeyFile);
            String key=privateKeyFile.replace("-----BEGIN EC PRIVATE KEY-----", "").replaceAll(System.lineSeparator(), "").replace("-----END EC PRIVATE KEY-----", "");
            privateKey = ECDSAUtil.getPrivateKey(key);
        } catch (Exception e) {
            log.error(String.format("Failed to read private key [%s]", privateKeyPath),e);
            return false;
        }
        return true;
    }


    private boolean readXmlFile() {
        xmlFile = ReaderHelper.readFileAsString(xmlPath);
        if (StringUtils.isBlank(xmlFile)) {
            log.info(String.format("XML file [%s] is empty", xmlPath));
            return false;
        }
        return true;
    }
}
