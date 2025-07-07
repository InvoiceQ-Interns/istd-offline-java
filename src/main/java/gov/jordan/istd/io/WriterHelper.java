package gov.jordan.istd.io;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.nio.charset.StandardCharsets;

public class WriterHelper {

    private final static Logger log=Logger.getLogger("WriterHelper");
    public static boolean writeFile(String filePath,String content){
        try {
            FileUtils.writeByteArrayToFile(new File(filePath),content.getBytes(StandardCharsets.UTF_8));
        }catch (Exception e){
            log.error("failed to write file on path",e);
            return false;
        }
        return true;
    }
}
