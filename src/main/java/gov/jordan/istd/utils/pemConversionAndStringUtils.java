package gov.jordan.istd.utils;

import java.util.Base64;


public class pemConversionAndStringUtils {


    public static String convertPrivateKeyBytesToPem(byte[] privateKeyBytes) {
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN ENCRYPTED PRIVATE KEY-----\n");
        builder.append(insertLineBreaks(Base64.getEncoder().encodeToString(privateKeyBytes), 64));
        builder.append("\n-----END ENCRYPTED PRIVATE KEY-----\n");
        return builder.toString();
    }


    public static String convertCertificateBytesToPem(byte[] certBytes) {
        StringBuilder builder = new StringBuilder();
        builder.append("-----BEGIN CERTIFICATE-----\n");
        builder.append(insertLineBreaks(Base64.getEncoder().encodeToString(certBytes), 64));
        builder.append("\n-----END CERTIFICATE-----\n");
        return builder.toString();
    }


    public static String cleanCsrString(String rawCsr) {
        if (rawCsr == null || rawCsr.trim().isEmpty()) {
            return "";
        }

        return rawCsr
            .replaceAll("(?i)-----BEGIN CERTIFICATE REQUEST-----", "")
            .replaceAll("(?i)-----END CERTIFICATE REQUEST-----", "")
            .replace("\r", "")
            .replace("\n", "")
            .replace("\t", "")
            .trim();
    }

    private static String insertLineBreaks(String input, int lineLength) {
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < input.length(); i += lineLength) {
            result.append(input, i, Math.min(i + lineLength, input.length()));
            if (i + lineLength < input.length()) {
                result.append("\n");
            }
        }
        return result.toString();
    }
}
