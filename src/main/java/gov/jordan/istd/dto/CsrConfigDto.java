package gov.jordan.istd.dto;

public class CsrConfigDto {
    private String enName;
    private String serialNumber;
    private String keyPassword;

    private int keySize = 2048;
    private String templateOid;
    private int majorVersion = 1;
    private int minorVersion = 0;

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

    public String getKeyPassword() {
        return keyPassword;
    }

    public void setKeyPassword(String keyPassword) {
        this.keyPassword = keyPassword;
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

    public String getPassword() {
        return keyPassword;
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