package gov.jordan.istd.dto;


public class DigitalSignature {
    private String digitalSignature;
    private byte[] xmlHashing;

    public DigitalSignature(String digitalSignature, byte[] xmlHashing) {
        this.digitalSignature = digitalSignature;
        this.xmlHashing = xmlHashing;
    }

    public String getDigitalSignature() {
        return digitalSignature;
    }

    public byte[] getXmlHashing() {
        return xmlHashing;
    }
}
