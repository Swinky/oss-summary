package org.microsoft.report;

import org.microsoft.analysis.AiAnalysisService;
import org.microsoft.analysis.AzureAiAnalysisService;
import org.microsoft.analysis.CommitCategorization;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.RepositoryData;
import org.microsoft.AzureFoundryAgentClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Main service for generating reports for any date range using AI analysis.
 * This separates AI analysis from HTML generation for better consistency and token efficiency.
 */
public class ReportService {
    private static final Logger logger = LoggerFactory.getLogger(ReportService.class);

    private final AiAnalysisService aiService;
    private final HtmlReportGenerator htmlGenerator;

    public ReportService(AzureFoundryAgentClient aiClient) {
        this.aiService = new AzureAiAnalysisService(aiClient); // enable per-commit AI summaries
        this.htmlGenerator = new HtmlReportGenerator();
    }

    /**
     * Generates a complete report for the specified date range using the new approach:
     * 1. Filter out bot activity
     * 2. Use AI for categorization and summary
     * 3. Generate HTML locally
     */
    public String generateReport(RepositoryData data, String startDate, String endDate) {
        logger.info("Starting report generation for date range: {} to {}", startDate, endDate);

        // Filter out bot activity from the commits that will be analyzed
        List<Commit> nonBotCommits = filterBotCommits(data.getCommits());
        logger.info("Filtered commits: {} non-bot commits from {} total",
                   nonBotCommits.size(), (data.getCommits() != null ? data.getCommits().size() : 0));

        // Get AI analysis - this uses much smaller prompts and responses
        logger.info("Getting AI categorization for commits...");
        CommitCategorization categorization = aiService.categorizeCommits(nonBotCommits);
        logger.info("Categorization complete: {}", categorization.toString());

        logger.info("Getting AI summary...");
        String summary = aiService.generateSummary(data, categorization, startDate, endDate);

        // Handle null summary from AI service
        if (summary == null) {
            summary = "No significant activity recorded for this period.";
        }

        logger.info("Summary generated: {} characters", summary.length());

        // Generate HTML locally - this ensures consistent structure
        logger.info("Generating HTML report locally...");
        String htmlReport = htmlGenerator.generateReport(data, categorization, summary, startDate, endDate);
        logger.info("HTML report generated: {} characters", htmlReport.length());

        return htmlReport;
    }

    /**
     * Filters out commits from bots (e.g., GlutenPerfBot).
     */
    private List<Commit> filterBotCommits(List<Commit> commits) {
        if (commits == null) return List.of();

        return commits.stream()
            .filter(commit -> {
                String authorLogin = commit.getAuthorLogin();
                return authorLogin == null || !authorLogin.toLowerCase().contains("bot");
            })
            .collect(Collectors.toList());
    }
}
