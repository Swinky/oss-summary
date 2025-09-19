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
    void testAlternativeArgumentNames() {
        // Test the new argument names: --endDate and --repositories
        String[] args = {
            "--endDate=2025-09-15",
            "--period=7",
            "--repositories=apache/spark,apache/kafka"
        };
        UserInput input = new UserInput(args);
        assertEquals("2025-09-15", input.getEndDate());
        assertEquals(7, input.getSummaryPeriod());
        assertEquals(List.of("apache/spark", "apache/kafka"), input.getRepositories());
    }

    @Test
    void testSpaceSeparatedArguments() {
        // Test --key value format instead of --key=value
        String[] args = {
            "--date", "2025-09-20",
            "--period", "14",
            "--repos", "owner/repo1,owner/repo2"
        };
        UserInput input = new UserInput(args);
        assertEquals("2025-09-20", input.getEndDate());
        assertEquals(14, input.getSummaryPeriod());
        assertEquals(List.of("owner/repo1", "owner/repo2"), input.getRepositories());
    }

    @Test
    void testAlternativeSpaceSeparatedArguments() {
        // Test --endDate and --repositories with space separation
        String[] args = {
            "--endDate", "2025-09-25",
            "--period", "21",
            "--repositories", "org/project1,org/project2"
        };
        UserInput input = new UserInput(args);
        assertEquals("2025-09-25", input.getEndDate());
        assertEquals(21, input.getSummaryPeriod());
        assertEquals(List.of("org/project1", "org/project2"), input.getRepositories());
    }

    @Test
    void testMixedArgumentFormats() {
        // Test mixing --key=value and --key value formats
        String[] args = {
            "--date=2025-09-30",
            "--period", "28",
            "--repositories=apache/maven,apache/tomcat"
        };
        UserInput input = new UserInput(args);
        assertEquals("2025-09-30", input.getEndDate());
        assertEquals(28, input.getSummaryPeriod());
        assertEquals(List.of("apache/maven", "apache/tomcat"), input.getRepositories());
    }

    @Test
    void testInvalidPeriodValues() {
        // Test that invalid period values default to 0
        String[] args1 = {"--period=0"};
        UserInput input1 = new UserInput(args1);
        assertEquals(0, input1.getSummaryPeriod());

        String[] args2 = {"--period=-5"};
        UserInput input2 = new UserInput(args2);
        assertEquals(0, input2.getSummaryPeriod());

        String[] args3 = {"--period=abc"};
        UserInput input3 = new UserInput(args3);
        assertEquals(0, input3.getSummaryPeriod());
    }

    @Test
    void testEmptyRepositoryList() {
        // Test empty repository list handling
        String[] args = {"--repos="};
        UserInput input = new UserInput(args);
        assertTrue(input.getRepositories().isEmpty());
    }

    @Test
    void testRepositoryListWithWhitespace() {
        // Test repository list with extra whitespace
        String[] args = {"--repos=  apache/spark  ,  apache/kafka  "};
        UserInput input = new UserInput(args);
        assertEquals(List.of("apache/spark", "apache/kafka"), input.getRepositories());
    }

    @Test
    void testUnknownArguments() {
        // Test that unknown arguments are ignored (logged as warnings)
        String[] args = {
            "--date=2025-09-09",
            "--unknown=value",
            "--period=7"
        };
        UserInput input = new UserInput(args);
        assertEquals("2025-09-09", input.getEndDate());
        assertEquals(7, input.getSummaryPeriod());
        assertNull(input.getRepositories());
    }

    @Test
    void testMissingArguments() {
        String[] args = {};
        UserInput input = new UserInput(args);
        assertNull(input.getEndDate());
        assertEquals(0, input.getSummaryPeriod());
        assertNull(input.getRepositories());
    }

    @Test
    void testPartialArguments() {
        // Test with only some arguments provided
        String[] args = {"--period=5"};
        UserInput input = new UserInput(args);
        assertNull(input.getEndDate());
        assertEquals(5, input.getSummaryPeriod());
        assertNull(input.getRepositories());
    }

    @Test
    void testMissingValueForSpaceSeparatedArgument() {
        // Test when --key is provided but no value follows
        String[] args = {"--date", "--period=7"};
        UserInput input = new UserInput(args);
        assertNull(input.getEndDate()); // Should be null due to missing value
        assertEquals(7, input.getSummaryPeriod());
    }
}
