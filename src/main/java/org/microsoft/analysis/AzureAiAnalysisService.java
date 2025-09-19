package org.microsoft.analysis;

import org.microsoft.AzureFoundryAgentClient;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.RepositoryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Azure AI-powered implementation of analysis service with parallel commit summarization.
 *
 * This service provides both commit categorization and individual commit summarization
 * capabilities using Azure AI. Commit summarization is performed in parallel for
 * improved performance when processing large numbers of commits.
 */
public class AzureAiAnalysisService implements AiAnalysisService {
    private static final Logger logger = LoggerFactory.getLogger(AzureAiAnalysisService.class);

    private final AzureFoundryAgentClient aiClient;
    private ParallelCommitSummaryService summaryService;
    private final boolean enableCommitSummaries;

    public AzureAiAnalysisService(AzureFoundryAgentClient aiClient) {
        this.aiClient = aiClient;
        this.enableCommitSummaries = true;
        // Initialize summary service with default configuration
        initializeSummaryService(4, 30, 500);
    }

    /**
     * Constructor with configurable summary service parameters.
     *
     * @param aiClient The Azure AI client
     * @param summaryThreads Number of threads for parallel summary generation
     * @param summaryTimeoutSeconds Timeout for summary requests
     * @param summaryMaxTokens Maximum tokens per summary
     */
    public AzureAiAnalysisService(AzureFoundryAgentClient aiClient, int summaryThreads,
                                 int summaryTimeoutSeconds, int summaryMaxTokens) {
        this.aiClient = aiClient;
        this.enableCommitSummaries = true;
        initializeSummaryService(summaryThreads, summaryTimeoutSeconds, summaryMaxTokens);
    }

    private void initializeSummaryService(int threads, int timeout, int maxTokens) {
        this.summaryService = new ParallelCommitSummaryService(aiClient, threads, timeout, maxTokens, true);
        logger.info("Initialized commit summary service with {} threads (preserveOrder=true)", threads);
    }

    @Override
    public CommitCategorization categorizeCommits(List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            return new CommitCategorization(List.of(), List.of(), List.of(), List.of());
        }

        String prompt = buildCategorizationPrompt(commits);
        String response = sendToAgent(prompt, 2000);
        CommitCategorization categorization = parseCategorizationResponse(response, commits);

        if (enableCommitSummaries) {
            // Generate AI summaries for ALL commits instead of just prioritized ones
            logger.info("Generating AI summaries for all commits in categorization");
            logger.info("Total commits to process: {}", categorization.getTotalCount());

            // Log breakdown by category
            logger.info("Bug Fixes: {} commits", categorization.getBugFixes().size());
            logger.info("Features: {} commits", categorization.getFeatures().size());
            logger.info("Improvements: {} commits", categorization.getImprovements().size());
            logger.info("Others: {} commits", categorization.getOthers().size());

            // Generate AI summaries for ALL commits using the existing method
            Map<String, String> aiSummaries = summaryService.generateCommitSummaries(categorization);

            // Attach AI summaries or fallback for every commit
            attachSummariesWithFallback(categorization, aiSummaries);
            logger.info("Completed commit categorization and full summarization (AI calls={})", aiSummaries.size());
        } else {
            logger.info("Commit summaries disabled; skipping per-commit summary generation");
        }
        return categorization;
    }

    private void attachSummariesWithFallback(CommitCategorization categorization, Map<String, String> aiSummaries) {
        List<List<Commit>> allCategories = Arrays.asList(
            categorization.getBugFixes(),
            categorization.getFeatures(),
            categorization.getImprovements(),
            categorization.getOthers()
        );
        for (List<Commit> category : allCategories) {
            for (int i = 0; i < category.size(); i++) {
                Commit commit = category.get(i);
                String summary = aiSummaries.get(commit.getSha());
                if (summary != null) {
                    commit.setSummary(summary);
                    logger.debug("AI summary attached for commit {}: {}", commit.getSha(), summary);
                } else {
                    // Leave the summary blank if no AI summary is available
                    commit.setSummary("");
                    logger.debug("No AI summary available for commit {}, leaving blank", commit.getSha());
                }
            }
        }
    }

    @Override
    public String generateSummary(RepositoryData data, CommitCategorization categorization,
                                 String startDate, String endDate) {
        if (data == null || categorization == null) {
            return "No repository activity found for the specified period.";
        }

        // Build a comprehensive prompt for repository summary
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a 2-3 sentence summary of this repository's activity.\n\n");
        prompt.append("Repository: ").append(data.getRepoName()).append("\n");
        prompt.append("Period: ").append(startDate).append(" to ").append(endDate).append("\n\n");

        // Add statistics
        prompt.append("Statistics:\n");
        prompt.append("- Total commits: ").append(categorization.getTotalCount()).append("\n");
        prompt.append("- Bug fixes: ").append(categorization.getBugFixes().size()).append("\n");
        prompt.append("- New features: ").append(categorization.getFeatures().size()).append("\n");
        prompt.append("- Improvements: ").append(categorization.getImprovements().size()).append("\n");
        prompt.append("- Other changes: ").append(categorization.getOthers().size()).append("\n");

        if (data.getPullRequests() != null) {
            prompt.append("- Pull requests: ").append(data.getPullRequests().size()).append("\n");
        }

        if (data.getIssues() != null) {
            prompt.append("- New issues: ").append(data.getIssues().size()).append("\n");
        }

        prompt.append("\nFocus on the most significant activity and trends. Be concise and informative.");

        try {
            String summary = sendToAgent(prompt.toString(), 500);
            return summary != null && !summary.trim().isEmpty() ? summary.trim() : "";
        } catch (Exception e) {
            logger.warn("Failed to generate AI summary: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Generates individual commit summaries for already categorized commits.
     * This method can be called separately if categorization was done earlier.
     *
     * @param categorization The categorized commits
     * @return Map of commit SHA to summary text
     */
    public Map<String, String> generateCommitSummaries(CommitCategorization categorization) {
        return summaryService.generateCommitSummaries(categorization);
    }

    private String buildCategorizationPrompt(List<Commit> commits) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Categorize each commit by number into: Bug Fixes, Features, Improvements, Others.\n");
        prompt.append("Use keywords: Bug Fixes (fix,bug,hotfix), Features (feat,feature,add,optimize), ");
        prompt.append("Improvements (refactor,perf), Others (build,ci,chore,test,docs).\n");
        prompt.append("Priority order: Bug Fixes > Features > Improvements > Others\n\n");

        for (int i = 0; i < commits.size() && i < 50; i++) { // Limit to 50 commits
            Commit commit = commits.get(i);
            String message = commit.getMessage();
            if (message != null) {
                String truncatedMessage = message.length() > 80 ?
                    message.substring(0, 80) + "..." : message;
                prompt.append(String.format("%d. %s\n", i + 1, truncatedMessage));
            }
        }

        prompt.append("\nRespond with ONLY this format: ");
        prompt.append("Bug Fixes: [1,3,5], Features: [2,4], Improvements: [6], Others: [7,8]");
        return prompt.toString();
    }

    private CommitCategorization parseCategorizationResponse(String response, List<Commit> commits) {
        List<Commit> bugFixes;
        List<Commit> features;
        List<Commit> improvements;
        List<Commit> others;

        try {
            // Parse the response format: "Bug Fixes: [1,3,5], Features: [2,4], ..."
            bugFixes = extractCommitsByCategory(response, "Bug Fixes", commits);
            features = extractCommitsByCategory(response, "Features", commits);
            improvements = extractCommitsByCategory(response, "Improvements", commits);
            others = extractCommitsByCategory(response, "Others", commits);

            // If no commits were categorized, use fallback
            if (bugFixes.isEmpty() && features.isEmpty() && improvements.isEmpty() && others.isEmpty()) {
                logger.warn("AI response did not categorize any commits, using fallback");
                return fallbackCategorization(commits);
            }

        } catch (Exception e) {
            logger.warn("Failed to parse AI categorization response: {}", e.getMessage());
            // Fallback: categorize using simple keyword matching
            return fallbackCategorization(commits);
        }

        return new CommitCategorization(bugFixes, features, improvements, others);
    }

    private List<Commit> extractCommitsByCategory(String response, String category, List<Commit> commits) {
        Pattern pattern = Pattern.compile(category + ":\\s*\\[([^\\]]*)\\]");
        Matcher matcher = pattern.matcher(response);

        if (matcher.find()) {
            String indices = matcher.group(1).trim();
            if (!indices.isEmpty()) {
                return Arrays.stream(indices.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .mapToInt(Integer::parseInt)
                    .filter(i -> i > 0 && i <= commits.size())
                    .mapToObj(i -> commits.get(i - 1)) // Convert to 0-based index
                    .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }

    private CommitCategorization fallbackCategorization(List<Commit> commits) {
        List<Commit> bugFixes = new ArrayList<>();
        List<Commit> features = new ArrayList<>();
        List<Commit> improvements = new ArrayList<>();
        List<Commit> others = new ArrayList<>();

        for (Commit commit : commits) {
            String message = commit.getMessage();
            if (message == null) {
                others.add(commit);
                continue;
            }

            String lowerMessage = message.toLowerCase();
            if (lowerMessage.contains("fix") || lowerMessage.contains("bug") || lowerMessage.contains("hotfix")) {
                bugFixes.add(commit);
            } else if (lowerMessage.contains("feat") || lowerMessage.contains("feature") ||
                      lowerMessage.contains("add") || lowerMessage.contains("optimize")) {
                features.add(commit);
            } else if (lowerMessage.contains("refactor") || lowerMessage.contains("perf")) {
                improvements.add(commit);
            } else {
                others.add(commit);
            }
        }

        return new CommitCategorization(bugFixes, features, improvements, others);
    }

    private String sendToAgent(String prompt, int maxTokens) {
        try {
            // Use the new overloaded method with custom prompt
            return aiClient.getSummaryFromAgent(null, "", "", 0, "", prompt, maxTokens);
        } catch (Exception e) {
            logger.error("Failed to get response from AI agent: {}", e.getMessage());
            return "";
        }
    }

    /**
     * Shuts down the parallel summary service to free resources.
     * Call this when the analysis service is no longer needed.
     */
    public void shutdown() {
        if (summaryService != null) {
            summaryService.shutdown();
            logger.info("Shut down commit summary service");
        }
    }
}
