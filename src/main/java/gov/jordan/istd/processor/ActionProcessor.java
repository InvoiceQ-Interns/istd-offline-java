package gov.jordan.istd.processor;

import gov.jordan.istd.properties.PropertiesManager;
import org.apache.log4j.Logger;

public abstract class ActionProcessor {

    protected final Logger log=Logger.getLogger("ActionProcessor");
    protected PropertiesManager propertiesManager;
    protected abstract boolean loadArgs(String[] args);
    protected abstract boolean validateArgs();
    protected abstract boolean process();
    protected abstract boolean output();
    public boolean process(String[] args,PropertiesManager propertiesManager) {
        this.propertiesManager=propertiesManager;

        if (!loadArgs(args)) {
            log.error("failed to load arguments");
            return false;
        }
        if (!validateArgs()) {
            log.error("Invalid arguments");
            return false;
        }
        if (!process()) {
            log.error("Failed to process");
            return false;
        }
        if (!output()) {
            log.error("Failed to output");
            return false;
        }
        return true;
    }
}
