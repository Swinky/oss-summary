package org.microsoft.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterEach;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.microsoft.AzureFoundryAgentClient;
import org.microsoft.github.data.Commit;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for ParallelCommitSummaryService.
 *
 * Tests verify parallel processing, error handling, timeout behavior,
 * and integration with Azure AI services for single string summaries.
 */
class ParallelCommitSummaryServiceTest {

    @Mock
    private AzureFoundryAgentClient mockAiClient;

    private ParallelCommitSummaryService summaryService;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        summaryService = new ParallelCommitSummaryService(mockAiClient, 4, 30, 120);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (summaryService != null) {
            summaryService.shutdown();
        }
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void generateCommitSummaries_withValidCommits_shouldReturnCleanedSummaries() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(3);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("Fixed null pointer exception in authentication module.")
            .thenReturn("Added user authentication system for enhanced security.")
            .thenReturn("Refactored database layer to improve performance.");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertEquals(3, summaries.size());
        String summary1 = summaries.get("abc123");
        assertNotNull(summary1);
        assertFalse(summary1.contains("\n"), "Summary should be single line");
        assertTrue(summary1.startsWith("Fixed null pointer exception"));
        assertTrue(summary1.length() <= 200, "Summary should be reasonably short");
    }

    @Test
    void generateCommitSummaries_withEmptyList_shouldReturnEmptyMap() {
        // Given
        List<Commit> commits = Collections.emptyList();

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertTrue(summaries.isEmpty());
        verifyNoInteractions(mockAiClient);
    }

    @Test
    void generateCommitSummaries_withNullList_shouldReturnEmptyMap() {
        // Given
        List<Commit> commits = null;

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertTrue(summaries.isEmpty());
        verifyNoInteractions(mockAiClient);
    }

    @Test
    void generateCommitSummaries_withAiFailure_shouldNotIncludeFailedCommits() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(2);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenThrow(new RuntimeException("AI service unavailable"))
            .thenReturn("Working AI response for feature addition");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertEquals(1, summaries.size());
        assertFalse(summaries.containsKey("abc123"), "Failed AI request should not produce a summary");

        String aiSummary = summaries.get("def456");
        assertNotNull(aiSummary);
        assertFalse(aiSummary.contains("\n"));
        assertTrue(aiSummary.contains("Working AI response"));
    }

    @Test
    void generateCommitSummaries_withCategorization_shouldProcessAllCommits() throws Exception {
        // Given
        CommitCategorization categorization = createTestCategorization();
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("Bug fix improves system stability")
            .thenReturn("Feature adds new user capabilities")
            .thenReturn("Performance improvement reduces latency");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(categorization);

        // Then
        assertEquals(3, summaries.size());
        verify(mockAiClient, times(3)).getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt());
    }

    @Test
    void generateCommitSummaries_withLongCommitMessage_shouldTruncateInput() throws Exception {
        // Given
        Commit commit = new Commit();
        commit.setSha("abc123");
        commit.setAuthorLogin("testuser");
        commit.setMessage("This is a very long commit message that exceeds the maximum length limit and should be truncated to save tokens when sending to the AI service. " +
                         "It contains lots of unnecessary details that don't add value to the summary generation process and would waste API tokens.");

        List<Commit> commits = List.of(commit);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("Processed truncated message successfully");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertEquals(1, summaries.size());

        // Verify the prompt sent to AI contains truncated message
        verify(mockAiClient).getSummaryFromAgent(any(), any(), any(), anyInt(), any(), argThat(prompt ->
            prompt.contains("...") && prompt.length() < commit.getMessage().length() + 300), anyInt());
    }

    @Test
    void generateCommitSummaries_withShortAiResponse_shouldKeepAsIs() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(1);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("Simple fix applied");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        String summary = summaries.get("abc123");
        assertNotNull(summary);
        assertFalse(summary.contains("\n"));
        assertEquals("Simple fix applied.", summary);
    }

    @Test
    void generateCommitSummaries_withEmptyAiResponse_shouldNotIncludeCommit() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(1);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertEquals(0, summaries.size(), "Empty AI response should not produce a summary");
    }

    @Test
    void generateCommitSummaries_withMultiLineAiResponse_shouldFlattenToSingleLine() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(1);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("First line of summary\nSecond line of summary\nThird line ignored");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        String summary = summaries.get("abc123");
        assertNotNull(summary);
        assertFalse(summary.contains("\n"), "Should be flattened to single line");
        assertTrue(summary.contains("First line"));
        assertTrue(summary.contains("Second line"));
        assertFalse(summary.contains("Third line"), "Should limit to reasonable content");
    }

    @Test
    void generateCommitSummaries_withPrefixedAiResponse_shouldRemovePrefix() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(1);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("Summary: This commit fixes the authentication bug");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        String summary = summaries.get("abc123");
        assertNotNull(summary);
        assertEquals("This commit fixes the authentication bug.", summary);
        assertFalse(summary.startsWith("Summary:"), "Should remove common AI prefixes");
    }

    @Test
    void generateCommitSummaries_withLongAiResponse_shouldTruncate() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(1);
        String longResponse = "This is a very long AI response that exceeds the reasonable length limit for commit summaries. ".repeat(10);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn(longResponse);

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        String summary = summaries.get("abc123");
        assertNotNull(summary);
        assertTrue(summary.length() <= 200, "Should truncate overly long responses");
        assertTrue(summary.endsWith("...") || summary.endsWith("."), "Should end properly");
    }

    @Test
    void generateCommitSummaries_withVersionBumpCommit_shouldNotIncludeWhenAiFails() throws Exception {
        // Given
        Commit commit = new Commit();
        commit.setSha("abc123");
        commit.setAuthorLogin("testuser");
        commit.setMessage("Bump version to 2.0.1 for release");

        List<Commit> commits = List.of(commit);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn(""); // Empty response

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertEquals(0, summaries.size(), "Version bump commit with empty AI response should not be included");
    }

    @Test
    void generateCommitSummaries_withNullCommitMessage_shouldNotIncludeWhenAiFails() throws Exception {
        // Given
        Commit commit = new Commit();
        commit.setSha("abc123");
        commit.setAuthorLogin("testuser");
        commit.setMessage(null);

        List<Commit> commits = List.of(commit);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn(""); // Simulate empty AI response

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertEquals(0, summaries.size(), "Commit with null message and empty AI response should not be included");
    }

    @Test
    void generateCommitSummaries_withWhitespaceOnlyAiResponse_shouldNotIncludeCommit() throws Exception {
        // Given
        List<Commit> commits = createTestCommits(1);
        when(mockAiClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
            .thenReturn("   \n    \n\t\n");

        // When
        Map<String, String> summaries = summaryService.generateCommitSummaries(commits);

        // Then
        assertEquals(0, summaries.size(), "Whitespace-only AI response should not produce a summary");
    }

    // Helper methods
    private List<Commit> createTestCommits(int count) {
        List<Commit> commits = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            Commit commit = new Commit();
            commit.setSha(i == 0 ? "abc123" : i == 1 ? "def456" : "ghi789" + i);
            commit.setAuthorLogin("testuser" + (i + 1));
            commit.setMessage(String.format("Fix critical bug #%d in module %s", i + 1, i == 0 ? "auth" : i == 1 ? "db" : "ui"));
            commits.add(commit);
        }
        return commits;
    }

    private CommitCategorization createTestCategorization() {
        Commit bugFix = new Commit();
        bugFix.setSha("bug123");
        bugFix.setAuthorLogin("bugfixer");
        bugFix.setMessage("Fix null pointer exception in login");

        Commit feature = new Commit();
        feature.setSha("feat456");
        feature.setAuthorLogin("developer");
        feature.setMessage("Add new user dashboard feature");

        Commit improvement = new Commit();
        improvement.setSha("imp789");
        improvement.setAuthorLogin("optimizer");
        improvement.setMessage("Optimize database query performance");

        return new CommitCategorization(
            List.of(bugFix),
            List.of(feature),
            List.of(improvement),
            List.of()
        );
    }
}
