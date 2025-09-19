package org.microsoft.analysis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.microsoft.AzureFoundryAgentClient;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.RepositoryData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AzureAiAnalysisServiceTest {

    @Mock
    private AzureFoundryAgentClient mockClient;

    private AzureAiAnalysisService service;

    @BeforeEach
    void setUp() {
        service = new AzureAiAnalysisService(mockClient);
    }

    @Test
    void shouldCategorizeCommitsCorrectly() {
        // Given
        List<Commit> commits = Arrays.asList(
            createCommit("fix: resolve null pointer exception"),
            createCommit("feat: add new dashboard feature"),
            createCommit("refactor: improve performance"),
            createCommit("build: update dependencies")
        );

        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [1], Features: [2], Improvements: [3], Others: [4]");

        // When
        CommitCategorization result = service.categorizeCommits(commits);

        // Then
        assertEquals(1, result.getBugFixes().size());
        assertEquals(1, result.getFeatures().size());
        assertEquals(1, result.getImprovements().size());
        assertEquals(1, result.getOthers().size());
        assertEquals(4, result.getTotalCount());

        // Verify correct commits are in correct categories
        assertEquals("fix: resolve null pointer exception", result.getBugFixes().get(0).getMessage());
        assertEquals("feat: add new dashboard feature", result.getFeatures().get(0).getMessage());
        assertEquals("refactor: improve performance", result.getImprovements().get(0).getMessage());
        assertEquals("build: update dependencies", result.getOthers().get(0).getMessage());
    }

    @Test
    void shouldHandleEmptyCommitList() {
        // When
        CommitCategorization result = service.categorizeCommits(List.of());

        // Then
        assertTrue(result.getBugFixes().isEmpty());
        assertTrue(result.getFeatures().isEmpty());
        assertTrue(result.getImprovements().isEmpty());
        assertTrue(result.getOthers().isEmpty());
        assertEquals(0, result.getTotalCount());

        // Verify no AI call was made
        verifyNoInteractions(mockClient);
    }

    @Test
    void shouldUseFallbackCategorizationOnAiFailure() {
        // Given
        List<Commit> commits = Arrays.asList(
            createCommit("fix: critical bug"),
            createCommit("feat: new feature"),
            createCommit("refactor: code cleanup")
        );

        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Invalid response format");

        // When
        CommitCategorization result = service.categorizeCommits(commits);

        // Then
        assertEquals(1, result.getBugFixes().size());
        assertEquals(1, result.getFeatures().size());
        assertEquals(1, result.getImprovements().size());
        assertEquals(0, result.getOthers().size());
    }

    @Test
    void shouldGenerateAppropriatePromptLength() {
        // Given - many commits to test prompt length
        List<Commit> manyCommits = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            manyCommits.add(createCommit("commit message " + i));
        }

        // Mock AI response
        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []");

        // When
        service.categorizeCommits(manyCommits);

        // Then - verify the prompt sent to AI is reasonable length
        verify(mockClient).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), argThat(prompt -> {
            int estimatedTokens = prompt.length() / 4;
            return estimatedTokens < 20000; // Should be less than 20k tokens
        }), eq(2000));
    }

    @Test
    void shouldGenerateReasonableSummary() {
        // Given
        RepositoryData data = createTestRepositoryData();
        CommitCategorization categorization = new CommitCategorization(
            List.of(createCommit("fix: bug")),
            List.of(createCommit("feat: feature")),
            List.of(createCommit("refactor: improve")),
            List.of(createCommit("build: deps"))
        );

        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("This week saw balanced development with bug fixes and new features.");

        // When
        String summary = service.generateSummary(data, categorization, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(summary);
        assertFalse(summary.isEmpty());
        verify(mockClient).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""),
            argThat(prompt -> prompt.contains("2024-01-01") && prompt.contains("2024-01-07")), eq(500));
    }

    @Test
    void shouldReturnEmptyStringWhenAiDoesNotGenerateSummary() {
        // Given
        RepositoryData data = createTestRepositoryData();
        CommitCategorization categorization = new CommitCategorization(
            List.of(createCommit("fix: bug")),
            List.of(createCommit("feat: feature")),
            List.of(),
            List.of()
        );

        // Mock AI returning null or empty response
        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("");

        // When
        String summary = service.generateSummary(data, categorization, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(summary);
        assertTrue(summary.isEmpty());
        verify(mockClient).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500));
    }

    @Test
    void shouldReturnEmptyStringWhenAiGeneratesSummaryFails() {
        // Given
        RepositoryData data = createTestRepositoryData();
        CommitCategorization categorization = new CommitCategorization(
            List.of(createCommit("fix: bug")),
            List.of(createCommit("feat: feature")),
            List.of(),
            List.of()
        );

        // Mock AI throwing exception
        when(mockClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenThrow(new RuntimeException("AI service unavailable"));

        // When
        String summary = service.generateSummary(data, categorization, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(summary);
        assertTrue(summary.isEmpty());
        verify(mockClient).getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500));
    }

    private Commit createCommit(String message) {
        Commit commit = new Commit();
        commit.setMessage(message);
        commit.setAuthorLogin("testuser");
        commit.setSha("abc123");
        return commit;
    }

    private RepositoryData createTestRepositoryData() {
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");
        data.setCommits(List.of(createCommit("test commit")));
        data.setPullRequests(List.of());
        data.setIssues(List.of());
        return data;
    }
}
