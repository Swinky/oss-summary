package org.microsoft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Arrays;
import java.util.List;

class ConfigLoaderTest {
    @Test
    void testLoadValidConfig() {
        try {
            ConfigLoader loader = new ConfigLoader("src/test/resources/config.properties");
            assertEquals(7, loader.getSummaryPeriod());
            assertEquals("2025-09-10", loader.getEndDate());
            List<String> expectedRepos = Arrays.asList(
                "apache/incubator-gluten",
                "facebook/velox"
            );
            assertEquals(expectedRepos, loader.getRepositories());
        } catch (java.io.IOException e) {
            fail("IOException thrown: " + e.getMessage());
        }
    }

    @Test
    void testMissingConfigFile() {
        assertThrows(Exception.class, () -> new ConfigLoader("src/test/resources/missing.properties"));
    }
}
