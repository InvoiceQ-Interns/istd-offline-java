package gov.jordan.istd.processor.impl;

import gov.jordan.istd.io.ReaderHelper;
import gov.jordan.istd.processor.ActionProcessor;
import gov.jordan.istd.security.SecurityUtils;
import org.apache.commons.lang3.StringUtils;

public class DecryptProcess extends ActionProcessor {

    private String encryptedFilePath;
    private String encryptedFile="";
    private String decryptedFile="";
    @Override
    protected boolean loadArgs(String[] args) {
        if(args.length!=1){
            log.info("Usage: java -jar fotara-sdk.jar decrypt <encrypted-file-path>");
            return false;
        }
        encryptedFilePath=args[0];
        return true;
    }

    @Override
    protected boolean validateArgs() {
        encryptedFile= ReaderHelper.readFileAsString(encryptedFilePath);
        return StringUtils.isNotBlank(encryptedFile);
    }

    @Override
    protected boolean process() {
        decryptedFile= SecurityUtils.decrypt(encryptedFile);
        return StringUtils.isNotBlank(decryptedFile);
    }

    @Override
    protected boolean output() {
        log.info("Decrypted file: "+decryptedFile);
        return true;
    }
}
