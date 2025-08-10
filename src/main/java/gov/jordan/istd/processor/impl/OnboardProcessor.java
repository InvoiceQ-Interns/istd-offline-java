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
import gov.jordan.istd.utils.JsonUtils;
import gov.jordan.istd.utils.PrivateKeyUtil;
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
        if (args.length != 5) {
            log.info("Usage: java -jar fotara-sdk.jar onboard <otp> <output-directory> <en-name> <serial-number> <config-file>");
            return false;
        }
        if(StringUtils.isBlank(args[0]) || !args[0].matches("\\d{6}")){
            log.info("Invalid otp - must be 6 digits");
            return false;
        }
        otp = args[0];
        outputDirectory = args[1];
        String enName = args[2];
        String serialNumber = args[3];
        configFilePath = args[4];

        csrConfigDto = new CsrConfigDto();
        csrConfigDto.setEnName(enName);
        csrConfigDto.setSerialNumber(serialNumber);

        client = new FotaraClient(propertiesManager);
        return true;
    }

    @Override
    protected boolean validateArgs() {
        if (!ReaderHelper.isDirectoryExists(outputDirectory)) {
            log.info(String.format("Output directory [%s] does not exist", outputDirectory));
            return false;
        }

        if (StringUtils.isBlank(configFilePath)) {
            log.info("Config file path is required");
            return false;
        }

        String configFile = ReaderHelper.readFileAsString(configFilePath);
        if (StringUtils.isBlank(configFile)) {
            log.info(String.format("Config file [%s] is empty", configFilePath));
            return false;
        }

        CsrConfigDto configFromFile = JsonUtils.readJson(configFile, CsrConfigDto.class);
        if (Objects.isNull(configFromFile)) {
            log.info(String.format("Config file [%s] is invalid", configFilePath));
            return false;
        }

        if (configFromFile.getKeySize() > 0) {
            csrConfigDto.setKeySize(configFromFile.getKeySize());
        }
        if (StringUtils.isNotBlank(configFromFile.getTemplateOid())) {
            csrConfigDto.setTemplateOid(configFromFile.getTemplateOid());
        }
        if (configFromFile.getMajorVersion() > 0) {
            csrConfigDto.setMajorVersion(configFromFile.getMajorVersion());
        }
        if (configFromFile.getMinorVersion() >= 0) {
            csrConfigDto.setMinorVersion(configFromFile.getMinorVersion());
        }

        return true;
    }

    @Override
    protected boolean process() {
        CsrKeysProcessor csrKeysProcessor = new CsrKeysProcessor();
        String[] csrArgs = {outputDirectory, csrConfigDto.getEnName(), csrConfigDto.getSerialNumber(), configFilePath};
        boolean isValid = csrKeysProcessor.process(csrArgs, propertiesManager);
        if (!isValid) {
            log.error("Failed to generate CSR and keys");
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
            String timestamp = findLatestTimestamp();
            if (timestamp == null) {
                log.error("No CSR files found in output directory");
                return false;
            }
            
            String commonName = extractCommonNameFromDN(csrConfigDto.getSubjectDn());
            String baseFileName = String.format("%s_%s", commonName, timestamp);
            String csrFile = outputDirectory + "/" + baseFileName + ".csr";
            
            csrEncoded = SecurityUtils.decrypt(ReaderHelper.readFileAsString(csrFile));
            
            String[] serialNumberParts = StringUtils.split(csrConfigDto.getSerialNumber(), "|");
            if (serialNumberParts.length >= 3) {
                deviceId = serialNumberParts[2];
                taxPayerNumber = serialNumberParts[0];
            } else {
                deviceId = "1";
                taxPayerNumber = csrConfigDto.getSerialNumber();
            }
        }catch (Exception e){
            log.error("Failed to load CSR configs",e);
            return false;
        }
        return true;
    }

    private boolean loadPrivateKey() {
        try {
            String timestamp = findLatestTimestamp();
            if (timestamp == null) {
                log.error("No private key files found in output directory");
                return false;
            }
            
            String commonName = extractCommonNameFromDN(csrConfigDto.getSubjectDn());
            String baseFileName = String.format("%s_%s", commonName, timestamp);
            String keyFile = outputDirectory + "/" + baseFileName + ".key";
            
            String privateKeyBase64 = SecurityUtils.decrypt(ReaderHelper.readFileAsString(keyFile));
            privateKey = PrivateKeyUtil.loadPrivateKey(privateKeyBase64, null);

        } catch (Exception e) {
            log.error("Failed to load private key", e);
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
            int counter=0;
            testQueue.add(enrichFile(Objects.requireNonNull(ReaderHelper.readFileFromResource("samples/b2b_invoice.xml")),counter++));
            testQueue.add(enrichFile(Objects.requireNonNull(ReaderHelper.readFileFromResource("samples/b2b_credit.xml")),counter++));
            valid=true;
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
        enrichedFile = enrichedFile.replace("${TAXPAYER_NAME}", csrConfigDto.getEnName());
        enrichedFile= enrichedFile.replace("${DEVICE_ID}", deviceId);
        return enrichedFile;
    }

    private String findLatestTimestamp() {
        try {
            java.io.File dir = new java.io.File(outputDirectory);
            java.io.File[] files = dir.listFiles((d, name) -> name.endsWith(".csr") || name.endsWith(".key"));
            if (files == null || files.length == 0) {
                return null;
            }

            String latestTimestamp = null;
            for (java.io.File file : files) {
                String fileName = file.getName();
                String[] parts = fileName.split("_");
                if (parts.length >= 2) {
                    String timestamp = parts[parts.length - 1].replace(".csr", "").replace(".key", "");
                    if (latestTimestamp == null || timestamp.compareTo(latestTimestamp) > 0) {
                        latestTimestamp = timestamp;
                    }
                }
            }
            return latestTimestamp;
        } catch (Exception e) {
            log.error("Failed to find latest timestamp", e);
            return null;
        }
    }

    private String extractCommonNameFromDN(String subjectDn) {
        try {
            String[] parts = subjectDn.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (trimmed.toUpperCase().startsWith("CN=")) {
                    return trimmed.substring(3).trim().replaceAll("[^a-zA-Z0-9_-]", "_");
                }
            }
            return "CSR";
        } catch (Exception e) {
            return "CSR";
        }
    }
}
