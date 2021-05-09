package javax0.yamaledt;

import javax0.jamal.DocumentConverter;
import org.junit.jupiter.api.Test;

public class TestConvertReadme {
    @Test
    void generateDoc() throws Exception {
        DocumentConverter.convert(".//README.adoc.jam");
    }

    @Test
    void generateReleases() throws Exception {
        DocumentConverter.convert(".//RELEASES.adoc.jam");
    }
}
