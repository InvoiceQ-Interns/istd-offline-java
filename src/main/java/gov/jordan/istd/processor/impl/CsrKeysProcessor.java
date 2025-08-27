package gov.jordan.istd.processor.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
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
    private String privateKeyBase64;

    @Override
    protected boolean loadArgs(String[] args) {
        if (args.length != 2) {
            log.info("Usage: java -jar fotara-sdk-1.0.6-jar-with-dependencies.jar generate-csr-keys <directory> <config-file>");
            return false;
        }
        outputDirectory = args[0];
        configFilePath = args[1];

        csrConfigDto = new CsrConfigDto();

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

        // First load standard config from resources (keySize, templateOid, major, minor)
        csrConfigDto.loadStandardConfigFromResources();

        // Then load user config from provided file (organizationIdentifier, organizationUnitName, country)
        String configFile = ReaderHelper.readFileAsString(configFilePath);
        if (StringUtils.isBlank(configFile)) {
            log.info(String.format("Config file [%s] is empty", configFilePath));
            return false;
        }

        // Create a temporary DTO for parsing user config with JSON annotations
        UserConfigDto userConfig = JsonUtils.readJson(configFile, UserConfigDto.class);
        if (Objects.isNull(userConfig)) {
            log.info(String.format("Config file [%s] is invalid", configFilePath));
            return false;
        }

        // Apply user config to csrConfigDto
        if (StringUtils.isNotBlank(userConfig.getCorporateName())) {
            csrConfigDto.setEnName(userConfig.getCorporateName());
        }
        if (StringUtils.isNotBlank(userConfig.getSerialNumber())) {
            csrConfigDto.setSerialNumber(userConfig.getSerialNumber());
        }
        if (StringUtils.isNotBlank(userConfig.getOrganizationIdentifier())) {
            csrConfigDto.setOrganizationIdentifier(userConfig.getOrganizationIdentifier());
        }
        if (StringUtils.isNotBlank(userConfig.getOrganizationUnitName())) {
            csrConfigDto.setOrganizationUnitName(userConfig.getOrganizationUnitName());
        }
        if (StringUtils.isNotBlank(userConfig.getCountry())) {
            csrConfigDto.setCountry(userConfig.getCountry());
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

        if (StringUtils.isBlank(csrConfigDto.getOrganizationIdentifier())) {
            log.info("Organization identifier is required from config file.");
            return false;
        }

        if (StringUtils.isBlank(csrConfigDto.getOrganizationUnitName())) {
            log.info("Organization unit name is required from config file.");
            return false;
        }

        if (StringUtils.isBlank(csrConfigDto.getCountry())) {
            log.info("Country is required from config file.");
            return false;
        }

        if (csrConfigDto.getKeySize() < 1024) {
            log.info("Key size must be at least 1024 bits");
            return false;
        }

        // Validate that DN can be generated
        String dn = csrConfigDto.getSubjectDn();
        if (StringUtils.isBlank(dn)) {
            log.info("Unable to generate DN - missing required fields");
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
            privateKeyBase64 = Base64.getEncoder().encodeToString(csrResponse.getPrivateKeyBytes());

            log.info("Successfully generated CSR and unencrypted private key");
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
        valid = WriterHelper.writeFile(keyFile, SecurityUtils.encrypt(privateKeyBase64)) && valid;
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

    // Helper DTO class for parsing user config JSON
    private static class UserConfigDto {
        @JsonProperty("Common Name")
        private String corporateName;

        @JsonProperty("organization")
        private String organizationIdentifier;

        @JsonProperty("organizationUnitName")
        private String organizationUnitName;

        @JsonProperty("SerialNumber")
        private String serialNumber;

        @JsonProperty("Country (ISO2)")
        private String country;

        public String getCorporateName() { return corporateName; }
        public String getOrganizationIdentifier() { return organizationIdentifier; }
        public String getOrganizationUnitName() { return organizationUnitName; }
        public String getSerialNumber() { return serialNumber; }
        public String getCountry() { return country; }
    }
}
