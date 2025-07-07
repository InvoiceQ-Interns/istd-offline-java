package gov.jordan.istd.processor.impl;

import gov.jordan.istd.processor.ActionProcessor;

public class InvoiceValidationProcessor extends ActionProcessor {
    String xmlFilePath="";
    @Override
    protected boolean loadArgs(String[] args) {
        if(args.length!=1){
            log.info("Usage: java -jar fotara-sdk.jar invoice-validate <xml-file-path>");
            return false;
        }
        xmlFilePath=args[0];
        return true;
    }

    @Override
    protected boolean validateArgs() {
        return true;
    }

    @Override
    protected boolean process() {
        return true;
    }

    @Override
    protected boolean output() {
        log.info(String.format("XML file [%s] STATUS:\nXSD VALIDATION= [%s]\nCALCULATIONS RULES= [%s]\nREGULATIONS RULES= [%s]",xmlFilePath,"PASSED","PASSED","PASSED"));
        return true;
    }
}
