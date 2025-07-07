package gov.jordan.istd.processor.impl;

import gov.jordan.istd.client.FotaraClient;
import gov.jordan.istd.dto.CertificateResponse;
import gov.jordan.istd.dto.ComplianceInvoiceResponse;
import gov.jordan.istd.dto.EInvoiceResponse;
import gov.jordan.istd.helper.RequesterGeneratorHelper;
import gov.jordan.istd.helper.SigningHelper;
import gov.jordan.istd.io.ReaderHelper;
import gov.jordan.istd.io.WriterHelper;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.security.SecurityUtils;
import gov.jordan.istd.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Objects;

public class ComplianceSubmitProcessor extends ActionProcessor {
    private final RequesterGeneratorHelper requesterGeneratorHelper = new RequesterGeneratorHelper();
    private String xmlPath = "";
    private String complianceCertificatePath = "";
    private String signedXml;
    private CertificateResponse productionCertificateResponse;
    private FotaraClient client;
    private ComplianceInvoiceResponse eInvoiceResponse;
    private String outputPath;

    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 3) {
            log.info("Usage: java -jar fotara-sdk.jar submit <signed-xml-path> <compliance-certificate-path> <output-path>");
            return false;
        }
        xmlPath = args[0];
        complianceCertificatePath = args[1];
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
        String productionCertificateResponseStr = ReaderHelper.readFileAsString(complianceCertificatePath);
        if (StringUtils.isBlank(productionCertificateResponseStr)) {
            log.info(String.format("Invalid production certificate response [%s]", complianceCertificatePath));
        }
        productionCertificateResponseStr= SecurityUtils.decrypt(productionCertificateResponseStr);
        productionCertificateResponse=new CertificateResponse();
        productionCertificateResponse.setBinarySecurityToken(productionCertificateResponseStr);
        productionCertificateResponse.setSecret(Base64.getEncoder().encodeToString(productionCertificateResponseStr.getBytes(StandardCharsets.UTF_8)));
       return StringUtils.isNotBlank(productionCertificateResponseStr);
    }

    @Override
    protected boolean process() {
        String jsonBody = requesterGeneratorHelper.generateEInvoiceRequest(signedXml);
        eInvoiceResponse = client.complianceInvoice(productionCertificateResponse, jsonBody);
        return Objects.nonNull(eInvoiceResponse);
    }

    @Override
    protected boolean output() {
        log.info(String.format("Response [%s]",JsonUtils.toJson(eInvoiceResponse)));
        WriterHelper.writeFile(outputPath,JsonUtils.toJson(eInvoiceResponse));
        return true;
    }
}
