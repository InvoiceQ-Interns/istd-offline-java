package gov.jordan.istd.properties.impl;

import gov.jordan.istd.properties.PropertiesFactory;
import gov.jordan.istd.properties.PropertiesManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class DevelopmentProperties implements PropertiesManager {
    private static PropertiesManager propertiesManager;
    private final Map<String,String> data=new HashMap<>();
    private DevelopmentProperties() {
        data.put("environment","development");
        data.put("fotara.api.url.compliance.csr","http://localhost:5212/v1/compliance/csr");
        data.put("fotara.api.url.compliance.invoice","http://localhost:5212/v1/compliance/invoice");
        data.put("fotara.api.url.prod.certificate","http://localhost:5212/v1/prod/certificate");
        data.put("fotara.api.url.prod.invoice","http://qpt.invoiceq.com/service/core/invoices/clearance" );
        data.put("fotara.api.url.prod.report.invoice", "http://qpt.invoiceq.com/service/core/invoices/reporting" );
        // data.put("fotara.certificate.template","DEV_TEMP");
        data.put("fotara.certificate.template","NQCSignature");
    }

    public static synchronized PropertiesManager getInstance() {
        if(Objects.isNull(propertiesManager)){
            propertiesManager=new DevelopmentProperties();
        }
        return propertiesManager;
    }
    @Override
    public String getProperty(String key) {
        return data.getOrDefault(key,null);
    }
}
