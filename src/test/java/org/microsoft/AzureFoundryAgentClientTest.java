package org.microsoft;

import com.azure.ai.openai.OpenAIClient;
import com.azure.ai.openai.models.ChatChoice;
import com.azure.ai.openai.models.ChatCompletions;
import com.azure.ai.openai.models.ChatCompletionsOptions;
import com.azure.ai.openai.models.ChatResponseMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.microsoft.github.data.RepositoryData;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AzureFoundryAgentClientTest {
    @Mock
    private OpenAIClient mockOpenAIClient;
    @Mock
    private ChatCompletions mockCompletions;
    @Mock
    private ChatChoice mockChoice;
    @Mock
    private ChatResponseMessage mockChatResponseMessage;

    private AzureFoundryAgentClient agentClient;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Use reflection to inject the mock OpenAIClient into the agentClient
        agentClient = new AzureFoundryAgentClient("https://dummy-endpoint/", "dummy-key", "dummy-deployment");
        try {
            java.lang.reflect.Field clientField = AzureFoundryAgentClient.class.getDeclaredField("client");
            clientField.setAccessible(true);
            clientField.set(agentClient, mockOpenAIClient);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testGetSummaryFromAgent_Success() {
        RepositoryData repoData = new RepositoryData();
        repoData.setRepoName("test-repo");
        String expectedSummary = "This is a test summary from Azure OpenAI.";
        when(mockOpenAIClient.getChatCompletions(any(), any(ChatCompletionsOptions.class))).thenReturn(mockCompletions);
        when(mockCompletions.getChoices()).thenReturn(Arrays.asList(mockChoice));
        when(mockChoice.getMessage()).thenReturn(mockChatResponseMessage);
        when(mockChatResponseMessage.getContent()).thenReturn(expectedSummary);
        String summary = agentClient.getSummaryFromAgent(repoData, "2025-09-01", "2025-09-08", 7, "test1, test2");
        assertEquals(expectedSummary, summary);
    }

    @Test
    void testGetSummaryFromAgent_NoChoices() {
        RepositoryData repoData = new RepositoryData();
        repoData.setRepoName("test-repo");
        when(mockOpenAIClient.getChatCompletions(any(), any(ChatCompletionsOptions.class))).thenReturn(mockCompletions);
        when(mockCompletions.getChoices()).thenReturn(Arrays.asList());
        String summary = agentClient.getSummaryFromAgent(repoData, "2025-09-01", "2025-09-08", 7, "test1, test2");
        assertEquals("", summary);
    }

    @Test
    void testGetSummaryFromAgent_Exception() {
        RepositoryData repoData = new RepositoryData();
        repoData.setRepoName("test-repo");
        when(mockOpenAIClient.getChatCompletions(any(), any(ChatCompletionsOptions.class))).thenThrow(new RuntimeException("OpenAI error"));
        String summary = agentClient.getSummaryFromAgent(repoData, "2025-09-01", "2025-09-08", 7, "test1, test2");
        assertTrue(summary.contains("[ERROR] Exception: OpenAI error"));
    }

    @Test
    void testBuildSummaryPrompt() {
        RepositoryData repoData = new RepositoryData();
        repoData.setRepoName("test-repo");
        String prompt = agentClient.buildSummaryPrompt(repoData, "2025-09-01", "2025-09-08", 7, "test1, test2");
        assertTrue(prompt.contains("Summarize the following repository data for the period 2025-09-01 to 2025-09-08 (7 days)"));
        assertTrue(prompt.contains("Microsoft team members for reference: test1, test2"));
    }

    @Test
    void testMicrosoftTeamActivityPromptAndReply() {
        // Prepare RepositoryData with both team and non-team commits
        RepositoryData repoData = new RepositoryData();
        repoData.setRepoName("test-repo");
        org.microsoft.github.data.Commit teamCommit = new org.microsoft.github.data.Commit();
        teamCommit.setSha("abc123");
        teamCommit.setMessage("Team commit");
        teamCommit.setAuthorLogin("mskapilks");
        org.microsoft.github.data.Commit nonTeamCommit = new org.microsoft.github.data.Commit();
        nonTeamCommit.setSha("def456");
        nonTeamCommit.setMessage("Non-team commit");
        nonTeamCommit.setAuthorLogin("xiaoxmeng");
        repoData.setTeamCommits(Arrays.asList(teamCommit));
        repoData.setCommits(Arrays.asList(teamCommit, nonTeamCommit));

        // Build the prompt and check team member list is present
        String prompt = agentClient.buildSummaryPrompt(repoData, "2025-09-01", "2025-09-08", 7, "mskapilks");
        assertTrue(prompt.contains("mskapilks"), "Team member list should be present in Microsoft Team Activity section of the prompt");
    }
}
