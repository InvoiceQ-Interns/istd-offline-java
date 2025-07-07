package gov.jordan.istd.processor.impl;

import gov.jordan.istd.client.FotaraClient;
import gov.jordan.istd.dto.CertificateResponse;
import gov.jordan.istd.dto.ComplianceInvoiceResponse;
import gov.jordan.istd.dto.CsrConfigDto;
import gov.jordan.istd.dto.EInvoiceSigningResults;
import gov.jordan.istd.helper.RequesterGeneratorHelper;
import gov.jordan.istd.helper.SigningHelper;
import gov.jordan.istd.io.ReaderHelper;
import gov.jordan.istd.io.WriterHelper;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.security.SecurityUtils;
import gov.jordan.istd.utils.ECDSAUtil;
import gov.jordan.istd.utils.JsonUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import java.nio.charset.StandardCharsets;
import java.security.PrivateKey;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ArrayBlockingQueue;

public class OnboardProcessor extends ActionProcessor {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private String outputDirectory = "";
    private String configFilePath = "";
    private String csrEncoded = "";
    private PrivateKey privateKey;
    private String deviceId;
    private String taxPayerNumber;
    private CsrConfigDto csrConfigDto;
    private String otp;
    private String complianceCertificateStr;
    private CertificateResponse complianceCsrResponse;
    private CertificateResponse prodCertificateResponse;
    private final Queue<String> testQueue = new ArrayBlockingQueue<>(10);
    private final Map<String,String> signedXmlMap=new HashMap<>();
    private final SigningHelper signingHelper=new SigningHelper();
    private final RequesterGeneratorHelper requesterGeneratorHelper=new RequesterGeneratorHelper();
    private FotaraClient client;
    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 3) {
            log.info("Usage: java -jar fotara-sdk.jar onboard <otp> <output-directory> <config-path>");
            return false;
        }
        if(StringUtils.isBlank(args[0]) ||  args[0].matches("\\d{6}")){
            log.info("Invalid otp");
            return false;
        }
        otp = args[0];
        outputDirectory = args[1];
        configFilePath = args[2];
        client= new FotaraClient(propertiesManager);
        return true;
    }

    @Override
    protected boolean validateArgs() {
        return true;
    }

    @Override
    protected boolean process() {
        CsrKeysProcessor csrKeysProcessor = new CsrKeysProcessor();
        boolean isValid = csrKeysProcessor.process(new String[]{outputDirectory, configFilePath}, propertiesManager);
        if (!isValid) {
            return false;
        }
        if(!loadPrivateKey()){
            log.info("Failed to load private key");
            return false;
        }
        if(!loadCsrConfigs()){
            log.info("Failed to load CSR configs");
            return false;
        }
        if(!complianceCsr()){
            log.info("Failed to compliance csr");
            return false;
        }
        if(!enrichTestQueue()){
            log.info("Failed to create test xmls");
            return false;
        }
        if(!complianceInvoices()){
            log.info("Failed to compliance invoices");
            return false;
        }
        if(!getProdCertificate()){
            log.info("Failed to get prod certificate");
            return false;
        }
        return true;
    }

    @Override
    protected boolean output() {
        boolean valid=true;
        for(String key:signedXmlMap.keySet()){
            valid= WriterHelper.writeFile(outputDirectory+"/"+key,signedXmlMap.get(key)) && valid;
        }
        String productCertificate = outputDirectory + "/production_csid.cert";
        String productionResponse = outputDirectory + "/production_response.json";
        valid= WriterHelper.writeFile(productionResponse,SecurityUtils.encrypt(JsonUtils.toJson(prodCertificateResponse))) && valid;
        valid= WriterHelper.writeFile(productCertificate,
                SecurityUtils.encrypt(new String(Base64.getDecoder().decode(prodCertificateResponse.getBinarySecurityToken()),StandardCharsets.UTF_8)))
                && valid;
        return valid;
    }

    private boolean loadCsrConfigs() {
        try {
            csrEncoded = SecurityUtils.decrypt(ReaderHelper.readFileAsString(outputDirectory + "/csr.encoded"));
            csrConfigDto = JsonUtils.readJson(ReaderHelper.readFileAsString(configFilePath), CsrConfigDto.class);
            assert csrConfigDto != null;
            String[] serialNumberParts = StringUtils.split(csrConfigDto.getSerialNumber(), "|");
            deviceId = serialNumberParts[2];
            taxPayerNumber = serialNumberParts[0];
        }catch (Exception e){
            log.error("Failed to load CSR configs",e);
            return false;
        }
        return true;
    }

    private boolean loadPrivateKey() {
        try {
            String privateKeyPEM = ReaderHelper.readFileAsString(outputDirectory + "/private.pem");
            assert privateKeyPEM != null;
            privateKeyPEM= SecurityUtils.decrypt(privateKeyPEM);
            String key=privateKeyPEM.replace("-----BEGIN EC PRIVATE KEY-----", "").replaceAll(System.lineSeparator(), "").replace("-----END EC PRIVATE KEY-----", "");
            privateKey= ECDSAUtil.getPrivateKey(key);
        }catch (Exception e){
            log.error("Failed to load private key",e);
            return false;
        }
        return true;
    }

    private boolean getProdCertificate() {
        prodCertificateResponse=client.getProdCertificate(complianceCsrResponse,complianceCsrResponse.getRequestID());
        return Objects.nonNull(prodCertificateResponse) && StringUtils.equalsIgnoreCase(prodCertificateResponse.getDispositionMessage(),"ISSUED");
    }

    private boolean complianceInvoices() {
        boolean valid=true;
        int counter=0;
        while (!testQueue.isEmpty()) {
            String xml=testQueue.poll();
            EInvoiceSigningResults signingResults=signingHelper.signEInvoice(xml,privateKey,complianceCertificateStr);
            String jsonBody= requesterGeneratorHelper.generateEInvoiceRequest(signingResults.getInvoiceHash(),signingResults.getInvoiceUUID(),signingResults.getSignedXml());
            ComplianceInvoiceResponse complianceInvoiceResponse=client.complianceInvoice(complianceCsrResponse,jsonBody);
            if(Objects.isNull(complianceInvoiceResponse) || BooleanUtils.isNotTrue(complianceInvoiceResponse.isValid())){
               log.info(String.format("Failed to compliance invoice [%s] and error [%s]",
                       Base64.getEncoder().encodeToString(xml.getBytes(StandardCharsets.UTF_8)),
                       JsonUtils.toJson(complianceInvoiceResponse)));
                valid=false;
            }else{
                String id="einvoice_test_"+taxPayerNumber+"_"+deviceId+"_"+(counter++)+".xml";
                signedXmlMap.put(id,signingResults.getSignedXml());
            }
        }
        return valid;
    }

    private boolean complianceCsr() {
        complianceCsrResponse=client.complianceCsr(otp,csrEncoded);
        complianceCertificateStr=new String(Base64.getDecoder().decode(complianceCsrResponse.getBinarySecurityToken()),StandardCharsets.UTF_8);
        return Objects.nonNull(complianceCsrResponse) && StringUtils.equalsIgnoreCase(complianceCsrResponse.getDispositionMessage(),"ISSUED");
    }

    private boolean enrichTestQueue() {
        boolean valid=false;
        try {
            String invoiceType = csrConfigDto.getInvoiceType();
            boolean isB2B = invoiceType.charAt(0) == '1';
            boolean isB2C = invoiceType.charAt(1) == '1';
            int counter=0;
            if (isB2B) {
                testQueue.add(enrichFile(Objects.requireNonNull(ReaderHelper.readFileFromResource("samples/b2b_invoice.xml")),counter++));
                testQueue.add(enrichFile(Objects.requireNonNull(ReaderHelper.readFileFromResource("samples/b2b_credit.xml")),counter++));
                valid=true;
            }
            if(isB2C){
                testQueue.add(enrichFile(Objects.requireNonNull(ReaderHelper.readFileFromResource("samples/b2b_invoice.xml")),counter++));
                testQueue.add(enrichFile(Objects.requireNonNull(ReaderHelper.readFileFromResource("samples/b2b_credit.xml")),counter++));
                valid=true;
            }
        }catch (Exception e){
            log.error("Failed to enrich test queue",e);
            return false;
        }
        return valid;
    }

    private String enrichFile(String file,int counter) {
        String id=taxPayerNumber+"_"+deviceId+"_"+counter;
        String orgId=taxPayerNumber+"_"+deviceId+"_"+(counter-1);
        LocalDateTime now = LocalDateTime.now();
        String formattedDate = now.format(formatter);
        String enrichedFile = file.replace("${ID}", id);
        enrichedFile = enrichedFile.replace("${UUID}", UUID.nameUUIDFromBytes(id.getBytes(StandardCharsets.UTF_8)).toString());
        enrichedFile = enrichedFile.replace("${ISSUE_DATE}", formattedDate);
        enrichedFile = enrichedFile.replace("${ORG_ID}", orgId);
        enrichedFile = enrichedFile.replace("${ORG_UUID}", UUID.nameUUIDFromBytes(orgId.getBytes(StandardCharsets.UTF_8)).toString());
        enrichedFile = enrichedFile.replace("${VAT_NUMBER}", taxPayerNumber);
        enrichedFile = enrichedFile.replace("${TAXPAYER_NAME}", csrConfigDto.getCommonName());
        enrichedFile= enrichedFile.replace("${DEVICE_ID}", deviceId);
        return enrichedFile;
    }
}
