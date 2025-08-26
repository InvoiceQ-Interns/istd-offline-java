package gov.jordan.istd;

import gov.jordan.istd.resolvers.InputResolver;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.properties.PropertiesFactory;
import gov.jordan.istd.properties.PropertiesManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import java.util.Objects;

public class FotaraMain {
    private final Logger logger=Logger.getLogger("FotaraMain");


    public static void main(String[] args) {



        FotaraMain fotaraMain=new FotaraMain();
        fotaraMain.execute(args);
    }

    private void execute(String[] args) {
        if (args.length == 0 || StringUtils.isBlank(args[0])) {
            logger.error("Usage: java -jar fotara-sdk-1.0.6-jar-with-dependencies.jar <action> <args>");
            return;
        }
        String action = args[0];
        PropertiesManager propertiesManager= PropertiesFactory.getPropertiesManager();
        if(Objects.isNull(propertiesManager)){
            return;
        }

        // Fix the params array creation and copying
        String[] params;
        if (args.length > 1) {
            params = new String[args.length - 1];
            System.arraycopy(args, 1, params, 0, args.length - 1);

            // Debug logging to verify the params array
            logger.debug(String.format("Total args: %d, Action: %s, Params count: %d",
                        args.length, action, params.length));
            for (int i = 0; i < params.length; i++) {
                logger.debug(String.format("Param[%d]: %s", i, params[i]));
            }
        } else {
            params = new String[0]; // Empty array if no parameters
        }

        ActionProcessor actionProcessor= InputResolver.resolve(action);
        if(Objects.isNull(actionProcessor)){
            logger.error("Invalid Action");
            return;
        }
        actionProcessor.process(params,propertiesManager);
    }
}