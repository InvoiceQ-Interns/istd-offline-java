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

import java.util.Objects;

public class ReportSubmitProcessor extends ActionProcessor {
    private final RequesterGeneratorHelper requesterGeneratorHelper = new RequesterGeneratorHelper();
    private String xmlPath = "";
    private String productionCertificateResponsePath = "";
    private String signedXml;
    private CertificateResponse productionCertificateResponse;
    private FotaraClient client;
    private EInvoiceResponse eInvoiceResponse;
    private String outputPath;

    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 3) {
            log.info("Usage: java -jar fotara-sdk.jar submit <signed-xml-path> <production-certificate-response-path> <output-path>");
            return false;
        }
        xmlPath = args[0];
        productionCertificateResponsePath = args[1];
        outputPath = args[2];
        client = new FotaraClient(propertiesManager);
        return true;
    }

    @Override
    protected boolean validateArgs() {
        if(StringUtils.isBlank(outputPath)){
            log.info("Invalid output path");
            return false;
        }
        signedXml = ReaderHelper.readFileAsString(xmlPath);
        if (StringUtils.isBlank(signedXml)) {
            log.info(String.format("Invalid signed xml [%s]", xmlPath));
        }
        String productionCertificateResponseStr = ReaderHelper.readFileAsString(productionCertificateResponsePath);
        if (StringUtils.isBlank(productionCertificateResponseStr)) {
            log.info(String.format("Invalid production certificate response [%s]", productionCertificateResponsePath));
        }
        productionCertificateResponseStr= SecurityUtils.decrypt(productionCertificateResponseStr);
        productionCertificateResponse = JsonUtils.readJson(productionCertificateResponseStr, CertificateResponse.class);
        return Objects.nonNull(productionCertificateResponse)
                && StringUtils.isNotBlank(productionCertificateResponse.getSecret())
                && StringUtils.isNotBlank(productionCertificateResponse.getBinarySecurityToken());
    }

    @Override
    protected boolean process() {
        String jsonBody = requesterGeneratorHelper.generateEInvoiceRequest(signedXml);
        eInvoiceResponse = client.reportInvoice(productionCertificateResponse, jsonBody);
        return Objects.nonNull(eInvoiceResponse) && (
                StringUtils.equalsIgnoreCase(eInvoiceResponse.getStatus(), "CLEARED")
                        || StringUtils.equalsIgnoreCase(eInvoiceResponse.getStatus(), "REPORTED"));
    }

    @Override
    protected boolean output() {
        log.info(String.format("Response [%s]",JsonUtils.toJson(eInvoiceResponse)));
        WriterHelper.writeFile(outputPath,JsonUtils.toJson(eInvoiceResponse));
        return true;
    }
}
