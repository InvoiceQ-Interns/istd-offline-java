package gov.jordan.istd.dto;

public class CsrResponseDto {
    private byte[] csrDer;
    private byte[] privateKeyBytes;
    private byte[] publicKeyBytes;

    public CsrResponseDto(byte[] csrDer, byte[] privateKeyBytes, byte[] publicKeyBytes) {
        this.csrDer = csrDer;
        this.privateKeyBytes = privateKeyBytes;
        this.publicKeyBytes = publicKeyBytes;
    }

    public byte[] getCsrDer() {
        return csrDer;
    }

    public byte[] getPrivateKeyBytes() {
        return privateKeyBytes;
    }

    public byte[] getPublicKeyBytes() {
        return publicKeyBytes;
    }
}
