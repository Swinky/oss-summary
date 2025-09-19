package org.microsoft.analysis;

import org.microsoft.AzureFoundryAgentClient;
import org.microsoft.github.data.Commit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Multi-threaded service for generating individual commit summaries using Azure AI.
 * This service processes commits in parallel to generate concise 2-line summaries
 * that explain what changed and why it matters. Each commit is processed independently
 * to maximize throughput while maintaining quality.
 * Features:
 * - Parallel processing with configurable thread pool
 * - Timeout protection and error handling
 * - Token-optimized prompts for cost efficiency
 *
 * @author OSS Summary Team
 * @since 1.0
 */
public class ParallelCommitSummaryService {
    private static final Logger logger = LoggerFactory.getLogger(ParallelCommitSummaryService.class);

    private final AzureFoundryAgentClient aiClient;
    private final int threadPoolSize;
    private final int timeoutSeconds;
    private final int maxTokens;
    private final boolean preserveOrder;
    private final ExecutorService executorService;
    private final IssueDescriptionResolver issueResolver; // optional

    /**
     * Optional provider for issue descriptions.
     */
    public interface IssueDescriptionResolver {
        /**
         * @param issueNumber numeric portion of issue reference (e.g. "123")
         * @return description text or null if not found
         */
        String getIssueDescription(String issueNumber);
    }

    /**
     * Creates a new ParallelCommitSummaryService with specified configuration.
     *
     * @param aiClient       The Azure AI client for making summary requests
     * @param threadPoolSize Number of concurrent threads for processing (recommended: 4-8)
     * @param timeoutSeconds Timeout for AI requests in seconds (recommended: 30-60)
     * @param maxTokens      Maximum tokens per summary request (recommended: 120-150)
     */
    public ParallelCommitSummaryService(AzureFoundryAgentClient aiClient, int threadPoolSize,
                                        int timeoutSeconds, int maxTokens) {
        this(aiClient, threadPoolSize, timeoutSeconds, maxTokens, false, null);
    }

    public ParallelCommitSummaryService(AzureFoundryAgentClient aiClient, int threadPoolSize,
                                        int timeoutSeconds, int maxTokens, boolean preserveOrder) {
        this(aiClient, threadPoolSize, timeoutSeconds, maxTokens, preserveOrder, null);
    }

    public ParallelCommitSummaryService(AzureFoundryAgentClient aiClient, int threadPoolSize,
                                        int timeoutSeconds, int maxTokens, boolean preserveOrder,
                                        IssueDescriptionResolver issueResolver) {
        this.aiClient = aiClient;
        this.threadPoolSize = threadPoolSize;
        this.timeoutSeconds = timeoutSeconds;
        this.maxTokens = maxTokens;
        this.preserveOrder = preserveOrder;
        this.issueResolver = issueResolver;
        this.executorService = Executors.newFixedThreadPool(threadPoolSize);
        logger.info("Initialized ParallelCommitSummaryService: {} threads, {}s timeout, {} max tokens, preserveOrder={}",
                threadPoolSize, timeoutSeconds, maxTokens, preserveOrder);
    }

    /**
     * Generates 2-line summaries for all commits in the categorization in parallel.
     * <p>
     * This method extracts all commits from the categorization, processes them
     * concurrently, and returns a map of commit SHA to summary text.
     *
     * @param categorization The commit categorization containing all commits
     * @return Map of commit SHA to 2-line summary text
     */
    public Map<String, String> generateCommitSummaries(CommitCategorization categorization) {
        List<Commit> allCommits = extractAllCommits(categorization);
        return generateCommitSummaries(allCommits);
    }

    /**
     * Generates 2-line summaries for a list of commits in parallel.
     * <p>
     * Each commit is processed independently in its own thread. The method waits
     * for all summaries to complete (up to the configured timeout) and returns
     * a map of results.
     *
     * @param commits List of commits to summarize
     * @return Map of commit SHA to 2-line summary text
     */
    public Map<String, String> generateCommitSummaries(List<Commit> commits) {
        if (commits == null || commits.isEmpty()) {
            return new HashMap<>();
        }

        // Remove smallBatchSequential optimization - always use the configured mode
        boolean runSequential = preserveOrder;

        logger.info("=== EXECUTION ANALYSIS ===");
        logger.info("Total commits to process: {}", commits.size());
        logger.info("ThreadPoolSize: {}, PreserveOrder: {}", threadPoolSize, preserveOrder);
        logger.info("Running in {} mode", runSequential ? "SEQUENTIAL" : "PARALLEL");
        logger.info("========================");

        long startTime = System.currentTimeMillis();
        try {
            if (runSequential) {
                logger.info("Starting SEQUENTIAL processing of {} commits", commits.size());
                Map<String, String> summaryMap = new LinkedHashMap<>();
                int processedCount = 0;
                for (Commit commit : commits) {
                    processedCount++;
                    logger.info("Processing commit {}/{}: {}", processedCount, commits.size(), abbreviateSha(commit.getSha()));
                    CommitSummaryResult result = generateSummary(commit);
                    if (result != null && result.summary != null && !result.summary.trim().isEmpty()) {
                        summaryMap.put(result.commitSha, result.summary);
                        logger.info("Added summary for commit {}: '{}'", abbreviateSha(commit.getSha()), result.summary);
                    } else {
                        logger.warn("No summary generated for commit {}", abbreviateSha(commit.getSha()));
                    }
                }
                long totalTime = System.currentTimeMillis() - startTime;
                logger.info("Completed generating {} summaries sequentially in {}ms (avg: {}ms per commit)",
                        summaryMap.size(), totalTime, totalTime / Math.max(commits.size(), 1));
                return summaryMap;
            }

            List<CompletableFuture<CommitSummaryResult>> futures = commits.stream()
                    .map(commit -> CompletableFuture.supplyAsync(() -> generateSummary(commit), executorService))
                    .toList();

            Map<String, String> summaryMap = new HashMap<>();
            for (CompletableFuture<CommitSummaryResult> future : futures) {
                try {
                    CommitSummaryResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
                    if (result != null && result.summary != null && !result.summary.trim().isEmpty()) {
                        summaryMap.put(result.commitSha, result.summary);
                    }
                } catch (TimeoutException te) {
                    logger.warn("Timeout waiting for a commit summary ({}s)", timeoutSeconds);
                } catch (Exception e) {
                    logger.warn("Error retrieving commit summary: {}", e.getMessage());
                }
            }
            long totalTime = System.currentTimeMillis() - startTime;
            logger.info("Completed generating {} summaries in {}ms (avg: {}ms per commit)",
                    summaryMap.size(), totalTime, totalTime / Math.max(commits.size(), 1));
            return summaryMap;
        } catch (Exception e) {
            logger.error("Unexpected error while generating commit summaries", e);
            return new HashMap<>();
        }
    }

    /**
     * Abbreviates a SHA for logging purposes.
     */
    private String abbreviateSha(String sha) {
        if (sha == null) return "unknown";
        return sha.length() <= 8 ? sha : sha.substring(0, 8);
    }

    /**
     * Generates a 2-sentence summary for a single commit.
     */
    private CommitSummaryResult generateSummary(Commit commit) {
        String threadId = Thread.currentThread().getName();
        try {
            logger.info("[Thread: {}] Processing commit {}", threadId, abbreviateSha(commit.getSha()));
            String prompt = buildSummaryPrompt(commit);
            logger.debug("[Thread: {}] Built prompt for commit {}: '{}'", threadId, abbreviateSha(commit.getSha()), prompt);
            String summary = sendToAgent(prompt);
            logger.debug("[Thread: {}] Raw AI response for commit {}: '{}'", threadId, abbreviateSha(commit.getSha()), summary);

            if (summary != null && !summary.trim().isEmpty()) {
                summary = formatSummaryResponse(summary);
                logger.debug("[Thread: {}] Formatted summary for commit {}: '{}'", threadId, abbreviateSha(commit.getSha()), summary);
            } else {
                logger.warn("[Thread: {}] AI returned null/empty response for commit {}", threadId, abbreviateSha(commit.getSha()));
            }

            logger.info("[Thread: {}] Completed processing commit {} - Final summary: '{}'",
                       threadId, abbreviateSha(commit.getSha()), summary != null ? summary : "null");

            return new CommitSummaryResult(commit.getSha(), summary);
        } catch (Exception e) {
            logger.error("[Thread: {}] Failed to generate summary for commit {}: {}",
                        threadId, abbreviateSha(commit.getSha()), e.getMessage(), e);
            return new CommitSummaryResult(commit.getSha(), null);
        }
    }

    private String buildSummaryPrompt(Commit commit) {
        String message = commit.getMessage();
        String prDescription = commit.getPrDescription();

        // Extract issue references from both commit message AND PR description
        List<String> refs = new ArrayList<>();
        refs.addAll(extractIssueRefs(message));
        if (prDescription != null && !prDescription.trim().isEmpty()) {
            refs.addAll(extractIssueRefs(prDescription));
        }

        // Remove duplicates while preserving order
        refs = refs.stream().distinct().collect(Collectors.toList());

        // Build issues segment with descriptions
        StringBuilder issuesSegment = new StringBuilder();
        if (!refs.isEmpty()) {
            int limit = Math.min(refs.size(), 3); // limit to avoid token bloat
            for (int i = 0; i < limit; i++) {
                String ref = refs.get(i); // e.g. #123
                String num = ref.substring(1); // remove '#'
                String desc = issueResolver != null ? issueResolver.getIssueDescription(num) : null;
                if (desc != null && !desc.isBlank()) {
                    issuesSegment.append("Issue ").append(ref).append(": ")
                            .append(truncate(desc.trim(), 1000))
                            .append("\n");
                }
            }
        }

        // Include PR description in prompt if available
        StringBuilder prSegment = new StringBuilder();
        if (prDescription != null && !prDescription.trim().isEmpty()) {
            prSegment.append("PR Description: ")
                     .append(truncate(prDescription.trim(), 300))
                     .append("\n");
        }

        return String.format(
                "Generate exactly 2 short sentences (each <= 80 chars) summarizing this git commit. " +
                        "Return them in a SINGLE LINE separated by a space (no line breaks). %s%sCommit: %s\nAuthor: %s\nMessage: %s\n\nOutput format (single line): <Sentence 1.> <Sentence 2.>",
                prSegment,
                issuesSegment,
                abbreviateSha(commit.getSha()),
                commit.getAuthorLogin(),
                truncateMessage(message)
        );
    }

    /**
     * Sends a prompt to the Azure AI agent with configured token limit.
     */
    private String sendToAgent(String prompt) {
        try {
            String threadId = Thread.currentThread().getName();
            logger.debug("[Thread: {}] Sending prompt to AI agent: '{}'", threadId, prompt);
            String response = aiClient.getSummaryFromAgent(null, "", "", 0, "", prompt, maxTokens);
            logger.debug("[Thread: {}] Received response from AI agent: '{}'", threadId, response);
            return response;
        } catch (Exception e) {
            String threadId = Thread.currentThread().getName();
            logger.warn("[Thread: {}] AI service error: {}", threadId, e.getMessage());
            return null;
        }
    }

    /**
     * Truncates a commit message to save input tokens while preserving meaning.
     */
    private String truncateMessage(String message) {
        if (message == null || message.length() <= 200) return message;
        return message.substring(0, 197) + "...";
    }

    /**
     * Formats and validates AI summary response to ensure exactly 2 lines.
     */
    private String formatSummaryResponse(String response) {
        logger.debug("formatSummaryResponse - INPUT: '{}'", response);

        if (response == null || response.trim().isEmpty()) {
            logger.debug("formatSummaryResponse - OUTPUT: null (empty input)");
            return null;
        }

        // Remove common AI prefixes
        String cleaned = response.trim();
        String[] prefixes = {"Summary:", "summary:", "Commit summary:", "commit summary:", "Output:", "output:"};
        for (String prefix : prefixes) {
            if (cleaned.startsWith(prefix)) {
                cleaned = cleaned.substring(prefix.length()).trim();
                logger.debug("formatSummaryResponse - Removed prefix '{}', new value: '{}'", prefix, cleaned);
                break;
            }
        }

        // Check if only whitespace after cleaning
        if (cleaned.trim().isEmpty()) {
            logger.debug("formatSummaryResponse - OUTPUT: null (whitespace only after prefix removal)");
            return null;
        }

        // Split on newlines first, then normalize whitespace within each line
        String[] rawLines = cleaned.replace("\r", "").split("\n");
        List<String> pieces = new ArrayList<>();
        for (String line : rawLines) {
            String trimmed = line.trim().replaceAll("\\s+", " ");
            if (!trimmed.isEmpty()) pieces.add(trimmed);
        }
        logger.debug("formatSummaryResponse - Split into {} pieces: {}", pieces.size(), pieces);

        // If AI already returned a single line, check if it's a complete sentence
        if (pieces.size() == 1) {
            String singleLine = pieces.get(0);
            singleLine = ensureSentencePunctuation(singleLine);

            // If it's a reasonably complete sentence, don't force two sentences
            if (singleLine.length() >= 15 && (singleLine.endsWith(".") || singleLine.endsWith("!") || singleLine.endsWith("?"))) {
                String result = truncateIfNeeded(singleLine);
                logger.debug("formatSummaryResponse - OUTPUT (single complete sentence): '{}'", result);
                return result;
            } else {
                // If it's too short or doesn't seem complete, ensure two sentences
                String result = ensureTwoSentences(singleLine);
                result = truncateIfNeeded(result);
                logger.debug("formatSummaryResponse - OUTPUT (ensured two sentences): '{}'", result);
                return result;
            }
        }

        String first = !pieces.isEmpty() ? pieces.get(0) : "";
        String second = pieces.size() > 1 ? pieces.get(1) : "Enhances codebase quality";
        first = ensureSentencePunctuation(first);
        second = ensureSentencePunctuation(second);
        String result = first + " " + second;
        result = truncateIfNeeded(result);
        logger.debug("formatSummaryResponse - OUTPUT (multi-line combined): '{}'", result);
        return result;
    }

    /**
     * Truncates response if it exceeds reasonable length (200 chars).
     */
    private String truncateIfNeeded(String text) {
        if (text == null || text.length() <= 200) {
            return text;
        }

        // Try to truncate at a sentence boundary
        int lastPeriod = text.lastIndexOf('.', 197);
        if (lastPeriod > 150) {
            return text.substring(0, lastPeriod + 1);
        }

        // Otherwise truncate with ellipsis
        return text.substring(0, 197) + "...";
    }

    private String ensureTwoSentences(String line) {
        // Attempt to split by sentence terminators. If fewer than 2, fabricate second.
        String trimmed = line.trim().replaceAll("\\s+", " ");
        List<String> sentences = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (char c : trimmed.toCharArray()) {
            current.append(c);
            if (c == '.' || c == '!' || c == '?') {
                String s = current.toString().trim();
                if (!s.isEmpty()) sentences.add(s);
                current.setLength(0);
                if (sentences.size() == 2) break;
            }
        }

        if (sentences.size() < 2 && current.length() > 0) {
            sentences.add(current.toString().trim());
        }

        if (sentences.isEmpty()) {
            return "Update applied. Improves code quality.";
        }

        if (sentences.size() == 1) {
            sentences.add("Improves code quality.");
        }

        if (sentences.size() >= 2) {
            sentences.set(0, ensureSentencePunctuation(sentences.get(0)));
            sentences.set(1, ensureSentencePunctuation(sentences.get(1)));
        }

        return sentences.get(0) + " " + sentences.get(1);
    }

    private String ensureSentencePunctuation(String s) {
        if (s == null) return "";
        s = s.trim();
        if (s.isEmpty()) return s;
        if (s.endsWith(".") || s.endsWith("!") || s.endsWith("?")) return s;
        return s + ".";
    }

    /**
     * Extracts all commits from a categorization structure.
     */
    private List<Commit> extractAllCommits(CommitCategorization categorization) {
        logger.info("=== COMMIT CATEGORIZATION BREAKDOWN ===");
        logger.info("Bug Fixes: {} commits", categorization.getBugFixes().size());
        logger.info("Features: {} commits", categorization.getFeatures().size());
        logger.info("Improvements: {} commits", categorization.getImprovements().size());
        logger.info("Others: {} commits", categorization.getOthers().size());
        logger.info("Total in categorization: {} commits", categorization.getTotalCount());

        List<Commit> allCommits = new ArrayList<>();
        allCommits.addAll(categorization.getBugFixes());
        allCommits.addAll(categorization.getFeatures());
        allCommits.addAll(categorization.getImprovements());
        allCommits.addAll(categorization.getOthers());

        logger.info("Extracted {} commits total for summary generation", allCommits.size());
        logger.info("=======================================");

        return allCommits;
    }

    private List<String> extractIssueRefs(String text) {
        List<String> refs = new ArrayList<>();
        if (text == null) return refs;
        // full URLs
        Matcher urlMatcher = Pattern.compile("https://github\\.com/[^/]+/[^/]+/issues/(\\d+)").matcher(text);
        while (urlMatcher.find()) {
            String ref = "#" + urlMatcher.group(1);
            if (!refs.contains(ref)) refs.add(ref);
        }
        // #123 pattern

        Matcher hashMatcher = Pattern.compile("(?<![A-Za-z0-9_])#(\\d+)").matcher(text);
        while (hashMatcher.find()) {
            String ref = "#" + hashMatcher.group(1);
            if (!refs.contains(ref)) refs.add(ref);
        }
        return refs;
    }

    private String truncate(String text, int max) {
        if (text == null) return "";
        if (text.length() <= max) return text;
        return text.substring(0, max - 3) + "...";
    }

    /**
     * Shuts down the thread pool gracefully.
     */
    public void shutdown() {
        logger.info("Shutting down ParallelCommitSummaryService");
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
                logger.warn("Thread pool did not terminate within 30 seconds, forcing shutdown");
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Result wrapper for individual commit summary operations.
     *
     * This class encapsulates the result of processing a single commit,
     * including both the commit identifier and the generated summary.
     */
    private static class CommitSummaryResult {
        final String commitSha;
        final String summary;

        /**
         * Creates a new commit summary result.
         *
         * @param commitSha The SHA identifier of the commit
         * @param summary The generated summary text
         */
        CommitSummaryResult(String commitSha, String summary) {
            this.commitSha = commitSha;
            this.summary = summary;
        }
    }
}
