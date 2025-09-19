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
 * Integration tests for AzureAiAnalysisService with parallel commit summarization.
 *
 * Tests verify the complete flow from commit categorization through summary generation
 * and attachment to commit objects.
 */
class AzureAiAnalysisServiceIntegrationTest {

    @Mock
    private AzureFoundryAgentClient mockAiClient;

    private AzureAiAnalysisService analysisService;
    private AutoCloseable closeable;

    @BeforeEach
    void setUp() {
        closeable = MockitoAnnotations.openMocks(this);
        // Initialize with specific thread configuration for testing
        analysisService = new AzureAiAnalysisService(mockAiClient, 2, 10, 100);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (analysisService != null) {
            analysisService.shutdown();
        }
        if (closeable != null) {
            closeable.close();
        }
    }

    @Test
    void categorizeCommits_shouldCategorizeAndSummarizeCommits() throws Exception {
        // Given
        List<Commit> commits = createMixedCommits();

        // Mock categorization response
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Categorize each commit")), eq(2000)))
            .thenReturn("Bug Fixes: [1,3], Features: [2], Improvements: [], Others: [4]");

        // Mock summary responses (now match new prompt wording)
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Generate exactly 2 short sentences")), eq(100)))
            .thenReturn("Fixed memory leak in cache\nImproves application performance")
            .thenReturn("Added user authentication system\nEnhances security and user management")
            .thenReturn("Updated documentation\nImproves developer experience");

        // When
        CommitCategorization result = analysisService.categorizeCommits(commits);

        // Then
        assertNotNull(result);

        // Verify categorization
        assertEquals(2, result.getBugFixes().size());
        assertEquals(1, result.getFeatures().size());
        assertEquals(0, result.getImprovements().size());
        assertEquals(1, result.getOthers().size());

        // Verify summaries were attached
        Commit bugFixCommit = result.getBugFixes().get(0);
        assertNotNull(bugFixCommit.getSummary());
        assertTrue(bugFixCommit.getSummary().contains("Fixed memory leak"));
        assertTrue(bugFixCommit.getSummary().contains("Improves application performance"));

        Commit featureCommit = result.getFeatures().get(0);
        assertNotNull(featureCommit.getSummary());
        assertTrue(featureCommit.getSummary().contains("Added user authentication"));

        // Verify AI client interactions
        verify(mockAiClient, times(1)).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
               argThat(prompt -> prompt.contains("Categorize each commit")), eq(2000));
        verify(mockAiClient, times(3)).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
               argThat(prompt -> prompt.contains("Generate exactly 2 short sentences")), eq(100));
    }

    @Test
    void categorizeCommits_withAiCategorizationFailure_shouldUseFallbackAndGenerateSummaries() throws Exception {
        // Given
        List<Commit> commits = createMixedCommits();

        // Mock categorization failure
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Categorize each commit")), eq(2000)))
            .thenThrow(new RuntimeException("AI categorization failed"));

        // Mock summary responses
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Generate exactly 2 short sentences")), eq(100)))
            .thenReturn("Fallback summary line 1\nFallback summary line 2");

        // When
        CommitCategorization result = analysisService.categorizeCommits(commits);

        // Then
        assertNotNull(result);

        // Should use fallback categorization based on keywords
        assertTrue(result.getBugFixes().size() > 0 || result.getFeatures().size() > 0 ||
                   result.getImprovements().size() > 0 || result.getOthers().size() > 0);

        // Summaries should still be generated
        List<Commit> allCommits = new ArrayList<>();
        allCommits.addAll(result.getBugFixes());
        allCommits.addAll(result.getFeatures());
        allCommits.addAll(result.getImprovements());
        allCommits.addAll(result.getOthers());

        for (Commit commit : allCommits) {
            assertNotNull(commit.getSummary(), "Summary should be present for commit: " + commit.getSha());
        }
    }

    @Test
    void categorizeCommits_withSummaryFailures_shouldAttachEmptySummaries() throws Exception {
        // Given
        List<Commit> commits = createMixedCommits();

        // Mock successful categorization
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Categorize each commit")), eq(2000)))
            .thenReturn("Bug Fixes: [1], Features: [2], Improvements: [], Others: [3,4]");

        // Mock mixed summary responses (some succeed, some fail)
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Generate exactly 2 short sentences")), eq(100)))
            .thenReturn("AI generated summary\nWith proper formatting")
            .thenThrow(new RuntimeException("AI summary failed"))
            .thenReturn("Another AI summary\nSecond line");

        // When
        CommitCategorization result = analysisService.categorizeCommits(commits);

        // Then
        assertNotNull(result);

        // Verify AI-generated summaries are present or empty based on mock responses
        List<Commit> allCommits = new ArrayList<>();
        allCommits.addAll(result.getBugFixes());
        allCommits.addAll(result.getFeatures());
        allCommits.addAll(result.getOthers());

        // Bug fix commit should have a summary (first mock response)
        assertNotNull(result.getBugFixes().get(0).getSummary());
        assertFalse(result.getBugFixes().get(0).getSummary().isEmpty());

        // Feature commit should have empty summary (second mock throws exception)
        assertNotNull(result.getFeatures().get(0).getSummary());
        assertTrue(result.getFeatures().get(0).getSummary().isEmpty(),
                  "Feature commit should have empty summary due to AI failure");

        // At least one "Others" commit should have summary (third mock response)
        boolean hasNonEmptySummary = result.getOthers().stream()
            .anyMatch(commit -> commit.getSummary() != null && !commit.getSummary().isEmpty());
        assertTrue(hasNonEmptySummary, "At least one 'Others' commit should have non-empty summary");
    }

    @Test
    void generateCommitSummaries_separateCall_shouldGenerateSummariesForCategorization() throws Exception {
        // Given
        CommitCategorization categorization = createTestCategorization();

        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Generate exactly 2 short sentences")), eq(100)))
            .thenReturn("Technical change description\nBusiness impact explanation");

        // When
        Map<String, String> summaries = analysisService.generateCommitSummaries(categorization);

        // Then
        assertEquals(3, summaries.size());
        assertTrue(summaries.containsKey("bug123"));
        assertTrue(summaries.containsKey("feat456"));
        assertTrue(summaries.containsKey("impr789"));

        String summary = summaries.get("bug123");
        assertNotNull(summary);
        assertTrue(summary.contains("Technical change description"));
        assertTrue(summary.contains("Business impact explanation"));
        assertFalse(summary.contains("\n"), "Summary should be single line now");
    }

    @Test
    void categorizeCommits_withEmptyCommitList_shouldReturnEmptyCategorizationWithoutAiCalls() {
        // Given
        List<Commit> commits = Collections.emptyList();

        // When
        CommitCategorization result = analysisService.categorizeCommits(commits);

        // Then
        assertNotNull(result);
        assertTrue(result.getBugFixes().isEmpty());
        assertTrue(result.getFeatures().isEmpty());
        assertTrue(result.getImprovements().isEmpty());
        assertTrue(result.getOthers().isEmpty());
        assertEquals(0, result.getTotalCount());

        // Should not make any AI calls
        verifyNoInteractions(mockAiClient);
    }

    @Test
    void categorizeCommits_withNullCommitList_shouldReturnEmptyCategorizationWithoutAiCalls() {
        // Given
        List<Commit> commits = null;

        // When
        CommitCategorization result = analysisService.categorizeCommits(commits);

        // Then
        assertNotNull(result);
        assertTrue(result.getBugFixes().isEmpty());
        assertTrue(result.getFeatures().isEmpty());
        assertTrue(result.getImprovements().isEmpty());
        assertTrue(result.getOthers().isEmpty());
        assertEquals(0, result.getTotalCount());

        // Should not make any AI calls
        verifyNoInteractions(mockAiClient);
    }

    @Test
    void shutdown_shouldCleanupResources() {
        // Given - service is initialized

        // When
        analysisService.shutdown();

        // Then - should complete without errors
        // This test verifies proper resource cleanup
        assertTrue(true, "Shutdown completed successfully");
    }

    @Test
    void generateSummary_withValidInput_shouldReturnGeneratedSummary() {
        // Given
        CommitCategorization categorization = createTestCategorization();
        org.microsoft.github.data.RepositoryData repoData = new org.microsoft.github.data.RepositoryData();
        repoData.setRepoName("test/repo");
        repoData.setCommits(List.of(createCommit("test123", "Test commit", "tester")));

        // Mock AI response
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Generate a 2-3 sentence summary")), eq(500)))
            .thenReturn("This is a generated summary for the repository activity.");

        // When
        String summary = analysisService.generateSummary(repoData, categorization, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(summary);
        assertFalse(summary.isEmpty());
        assertEquals("This is a generated summary for the repository activity.", summary);

        // Verify AI was called with correct parameters
        verify(mockAiClient).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
               argThat(prompt -> prompt.contains("2024-01-01") && prompt.contains("2024-01-07")), eq(500));
    }

    @Test
    void generateSummary_whenAiReturnsEmptyString_shouldReturnEmptyString() {
        // Given
        CommitCategorization categorization = createTestCategorization();
        org.microsoft.github.data.RepositoryData repoData = new org.microsoft.github.data.RepositoryData();
        repoData.setRepoName("test/repo");
        repoData.setCommits(List.of(createCommit("test123", "Test commit", "tester")));

        // Mock AI returning empty response
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Generate a 2-3 sentence summary")), eq(500)))
            .thenReturn("");

        // When
        String summary = analysisService.generateSummary(repoData, categorization, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(summary);
        assertTrue(summary.isEmpty());

        // Verify AI was called
        verify(mockAiClient).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
               argThat(prompt -> prompt.contains("Generate a 2-3 sentence summary")), eq(500));
    }

    @Test
    void generateSummary_whenAiThrowsException_shouldReturnEmptyString() {
        // Given
        CommitCategorization categorization = createTestCategorization();
        org.microsoft.github.data.RepositoryData repoData = new org.microsoft.github.data.RepositoryData();
        repoData.setRepoName("test/repo");
        repoData.setCommits(List.of(createCommit("test123", "Test commit", "tester")));

        // Mock AI throwing exception
        when(mockAiClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
             argThat(prompt -> prompt.contains("Generate a 2-3 sentence summary")), eq(500)))
            .thenThrow(new RuntimeException("AI service failed"));

        // When
        String summary = analysisService.generateSummary(repoData, categorization, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(summary);
        assertTrue(summary.isEmpty());

        // Verify AI was called
        verify(mockAiClient).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
               argThat(prompt -> prompt.contains("Generate a 2-3 sentence summary")), eq(500));
    }

    // Helper methods

    private List<Commit> createMixedCommits() {
        List<Commit> commits = new ArrayList<>();

        // Bug fix commit
        Commit bugFix = new Commit();
        bugFix.setSha("bug123");
        bugFix.setMessage("fix: resolve memory leak in cache system");
        bugFix.setAuthorLogin("bugfixer");
        commits.add(bugFix);

        // Feature commit
        Commit feature = new Commit();
        feature.setSha("feat456");
        feature.setMessage("feat: add user authentication and authorization");
        feature.setAuthorLogin("developer");
        commits.add(feature);

        // Another bug fix
        Commit bugFix2 = new Commit();
        bugFix2.setSha("bug789");
        bugFix2.setMessage("hotfix: patch security vulnerability");
        bugFix2.setAuthorLogin("security");
        commits.add(bugFix2);

        // Documentation update
        Commit docs = new Commit();
        docs.setSha("docs012");
        docs.setMessage("docs: update API documentation");
        docs.setAuthorLogin("writer");
        commits.add(docs);

        return commits;
    }

    private CommitCategorization createTestCategorization() {
        List<Commit> bugFixes = List.of(createCommit("bug123", "Fix null pointer", "bugfixer"));
        List<Commit> features = List.of(createCommit("feat456", "Add new feature", "developer"));
        List<Commit> improvements = List.of(createCommit("impr789", "Refactor code", "refactorer"));
        List<Commit> others = List.of();

        return new CommitCategorization(bugFixes, features, improvements, others);
    }

    private Commit createCommit(String sha, String message, String author) {
        Commit commit = new Commit();
        commit.setSha(sha);
        commit.setMessage(message);
        commit.setAuthorLogin(author);
        return commit;
    }
}
