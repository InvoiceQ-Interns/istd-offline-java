package gov.jordan.istd.properties;

import gov.jordan.istd.properties.impl.DevelopmentProperties;
import gov.jordan.istd.properties.impl.ProdProperties;
import gov.jordan.istd.properties.impl.SimulationProperties;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

public class PropertiesFactory {
    private static final Logger logger=Logger.getLogger("PropertiesFactory");
    public static PropertiesManager getPropertiesManager() {
        String env = System.getProperty("env");
        env = "dev";
        if(StringUtils.isBlank(env)){
            logger.error("env param is missing, please provide env param, -Denv=${env} with allowed values [dev,sim,prod]");
            return null;
        }
        switch (env) {
            case "dev":
                return DevelopmentProperties.getInstance();
            case "sim":
                return SimulationProperties.getInstance();
            case "prod":
                return ProdProperties.getInstance();
            default:
                logger.error(String.format("Invalid env param [%s], allowed [dev,sim,prod]", env));
                return null;
        }
    }
}
