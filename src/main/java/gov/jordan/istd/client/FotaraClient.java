package gov.jordan.istd.client;

import gov.jordan.istd.dto.CertificateResponse;
import gov.jordan.istd.dto.ComplianceInvoiceResponse;
import gov.jordan.istd.dto.EInvoiceResponse;
import gov.jordan.istd.properties.PropertiesManager;
import gov.jordan.istd.utils.JsonUtils;
import org.apache.log4j.Logger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;

public class FotaraClient {
    private final Logger log=Logger.getLogger("FotaraClient");
    private final PropertiesManager propertiesManager;

    public FotaraClient(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
    }


    public CertificateResponse complianceCsr(String otp, String csrEncoded) {
        try {
            final HttpClient httpClient = HttpClient.newHttpClient();
            final String requestBody = "{ \"csr\":\"" + csrEncoded + "\"}";
            final String url = propertiesManager.getProperty("fotara.api.url.compliance.csr");
            HttpRequest request = getComplianceCsrHttpRequest(otp, url, requestBody);
            log.debug(String.format("compliance CSR [%s]", url));
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Response Code: " + response.statusCode());
            if (response.statusCode() / 100 == 2) {
                return JsonUtils.readJson(response.body(), CertificateResponse.class);
            }
        } catch (Exception e) {
            log.error("failed to compliance CSR ", e);
        }
        return null;
    }

    public ComplianceInvoiceResponse complianceInvoice(CertificateResponse complianceCsrResponse, String jsonBody) {

        final HttpClient httpClient = HttpClient.newHttpClient();
        final String url = propertiesManager.getProperty("fotara.api.url.compliance.invoice");
        final String auth = complianceCsrResponse.getBinarySecurityToken() + ":" + complianceCsrResponse.getBinarySecurityToken();
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.US_ASCII));
        final String authHeader = "Basic " + encodedAuth;
        final HttpRequest request = getDefaultHttpRequest(jsonBody, url, authHeader);
        ComplianceInvoiceResponse response = null;
        try {
            HttpResponse<String> httpResponse = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            int statusCode = httpResponse.statusCode();
            if (statusCode / 100 != 5) {
                response = JsonUtils.readJson(httpResponse.body().replace("\n",""), ComplianceInvoiceResponse.class);
            }
        } catch (Exception e) {
            log.error("failed to compliance invoice ", e);
        }
        return response;
    }

    public CertificateResponse getProdCertificate(CertificateResponse complianceResponse, long requestID) {
        final HttpClient httpClient = HttpClient.newHttpClient();
        final String url = propertiesManager.getProperty("fotara.api.url.prod.certificate");
        final String auth = complianceResponse.getBinarySecurityToken() + ":" + complianceResponse.getBinarySecurityToken();
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.US_ASCII));
        final String authHeader = "Basic " + encodedAuth;
        String jsonBody = "{\"compliance_request_id\":\"" + requestID + "\"}";
        final HttpRequest request = getDefaultHttpRequest(jsonBody, url, authHeader);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.debug("Response Code: " + response.statusCode());
            if (response.statusCode() / 100 == 2) {
                return JsonUtils.readJson(response.body(), CertificateResponse.class);
            }
        } catch (Exception e) {
            log.error("failed to compliance CSR ",e);
        }
        return null;
    }

    public EInvoiceResponse submitInvoice(CertificateResponse productionCertificateResponse, String jsonBody) {
        final HttpClient httpClient = HttpClient.newHttpClient();
        final String url = propertiesManager.getProperty("fotara.api.url.prod.invoice");
        final String auth = productionCertificateResponse.getBinarySecurityToken() + ":" + productionCertificateResponse.getBinarySecurityToken();
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.US_ASCII));
        final String authHeader = "Basic " + encodedAuth;
        final HttpRequest request = getDefaultHttpRequest(jsonBody, url, authHeader);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Code: " + response.statusCode());
            if (response.statusCode() / 100 == 2) {
                return JsonUtils.readJson(response.body(), EInvoiceResponse.class);
            }
        } catch (Exception e) {
            System.out.printf("failed to compliance CSR [%s]\n", e.getMessage());
        }
        return null;
    }

    private HttpRequest getDefaultHttpRequest(String jsonBody, String url, String authHeader) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("Accept-Language", "en")
                .header("Accept", "application/json")
                .header("Accept-Version", "V2")
                .header("Content-Type", "application/json")
                .header("Authorization", authHeader)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                .build();
    }
    private HttpRequest getComplianceCsrHttpRequest(String otp, String url, String requestBody) {
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(60))
                .header("OTP", otp)
                .header("Accept-Language", "en")
                .header("accept", "application/json")
                .header("Accept-Version", "V2")
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
    }

    public EInvoiceResponse reportInvoice(CertificateResponse productionCertificateResponse, String jsonBody) {
        final HttpClient httpClient = HttpClient.newHttpClient();
        final String url = propertiesManager.getProperty("fotara.api.url.prod.report.invoice");
        final String auth = productionCertificateResponse.getBinarySecurityToken() + ":" + productionCertificateResponse.getBinarySecurityToken();
        final String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.US_ASCII));
        final String authHeader = "Basic " + encodedAuth;
        final HttpRequest request = getDefaultHttpRequest(jsonBody, url, authHeader);
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            System.out.println("Response Code: " + response.statusCode());
            if (response.statusCode() / 100 == 2) {
                return JsonUtils.readJson(response.body(), EInvoiceResponse.class);
            }
        } catch (Exception e) {
            System.out.printf("failed to compliance CSR [%s]\n", e.getMessage());
        }
        return null;
    }
}
