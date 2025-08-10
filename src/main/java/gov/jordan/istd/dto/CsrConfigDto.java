package gov.jordan.istd.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class CsrConfigDto {
    private String enName;
    private String serialNumber;

    @JsonProperty("keySize")
    private int keySize;
    @JsonProperty("templateOid")
    private String templateOid;
    @JsonProperty("major")
    private int majorVersion;
    @JsonProperty("minor")
    private int minorVersion;

    public CsrConfigDto() {}

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
        if (enName == null || enName.trim().isEmpty() ||
            serialNumber == null || serialNumber.trim().isEmpty()) {
            return null;
        }

        return String.format("CN=%s, O=Government of Jordan, OU=eID, SerialNumber=%s, C=JO",
                           enName.trim(), serialNumber.trim());
    }
}