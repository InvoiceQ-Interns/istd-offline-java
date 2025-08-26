package gov.jordan.istd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CsrConfigDto {

    @JsonProperty("Corporate Name")
    private String enName;
    @JsonProperty("organization")
    private String organizationIdentifier;
    @JsonProperty("organizationUnitName")
    private String organizationUnitName;
    @JsonProperty("SerialNumber")
    private String serialNumber;
    @JsonProperty("Country (ISO2)")
    private String country;

    @JsonProperty("keySize")
    private int keySize;

    @JsonProperty("templateOid")
    private String templateOid;

    @JsonProperty("major")
    private int majorVersion;

    @JsonProperty("minor")
    private int minorVersion;

    public CsrConfigDto() {}

    public String getOrganizationIdentifier() {
        return organizationIdentifier;
    }

    public void setOrganizationIdentifier(String organizationIdentifier) {
        this.organizationIdentifier = organizationIdentifier;
    }

    public String getOrganizationUnitName() {
        return organizationUnitName;
    }

    public void setOrganizationUnitName(String organizationUnitName) {
        this.organizationUnitName = organizationUnitName;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public String getEnName() {
        return enName;
    }

    public void setEnName(String enName) {
        this.enName = enName;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public int getKeySize() {
        return keySize;
    }

    public void setKeySize(int keySize) {
        this.keySize = keySize;
    }

    public String getTemplateOid() {
        return templateOid;
    }

    public void setTemplateOid(String templateOid) {
        this.templateOid = templateOid;
    }

    public int getMajorVersion() {
        return majorVersion;
    }

    public void setMajorVersion(int majorVersion) {
        this.majorVersion = majorVersion;
    }

    public int getMinorVersion() {
        return minorVersion;
    }

    public void setMinorVersion(int minorVersion) {
        this.minorVersion = minorVersion;
    }


    public String getSubjectDn() {
        if (enName == null || enName.trim().isEmpty()
                || organizationIdentifier == null || organizationIdentifier.trim().isEmpty() ||
                organizationUnitName == null || organizationUnitName.trim().isEmpty() ||
                country == null || country.trim().isEmpty() ||
            serialNumber == null || serialNumber.trim().isEmpty()
        ) {
            return null;
        }

        return String.format("CN=%s, O=%s, OU=%s, SerialNumber=%s, C=%s",
                           enName.trim(), organizationIdentifier.trim(), organizationUnitName.trim(), serialNumber.trim(), country.trim());
    }

    public void loadStandardConfigFromResources() {
        try {
            gov.jordan.istd.loader.AppResourceLoader resourceLoader = new gov.jordan.istd.loader.AppResourceLoader();
            java.io.InputStreamReader reader = resourceLoader.getInputStreamReader("CSRconfig.json");

            StringBuilder content = new StringBuilder();
            char[] buffer = new char[1024];
            int length;
            while ((length = reader.read(buffer)) != -1) {
                content.append(buffer, 0, length);
            }
            reader.close();

            CsrConfigDto standardConfig = gov.jordan.istd.utils.JsonUtils.readJson(content.toString(), CsrConfigDto.class);
            if (standardConfig != null) {
                if (standardConfig.getKeySize() > 0) {
                    this.keySize = standardConfig.getKeySize();
                }
                if (standardConfig.getTemplateOid() != null && !standardConfig.getTemplateOid().trim().isEmpty()) {
                    this.templateOid = standardConfig.getTemplateOid();
                }
                if (standardConfig.getMajorVersion() > 0) {
                    this.majorVersion = standardConfig.getMajorVersion();
                }
                if (standardConfig.getMinorVersion() >= 0) {
                    this.minorVersion = standardConfig.getMinorVersion();
                }
            }
        } catch (Exception e) {
            // Log error but continue - validation will catch missing required values
            System.err.println("Warning: Could not load standard config from resources: " + e.getMessage());
        }
    }
}