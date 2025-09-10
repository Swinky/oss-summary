package org.microsoft;

import org.junit.jupiter.api.*;
import org.microsoft.github.data.RepositoryData;
import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SummaryGeneratorOrchestratorTest {
    private static final String TEST_OUTPUT_DIR = "test-output";
    private ConfigLoader mockConfigLoader;
    private GitHubDataFetcher mockFetcher;
    private AzureFoundryAgentClient mockAgentClient;
    private SummaryGeneratorOrchestrator orchestrator;

    @BeforeEach
    void setup() {
        mockConfigLoader = mock(ConfigLoader.class);
        mockFetcher = mock(GitHubDataFetcher.class);
        mockAgentClient = mock(AzureFoundryAgentClient.class);
        orchestrator = new SummaryGeneratorOrchestrator(mockConfigLoader, mockFetcher, mockAgentClient);
        File dir = new File(TEST_OUTPUT_DIR);
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) file.delete();
            }
            if (!dir.delete()) {
                System.err.println("[WARN] Could not delete test output directory during setup.");
            }
        }
        if (!dir.mkdirs()) {
            System.err.println("[WARN] Could not create test output directory during setup.");
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
            if (!dir.delete()) {
                System.err.println("[WARN] Could not delete test output directory during cleanup.");
            }
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
        when(mockFetcher.fetchData(anyList(), anyString(), anyString())).thenReturn(Arrays.asList(repo1, repo2));
        when(mockAgentClient.getSummaryFromAgent(eq(repo1), anyString(), anyString(), anyInt())).thenReturn("Gluten summary");
        when(mockAgentClient.getSummaryFromAgent(eq(repo2), anyString(), anyString(), anyInt())).thenReturn("Velox summary");
        orchestrator.run(new String[0]);
        File glutenFile = new File(TEST_OUTPUT_DIR, "apache-incubator-gluten.html");
        File veloxFile = new File(TEST_OUTPUT_DIR, "facebook-velox.html");
        assertTrue(glutenFile.exists(), "Gluten HTML file should exist");
        assertTrue(veloxFile.exists(), "Velox HTML file should exist");
        String glutenContent = Files.readString(glutenFile.toPath());
        String veloxContent = Files.readString(veloxFile.toPath());
        assertTrue(glutenContent.contains("Gluten summary"), "Gluten summary should be in HTML");
        assertTrue(veloxContent.contains("Velox summary"), "Velox summary should be in HTML");
    }

    @Test
    void testRunWithNoArgs_UsesConfigValues() throws Exception {
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(java.util.Collections.singletonList("test/repo"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        RepositoryData mockRepoData = new RepositoryData();
        mockRepoData.setRepoName("test/repo");
        when(mockFetcher.fetchData(any(), anyString(), anyString())).thenReturn(java.util.Collections.singletonList(mockRepoData));
        when(mockAgentClient.getSummaryFromAgent(any(), anyString(), anyString(), anyInt())).thenReturn("Test Summary for test/repo");
        orchestrator.run(new String[]{});
        File outputFile = new File(TEST_OUTPUT_DIR, "test-repo.html");
        assertTrue(outputFile.exists(), "Output HTML file should exist");
        String outputContent = Files.readString(outputFile.toPath());
        assertTrue(outputContent.contains("Test Summary for test/repo"));
    }

    @Test
    void testRunWithArgs_UsesUserInput() throws Exception {
        String[] args = {"--date=2025-09-15", "--period=14", "--repos=user/repo"};
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(java.util.Collections.singletonList("config/repo"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        RepositoryData mockRepoData = new RepositoryData();
        mockRepoData.setRepoName("user/repo");
        when(mockFetcher.fetchData(any(), anyString(), anyString())).thenReturn(java.util.Collections.singletonList(mockRepoData));
        when(mockAgentClient.getSummaryFromAgent(any(), anyString(), anyString(), anyInt())).thenReturn("Test Summary for user/repo");
        orchestrator.run(args);
        File outputFile = new File(TEST_OUTPUT_DIR, "user-repo.html");
        assertTrue(outputFile.exists(), "Output HTML file should exist");
        String outputContent = Files.readString(outputFile.toPath());
        assertTrue(outputContent.contains("Test Summary for user/repo"));
    }

    @Test
    void testRunWithFetchError_HandlesGracefully() throws Exception {
        when(mockConfigLoader.getOutputDir()).thenReturn(TEST_OUTPUT_DIR);
        when(mockConfigLoader.getRepositories()).thenReturn(java.util.Collections.singletonList("test/repo"));
        when(mockConfigLoader.getEndDate()).thenReturn("2025-09-10");
        when(mockConfigLoader.getSummaryPeriod()).thenReturn(7);
        when(mockFetcher.fetchData(any(), anyString(), anyString())).thenThrow(new RuntimeException("GitHub API error"));
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
        RepositoryData repo1Data = new RepositoryData();
        repo1Data.setRepoName("repo1/test");
        RepositoryData repo2Data = new RepositoryData();
        repo2Data.setRepoName("repo2/test");
        when(mockFetcher.fetchData(any(), anyString(), anyString())).thenReturn(Arrays.asList(repo1Data, repo2Data));
        when(mockAgentClient.getSummaryFromAgent(eq(repo1Data), anyString(), anyString(), anyInt())).thenReturn("Summary for repo1/test");
        when(mockAgentClient.getSummaryFromAgent(eq(repo2Data), anyString(), anyString(), anyInt())).thenReturn("Summary for repo2/test");
        orchestrator.run(new String[]{});
        File file1 = new File(TEST_OUTPUT_DIR, "repo1-test.html");
        File file2 = new File(TEST_OUTPUT_DIR, "repo2-test.html");
        assertTrue(file1.exists(), "repo1 HTML file should exist");
        assertTrue(file2.exists(), "repo2 HTML file should exist");
        String content1 = Files.readString(file1.toPath());
        String content2 = Files.readString(file2.toPath());
        assertTrue(content1.contains("Summary for repo1/test"));
        assertTrue(content2.contains("Summary for repo2/test"));
    }
}
