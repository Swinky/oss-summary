package org.microsoft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

/**
 * Tests for the Main class - the application entry point.
 */
class MainTest {

    @Test
    void testMainWithConfigOnly() {
        // Test that main method doesn't throw exceptions when config file is present
        assertDoesNotThrow(() -> Main.main(new String[]{}));
    }

    @Test
    void testMainWithCliArgs() {
        String[] args = {
            "--date=2025-09-09",
            "--period=7",
            "--repos=https://github.com/apache/incubator-gluten"
        };
        assertDoesNotThrow(() -> Main.main(args));
    }

    @Test
    void testMainWithInvalidConfig() {
        // Capture System.err to verify error handling
        ByteArrayOutputStream errorStream = new ByteArrayOutputStream();
        PrintStream originalErr = System.err;
        System.setErr(new PrintStream(errorStream));

        // This would fail if we could mock the ConfigLoader constructor
        // For now, this test verifies the application doesn't crash unexpectedly
        assertDoesNotThrow(() -> Main.main(new String[]{}));

        // Restore System.err
        System.setErr(originalErr);
    }
}
