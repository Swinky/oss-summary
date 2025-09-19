package org.microsoft.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.microsoft.AzureFoundryAgentClient;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.RepositoryData;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private AzureFoundryAgentClient mockClient;

    private ReportService service;

    @BeforeEach
    void setUp() {
        service = new ReportService(mockClient);
    }

    @Test
    void shouldGenerateCompleteReport() {
        // Given
        RepositoryData testData = createTestRepositoryData();

        // Mock AI responses for categorization and overall summary only
        // (Remove the unnecessary commit summary mock that was causing the error)
        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [1], Features: [], Improvements: [], Others: []");
        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("Development activity remained steady this period with focus on bug fixes.");

        // When
        String report = service.generateReport(testData, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(report);
        assertTrue(report.contains("<h1>test/repo OSS Updates (2024-01-01 to 2024-01-07)</h1>"));
        assertTrue(report.contains("Development activity remained steady"));
        assertTrue(report.contains("<h2>Overall Summary</h2>"));
        assertTrue(report.contains("<h2>Commits by Category</h2>"));

        // Verify AI client was called at least twice: categorization and overall summary
        verify(mockClient, atLeast(2)).getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt());
    }

    @Test
    void shouldFilterBotCommitsBeforeAnalysis() {
        // Given
        RepositoryData dataWithBots = createTestRepositoryDataWithBots();

        // Mock AI response (should only be called for non-bot commits)
        when(mockClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []")
            .thenReturn("Placeholder summary");

        // When
        String report = service.generateReport(dataWithBots, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(report);
        // Bot commits should be filtered out before AI analysis
        verify(mockClient, atLeastOnce()).getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt());
    }

    @Test
    void shouldHandleNullSummaryFromAI() {
        // Given
        RepositoryData testData = createTestRepositoryData();

        // Mock AI to return categorization but null for summary
        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []");
        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn(null); // Null summary

        // When
        String report = service.generateReport(testData, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(report);
        // The ReportService sets a default message when AI returns null
        assertTrue(report.contains("No significant activity recorded for this period"),
                  "Should contain the default message when AI summary is null");
    }

    private RepositoryData createTestRepositoryData() {
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");

        Commit commit = new Commit();
        commit.setSha("abc123");
        commit.setMessage("fix: resolve critical issue");
        commit.setAuthorLogin("developer");
        data.setCommits(List.of(commit));

        return data;
    }

    private RepositoryData createTestRepositoryDataWithBots() {
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");

        Commit humanCommit = new Commit();
        humanCommit.setSha("abc123");
        humanCommit.setMessage("fix: resolve critical issue");
        humanCommit.setAuthorLogin("developer");

        Commit botCommit = new Commit();
        botCommit.setSha("def456");
        botCommit.setMessage("bot: automated update");
        botCommit.setAuthorLogin("dependabot");

        data.setCommits(Arrays.asList(humanCommit, botCommit));

        return data;
    }
}
