package gov.jordan.istd.processor.impl;

import gov.jordan.istd.dto.CsrConfigDto;
import gov.jordan.istd.dto.CsrResponseDto;
import gov.jordan.istd.io.ReaderHelper;
import gov.jordan.istd.io.WriterHelper;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.security.SecurityUtils;
import gov.jordan.istd.helper.CmsRequestHelper;
import gov.jordan.istd.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.openssl.PEMWriter;
import org.bouncycastle.util.io.pem.PemObject;

import java.io.StringWriter;
import java.util.Base64;
import java.util.Objects;

import static gov.jordan.istd.utils.pemConversionAndStringUtils.cleanCsrString;

public class CsrKeysProcessor extends ActionProcessor {

    private String outputDirectory = "";
    private String configFilePath = "";
    private CsrConfigDto csrConfigDto;
    private CsrResponseDto csrResponse;
    private String csrPem;
    private String csrDerBase64;
    private String encryptedPrivateKeyBase64;

    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 5) {
            log.info("Usage: java -jar fotara-sdk.jar generate-csr-keys <directory> <en-name> <serial-number> <key-password> <config-file>");

            return false;
        }
        outputDirectory = args[0];
        String enName = args[1];
        String serialNumber = args[2];
        String keyPassword = args[3];
        configFilePath = args[4];

        csrConfigDto = new CsrConfigDto();
        csrConfigDto.setEnName(enName);
        csrConfigDto.setSerialNumber(serialNumber);
        csrConfigDto.setKeyPassword(keyPassword);

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

        return validateCsrConfig();
    }

    private boolean validateCsrConfig() {
        if (StringUtils.isBlank(csrConfigDto.getEnName())) {
            log.info("Please enter a valid Name.");
            return false;
        }

        if (StringUtils.isBlank(csrConfigDto.getSerialNumber())) {
            log.info("Please enter a valid Serial Number.");
            return false;
        }

        if (StringUtils.isBlank(csrConfigDto.getKeyPassword())) {
            log.info("Please enter a password for the private key.");
            return false;
        }

        if (csrConfigDto.getKeySize() < 1024) {
            log.info("Key size must be at least 1024 bits");
            return false;
        }

        return true;
    }

    @Override
    protected boolean process() {
        try {
            String subjectDn = csrConfigDto.getSubjectDn();
            log.info(String.format("Generated DN: %s", subjectDn));
            log.info(String.format("RSA key size: %d", csrConfigDto.getKeySize()));

            if (StringUtils.isNotBlank(csrConfigDto.getTemplateOid())) {
                log.info(String.format("Certificate template OID: %s (v%d.%d)",
                        csrConfigDto.getTemplateOid(),
                        csrConfigDto.getMajorVersion(),
                        csrConfigDto.getMinorVersion()));
            }

            csrResponse = CmsRequestHelper.createCsr(csrConfigDto);

            String csrBase64 = Base64.getEncoder().encodeToString(csrResponse.getCsrDer());

            String cleanedCsr = cleanCsrString(csrBase64);

            csrPem = convertToPem("CERTIFICATE REQUEST", csrResponse.getCsrDer());
            csrDerBase64 = cleanedCsr;
            encryptedPrivateKeyBase64 = Base64.getEncoder().encodeToString(csrResponse.getPrivateKeyBytes());

            log.info("Successfully generated CSR and encrypted private key");
            return true;

        } catch (Exception e) {
            log.error("Failed to generate CSR", e);
            return false;
        }
    }

    @Override
    protected boolean output() {
        String timestamp = java.time.LocalDateTime.now().format(
            java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));

        String commonName = extractCommonNameFromDN(csrConfigDto.getSubjectDn());
        String baseFileName = String.format("%s_%s", commonName, timestamp);

        String csrFile = outputDirectory + "/" + baseFileName + ".csr";
        String keyFile = outputDirectory + "/" + baseFileName + ".key";
        String pubKeyFile = outputDirectory + "/" + baseFileName + ".pub";

        String publicKeyBase64 = Base64.getEncoder().encodeToString(csrResponse.getPublicKeyBytes());

        boolean valid = WriterHelper.writeFile(csrFile, SecurityUtils.encrypt(csrDerBase64));
        valid = WriterHelper.writeFile(keyFile, SecurityUtils.encrypt(encryptedPrivateKeyBase64)) && valid;
        valid = WriterHelper.writeFile(pubKeyFile, SecurityUtils.encrypt(publicKeyBase64)) && valid;

        return valid;
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

    private String convertToPem(String type, byte[] derBytes) throws Exception {
        PemObject pemObject = new PemObject(type, derBytes);
        StringWriter stringWriter = new StringWriter();
        PEMWriter pemWriter = new PEMWriter(stringWriter);
        pemWriter.writeObject(pemObject);
        pemWriter.close();
        return stringWriter.toString();
    }
}
