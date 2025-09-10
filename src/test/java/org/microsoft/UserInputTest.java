package org.microsoft;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class UserInputTest {
    @Test
    void testParseAllArguments() {
        String[] args = {
            "--date=2025-09-09",
            "--period=10",
            "--repos=https://github.com/apache/incubator-gluten,https://github.com/facebookincubator/velox"
        };
        UserInput input = new UserInput(args);
        assertEquals("2025-09-09", input.getEndDate());
        assertEquals(10, input.getSummaryPeriod());
        assertEquals(List.of(
            "https://github.com/apache/incubator-gluten",
            "https://github.com/facebookincubator/velox"
        ), input.getRepositories());
    }

    @Test
    void testMissingArguments() {
        String[] args = {};
        UserInput input = new UserInput(args);
        assertNull(input.getEndDate());
        assertEquals(0, input.getSummaryPeriod());
        assertNull(input.getRepositories());
    }
}

