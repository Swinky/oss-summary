package org.microsoft;

import org.microsoft.github.data.RepositoryData;

import java.util.ArrayList;
import java.util.List;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Orchestrates the main application logic for fetching and summarizing OSS data.
 */
public class SummaryGeneratorOrchestrator {

    private final ConfigLoader configLoader;
    private final GitHubDataFetcher fetcher;
    private final AzureFoundryAgentClient agentClient;

    public SummaryGeneratorOrchestrator(ConfigLoader configLoader, GitHubDataFetcher fetcher, AzureFoundryAgentClient agentClient) {
        this.configLoader = configLoader;
        this.fetcher = fetcher;
        this.agentClient = agentClient;
    }

    /**
     * Runs the application logic.
     * @param args Command-line arguments.
     */
    public void run(String[] args) {
        List<String> repositories;
        String endDate;
        int period;

        if (args.length == 0) {
            repositories = configLoader.getRepositories();
            endDate = configLoader.getEndDate();
            period = configLoader.getSummaryPeriod();
        } else {
            UserInput userInput = new UserInput(args);
            repositories = userInput.getRepositories() != null && !userInput.getRepositories().isEmpty()
                    ? userInput.getRepositories() : configLoader.getRepositories();
            endDate = userInput.getEndDate() != null ? userInput.getEndDate() : configLoader.getEndDate();
            period = userInput.getSummaryPeriod() > 0 ? userInput.getSummaryPeriod() : configLoader.getSummaryPeriod();
        }
        String startDate = calculateStartDate(endDate, period);

        long startAll = System.currentTimeMillis();
        System.out.println("[INFO] Starting summary generation for repositories: " + repositories);
        System.out.println("[INFO] Date period: " + startDate + " to " + endDate);
        List<RepositoryData> repoDataList = new ArrayList<>();
        long startFetch = System.currentTimeMillis();
        try {
            System.out.println("[INFO] Fetching GitHub data...");
            repoDataList = fetcher.fetchData(repositories, startDate, endDate);
            long endFetch = System.currentTimeMillis();
            System.out.println("[INFO] GitHub data fetched for " + repoDataList.size() + " repositories. Time taken: " + (endFetch - startFetch) + " ms");
        } catch (Exception e) {
            long endFetch = System.currentTimeMillis();
            System.err.println("Error fetching GitHub data: " + e.getMessage() + " (Time taken: " + (endFetch - startFetch) + " ms)");
            e.printStackTrace();
        }

        for (RepositoryData repoData : repoDataList) {
            long startSummary = System.currentTimeMillis();
            System.out.println("[INFO] Generating summary for repo: " + repoData.getRepoName());
            String summary = agentClient.getSummaryFromAgent(repoData, startDate, endDate, period);
            long endSummary = System.currentTimeMillis();
            System.out.println("[INFO] Summary generated for repo: " + repoData.getRepoName() + ". Time taken: " + (endSummary - startSummary) + " ms");
            String outputDir = configLoader.getOutputDir();
            String repoFileName = repoData.getRepoName().replace('/', '-').replace(' ', '_') + ".html";
            long startWrite = System.currentTimeMillis();
            writeSummaryToHtml(outputDir, repoFileName, summary, repoData.getRepoName(), startDate, endDate);
            long endWrite = System.currentTimeMillis();
            System.out.println("[INFO] Summary written to: " + outputDir + File.separator + repoFileName + ". Time taken: " + (endWrite - startWrite) + " ms");
        }
        long endAll = System.currentTimeMillis();
        System.out.println("[INFO] All summaries generated. Total time taken: " + (endAll - startAll) + " ms");
    }

    private String calculateStartDate(String endDate, int period) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate end = LocalDate.parse(endDate, formatter);
        LocalDate start = end.minusDays(period);
        return start.format(formatter);
    }

    /**
     * Writes the summary to an HTML file in the specified output directory.
     */
    private void writeSummaryToHtml(String outputDir, String fileName, String summary, String repoName, String startDate, String endDate) {
        File dir = new File(outputDir);
        if (!dir.exists()) dir.mkdirs();
        File file = new File(dir, fileName);
        try (FileWriter writer = new FileWriter(file)) {
            writer.write("<html><head><title>");
            writer.write(repoName + " Summary (" + startDate + " to " + endDate + ")");
            writer.write("</title></head><body>");
            writer.write(summary); // Agent output should be full HTML body
            writer.write("</body></html>");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to write summary to HTML file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
    }
}
