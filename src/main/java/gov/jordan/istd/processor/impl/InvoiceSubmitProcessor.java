package gov.jordan.istd.processor.impl;

import gov.jordan.istd.client.FotaraClient;
import gov.jordan.istd.dto.CertificateResponse;
import gov.jordan.istd.dto.EInvoiceResponse;
import gov.jordan.istd.helper.RequesterGeneratorHelper;
import gov.jordan.istd.io.ReaderHelper;
import gov.jordan.istd.io.WriterHelper;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.security.SecurityUtils;
import gov.jordan.istd.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Base64;
import java.util.Objects;

public class InvoiceSubmitProcessor extends ActionProcessor {
    private String xmlPath = "";

    private String signedXml;
    private FotaraClient client;
    private EInvoiceResponse eInvoiceResponse;
    private String clientId;
    private String secretKey;
    private String encodedXml;


    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 3) {
            log.info("Usage: java -jar fotara-sdk-1.0.6-jar-with-dependencies.jar submit-clearance <client-id> <secret-key> <signed-xml-path>");
            return false;
        }
        clientId = args[0];
        secretKey = args[1];
        xmlPath = args[2];
        client = new FotaraClient(propertiesManager);
        return true;
    }

    @Override
    protected boolean validateArgs() {
        if (StringUtils.isBlank(clientId)){
            log.info("Client ID is required");
            return false;
        }

        if (StringUtils.isBlank(secretKey)) {
            log.info("Secret Key is required");
            return false;
        }
        signedXml = ReaderHelper.readFileAsString(xmlPath);
        if (StringUtils.isBlank(signedXml)) {
            log.info(String.format("Invalid signed xml [%s]", xmlPath));
            return false;
        }

       return true;
    }

    @Override
    protected boolean process() {
        encodedXml = Base64.getEncoder().encodeToString(signedXml.getBytes());
        client.submitInvoice(encodedXml, clientId, secretKey);

        return true;
    }

    @Override
    protected boolean output() {
        log.info(String.format("Response [%s]",JsonUtils.toJson(eInvoiceResponse)));
        return true;
    }
}
