package gov.jordan.istd.properties.impl;

import gov.jordan.istd.properties.PropertiesManager;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class ProdProperties implements PropertiesManager {
    private static PropertiesManager propertiesManager;
    private final Map<String,String> data=new HashMap<>();

    private ProdProperties() {
        data.put("environment","simulation");
        data.put("fotara.api.url.compliance.csr","https://prod.fotara.com/v1/compliance/csr");
        data.put("fotara.api.url.compliance.invoice","https://prod.fotara.com/v1/compliance/invoice");
        data.put("fotara.api.url.prod.certificate","https://prod.fotara.com/v1/prod/certificate");
        data.put("fotara.api.url.prod.invoice","https://prod.fotara.com/v1/prod/invoice");
        data.put("fotara.certificate.template","PROD_TEMP");
    }

    public static synchronized PropertiesManager getInstance() {
        if(Objects.isNull(propertiesManager)){
            propertiesManager=new ProdProperties();
        }
        return propertiesManager;
    }
    @Override
    public String getProperty(String key) {
        return data.getOrDefault(key,null);
    }
}
