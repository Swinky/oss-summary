package org.microsoft;

import org.microsoft.github.data.RepositoryData;
import org.microsoft.github.service.GitHubService;
import org.microsoft.report.ReportService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static final Logger logger = LoggerFactory.getLogger(SummaryGeneratorOrchestrator.class);

    private final ConfigLoader configLoader;
    private final GitHubService githubService;
    private final ReportService reportService;

    public SummaryGeneratorOrchestrator(ConfigLoader configLoader, GitHubService githubService, AzureFoundryAgentClient agentClient) {
        this.configLoader = configLoader;
        this.githubService = githubService;
        this.reportService = new ReportService(agentClient);
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
        logger.info("Starting summary generation for repositories: {}", repositories);
        logger.info("Date period: {} to {}", startDate, endDate);

        List<RepositoryData> repoDataList = new ArrayList<>();
        long startFetch = System.currentTimeMillis();
        try {
            logger.info("Fetching GitHub data...");
            repoDataList = githubService.fetchData(repositories, startDate, endDate, configLoader.getMsteam());
            long endFetch = System.currentTimeMillis();
            logger.info("GitHub data fetched for {} repositories. Time taken: {} ms", repoDataList.size(), (endFetch - startFetch));
        } catch (Exception e) {
            long endFetch = System.currentTimeMillis();
            logger.error("Error fetching GitHub data: {} (Time taken: {} ms)", e.getMessage(), (endFetch - startFetch), e);
        }

        for (RepositoryData repoData : repoDataList) {
            long startSummary = System.currentTimeMillis();
            logger.info("Generating summary for repo: {}", repoData.getRepoName());

            // Bot filtering now happens at the source during data fetching - no need to filter again here

            // Use the new approach: AI for categorization/summary, local HTML generation
            String summary = reportService.generateReport(repoData, startDate, endDate);

            long endSummary = System.currentTimeMillis();
            logger.info("Summary generated for repo: {}. Time taken: {} ms", repoData.getRepoName(), (endSummary - startSummary));

            String outputDir = configLoader.getOutputDir();
            String repoFileName = repoData.getRepoName().replace('/', '-').replace(' ', '_') + ".html";
            long startWrite = System.currentTimeMillis();
            writeHtmlToFile(outputDir, repoFileName, summary);
            long endWrite = System.currentTimeMillis();
            logger.info("Summary written to: {}{}{}. Time taken: {} ms", outputDir, File.separator, repoFileName, (endWrite - startWrite));
        }
        long endAll = System.currentTimeMillis();
        logger.info("All summaries generated. Total time taken: {} ms", (endAll - startAll));
    }

    private String calculateStartDate(String endDate, int period) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate end = LocalDate.parse(endDate, formatter);
        LocalDate start = end.minusDays(period);
        return start.format(formatter);
    }

    /**
     * Writes the HTML report directly to file (no additional wrapping needed).
     */
    private void writeHtmlToFile(String outputDir, String fileName, String htmlContent) {
        try {
            File dir = new File(outputDir);
            if (!dir.exists()) {
                boolean created = dir.mkdirs();
                if (!created) {
                    logger.warn("Failed to create output directory: {}", outputDir);
                }
            }

            File file = new File(dir, fileName);
            try (FileWriter writer = new FileWriter(file)) {
                writer.write(htmlContent);
            }
            logger.info("HTML report saved to: {}", file.getAbsolutePath());
        } catch (IOException e) {
            logger.error("Error writing HTML file: {}", e.getMessage(), e);
        }
    }
}
