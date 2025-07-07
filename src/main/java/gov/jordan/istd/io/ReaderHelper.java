package gov.jordan.istd.io;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class ReaderHelper {
    private final static Logger log = Logger.getLogger("ReaderHelper");

    public static Boolean isDirectoryExists(String path) {
        return FileUtils.isDirectory(new File(path));
    }

    public static String readFileAsString(String filePath) {
        try {
            return FileUtils.readFileToString(new File(filePath), StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("failed to read file", e);
            return null;

        }
    }

    public static String readFileFromResource(String resourcePath) {
        try {
            return new String(Objects.requireNonNull(ReaderHelper.class.getClassLoader().getResourceAsStream(resourcePath))
                    .readAllBytes(), StandardCharsets.UTF_8);

        } catch (Exception e) {
            log.error("failed to read resource", e);
            return null;
        }
    }
}
