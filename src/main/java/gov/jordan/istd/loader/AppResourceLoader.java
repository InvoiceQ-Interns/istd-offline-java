package gov.jordan.istd.loader;


import javax.xml.transform.stream.StreamSource;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;


public class AppResourceLoader {

    public StreamSource getStreamResource(String fileName) {
        StreamSource streamSource = new StreamSource(getClass().getResourceAsStream("classpath:" + fileName));
        if (streamSource.isEmpty()) {
            return readStreamSourceUsingLoader(fileName);
        } else {
            return streamSource;
        }

    }

    private StreamSource readStreamSourceUsingLoader(String fileName) {
        return new StreamSource(getClass().getClassLoader().getResourceAsStream(fileName));
    }

    public InputStreamReader getInputStreamReader(String fileName) {
        try {
            return new InputStreamReader(Objects.requireNonNull(getClass().getResourceAsStream("classpath:" + fileName)), StandardCharsets.UTF_8);
        } catch (RuntimeException e) {
            return readInputStreamReaderUsingLoader(fileName);
        }
    }

    private InputStreamReader readInputStreamReaderUsingLoader(String fileName) {
        return new InputStreamReader(Objects.requireNonNull(getClass().getClassLoader().getResourceAsStream(fileName))
                , StandardCharsets.UTF_8);

    }
}
