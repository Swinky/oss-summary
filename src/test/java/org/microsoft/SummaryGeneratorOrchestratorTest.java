package org.microsoft;

import org.junit.jupiter.api.*;
import org.microsoft.github.data.RepositoryData;
import org.microsoft.github.service.GitHubService;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SummaryGeneratorOrchestratorTest {
    private static final String TEST_OUTPUT_DIR = "test-output";
    private ConfigLoader mockConfigLoader;
    private GitHubService mockGithubService;
    private AzureFoundryAgentClient mockAgentClient;
    private SummaryGeneratorOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        mockConfigLoader = mock(ConfigLoader.class);
        mockGithubService = mock(GitHubService.class);
        mockAgentClient = mock(AzureFoundryAgentClient.class);
        orchestrator = new SummaryGeneratorOrchestrator(mockConfigLoader, mockGithubService, mockAgentClient);
        File dir = new File(TEST_OUTPUT_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) file.delete();
            }
            if (!dir.delete()) {
                System.err.println("Could not delete test output directory: " + dir.getAbsolutePath());
            }
        }
    }

    @AfterEach
    void cleanup() {
        File dir = new File(TEST_OUTPUT_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) file.delete();
            }
            dir.delete();
        }
    }

    @Test
    void testHtmlOutputPerRepo() throws Exception {
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(Arrays.asList("apache/incubator-gluten", "facebook/velox"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        RepositoryData repo1 = new RepositoryData();
        repo1.setRepoName("apache/incubator-gluten");
        RepositoryData repo2 = new RepositoryData();
        repo2.setRepoName("facebook/velox");
        when(mockGithubService.fetchData(anyList(), anyString(), anyString(), anyList())).thenReturn(Arrays.asList(repo1, repo2));

        // Mock the AI responses for the new WeeklyReportService approach
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []");
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("Repository activity summary for this period.");

        orchestrator.run(new String[0]);

        File glutenFile = new File(TEST_OUTPUT_DIR, "apache-incubator-gluten.html");
        File veloxFile = new File(TEST_OUTPUT_DIR, "facebook-velox.html");

        assertTrue(glutenFile.exists(), "Gluten HTML file should exist");
        assertTrue(veloxFile.exists(), "Velox HTML file should exist");

        String glutenContent = Files.readString(glutenFile.toPath());
        String veloxContent = Files.readString(veloxFile.toPath());

        // Check for the new HTML structure instead of the old summary text
        assertTrue(glutenContent.contains("apache/incubator-gluten OSS Updates"), "Gluten HTML should contain OSS Updates header");
        assertTrue(glutenContent.contains("Overall Summary"), "Gluten HTML should contain Overall Summary section");
        assertTrue(veloxContent.contains("facebook/velox OSS Updates"), "Velox HTML should contain OSS Updates header");
        assertTrue(veloxContent.contains("Overall Summary"), "Velox HTML should contain Overall Summary section");
    }

    @Test
    void testRunWithNoArgs_UsesConfigValues() throws Exception {
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(java.util.Collections.singletonList("test/repo"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        when(mockConfigLoader.getMsteam()).thenReturn(Arrays.asList("testuser"));

        RepositoryData mockRepoData = new RepositoryData();
        mockRepoData.setRepoName("test/repo");

        when(mockGithubService.fetchData(anyList(), anyString(), anyString(), anyList())).thenReturn(java.util.Collections.singletonList(mockRepoData));

        // Mock AI responses for new architecture
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []");
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("No significant activity recorded for this period.");

        orchestrator.run(new String[]{});

        File outputFile = new File(TEST_OUTPUT_DIR, "test-repo.html");
        assertTrue(outputFile.exists(), "Output HTML file should exist");

        String outputContent = Files.readString(outputFile.toPath());
        assertTrue(outputContent.contains("test/repo OSS Updates"), "Should contain repo name in header");
        assertTrue(outputContent.contains("Overall Summary"), "Should contain Overall Summary section");
    }

    @Test
    void testRunWithArgs_UsesUserInput() throws Exception {
        String[] args = {"--date=2025-09-15", "--period=14", "--repos=user/repo"};
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(java.util.Collections.singletonList("config/repo"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        when(mockConfigLoader.getMsteam()).thenReturn(Arrays.asList("testuser"));

        RepositoryData mockRepoData = new RepositoryData();
        mockRepoData.setRepoName("user/repo");

        when(mockGithubService.fetchData(anyList(), anyString(), anyString(), anyList())).thenReturn(java.util.Collections.singletonList(mockRepoData));

        // Mock AI responses for new architecture
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []");
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("No significant activity recorded for this period.");

        orchestrator.run(args);

        File outputFile = new File(TEST_OUTPUT_DIR, "user-repo.html");
        assertTrue(outputFile.exists(), "Output HTML file should exist");

        String outputContent = Files.readString(outputFile.toPath());
        assertTrue(outputContent.contains("user/repo OSS Updates"), "Should contain user repo name in header");
        assertTrue(outputContent.contains("Overall Summary"), "Should contain Overall Summary section");
    }

    @Test
    void testRunWithFetchError_HandlesGracefully() throws Exception {
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(java.util.Collections.singletonList("test/repo"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        when(mockGithubService.fetchData(anyList(), anyString(), anyString(), anyList())).thenThrow(new RuntimeException("GitHub API error"));
        orchestrator.run(new String[]{});
        File dir = new File(TEST_OUTPUT_DIR);
        File[] files = dir.listFiles();
        assertTrue(files == null || files.length == 0, "No output files should be created on fetch error");
    }

    @Test
    void testRunWithMultipleRepositories() throws Exception {
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(Arrays.asList("repo1/test", "repo2/test"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        when(mockConfigLoader.getMsteam()).thenReturn(Arrays.asList("testuser"));

        RepositoryData repo1Data = new RepositoryData();
        repo1Data.setRepoName("repo1/test");
        RepositoryData repo2Data = new RepositoryData();
        repo2Data.setRepoName("repo2/test");

        when(mockGithubService.fetchData(anyList(), anyString(), anyString(), anyList())).thenReturn(Arrays.asList(repo1Data, repo2Data));

        // Mock AI categorization (maxTokens 2000) and summary (maxTokens 500) calls used by WeeklyReportService for both repos
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []");
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("No significant activity recorded for this period.");

        orchestrator.run(new String[]{});

        File file1 = new File(TEST_OUTPUT_DIR, "repo1-test.html");
        File file2 = new File(TEST_OUTPUT_DIR, "repo2-test.html");
        assertTrue(file1.exists(), "repo1 HTML file should exist");
        assertTrue(file2.exists(), "repo2 HTML file should exist");

        String content1 = Files.readString(file1.toPath());
        String content2 = Files.readString(file2.toPath());
        assertTrue(content1.contains("repo1/test OSS Updates"), "Should contain repo1 name in header");
        assertTrue(content1.contains("Overall Summary"), "Should contain Overall Summary section for repo1");
        assertTrue(content2.contains("repo2/test OSS Updates"), "Should contain repo2 name in header");
        assertTrue(content2.contains("Overall Summary"), "Should contain Overall Summary section for repo2");
    }

    @Test
    void testRunWithDefaultConfig() throws Exception {
        when(mockConfigLoader.getRepositories()).thenReturn(Arrays.asList("https://github.com/test/repo"));
        when(mockConfigLoader.getEndDate()).thenReturn("2023-12-31");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        when(mockConfigLoader.getMsteam()).thenReturn(Arrays.asList("testuser"));
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);

        RepositoryData mockRepoData = new RepositoryData();
        mockRepoData.setRepoName("test/repo");
        when(mockGithubService.fetchData(anyList(), anyString(), anyString(), anyList()))
                .thenReturn(Arrays.asList(mockRepoData));

        // Mock the agent client to return a simple HTML string
        when(mockAgentClient.getSummaryFromAgent(any(), any(), any(), anyInt(), any(), any(), anyInt()))
                .thenReturn("Test summary");

        orchestrator.run(new String[]{});

        verify(mockGithubService).fetchData(anyList(), anyString(), anyString(), anyList());
        // Verify output file was created
        File outputFile = new File(TEST_OUTPUT_DIR, "test-repo.html");
        assertTrue(outputFile.exists(), "Output file should be created");
    }

    @Test
    void testRunWithCommandLineArgs() throws Exception {
        // Mock all required configuration values
        when(mockConfigLoader.getMsteam()).thenReturn(Arrays.asList("testuser"));
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(Arrays.asList("default/repo")); // Provide default repos
        when(mockConfigLoader.getEndDate()).thenReturn("2023-12-31"); // Provide default end date
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7); // Provide default period

        RepositoryData mockRepoData = new RepositoryData();
        mockRepoData.setRepoName("custom/repo");
        when(mockGithubService.fetchData(anyList(), anyString(), anyString(), anyList()))
                .thenReturn(Arrays.asList(mockRepoData));

        // Mock AI responses for new WeeklyReportService architecture
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(2000)))
            .thenReturn("Bug Fixes: [], Features: [], Improvements: [], Others: []");
        when(mockAgentClient.getSummaryFromAgent(isNull(), eq(""), eq(""), eq(0), eq(""), anyString(), eq(500)))
            .thenReturn("Test summary");

        String[] args = {"--repos", "https://github.com/custom/repo", "--end-date", "2023-12-25", "--period", "14"};
        orchestrator.run(args);

        verify(mockGithubService).fetchData(eq(Arrays.asList("https://github.com/custom/repo")), anyString(), eq("2023-12-25"), anyList());
    }
}
