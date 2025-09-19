package org.microsoft.github.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.microsoft.github.data.RepositoryData;
import org.microsoft.github.data.Issue;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.Contributor;
import org.microsoft.utils.BotFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Unified service for all GitHub data operations including fetching repository data,
 * PR descriptions, issue descriptions, and other GitHub API interactions.
 *
 * This service combines the functionality of the former GitHubService and GitHubDataFetcher
 * classes to provide a single, comprehensive interface for GitHub operations.
 */
public class GitHubService {
    private static final Logger logger = LoggerFactory.getLogger(GitHubService.class);
    private static final Pattern PR_ID_PATTERN = Pattern.compile("\\(\\s*(\\d+)\\s*\\)$");
    private static final String GITHUB_API_BASE = "https://api.github.com";

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final String githubToken;

    /**
     * Creates a new GitHubService with the provided GitHub token.
     * @param githubToken GitHub personal access token for authentication
     */
    public GitHubService(String githubToken) {
        this.httpClient = HttpClient.newHttpClient();
        this.objectMapper = new ObjectMapper();
        this.githubToken = githubToken;
    }

    /**
     * Creates a new GitHubService with the provided GitHub token and custom HttpClient (for testing).
     * @param githubToken GitHub personal access token for authentication
     * @param httpClient Custom HttpClient instance to use
     */
    public GitHubService(String githubToken, HttpClient httpClient) {
        this.httpClient = httpClient;
        this.objectMapper = new ObjectMapper();
        this.githubToken = githubToken;
    }

    /**
     * Legacy constructor for backward compatibility with repositoryOwner/repositoryName parameters.
     * @param repositoryOwner Repository owner (not used in new implementation)
     * @param repositoryName Repository name (not used in new implementation)
     * @param accessToken GitHub access token
     * @deprecated Use GitHubService(String githubToken) instead
     */
    @Deprecated
    public GitHubService(String repositoryOwner, String repositoryName, String accessToken) {
        this(accessToken);
    }

    // ==================== REPOSITORY DATA FETCHING METHODS ====================

    /**
     * Fetches data for each repository in the given list within the specified date range.
     * @param repos List of repository URLs
     * @param startDate Start date (ISO 8601 format)
     * @param endDate End date (ISO 8601 format)
     * @param teamMembers List of team member usernames for filtering activity
     * @return List of RepositoryData objects populated with fetched data
     */
    public List<RepositoryData> fetchData(List<String> repos, String startDate, String endDate, List<String> teamMembers) throws IOException, InterruptedException {
        List<RepositoryData> repoDataList = new ArrayList<>();
        for (String repoUrl : repos) {
            String[] parts = repoUrl.replace("https://github.com/", "").split("/");
            String owner = parts[0];
            String repo = parts[1];
            RepositoryData repoData = new RepositoryData();
            repoData.setRepoName(owner + "/" + repo);

            List<Issue> allIssues = fetchIssues(owner, repo, startDate, endDate);
            List<Commit> allCommits = fetchCommits(owner, repo, startDate, endDate);
            List<PullRequest> allPRs = fetchPullRequests(owner, repo, startDate, endDate);

            // Enrich commits with PR information for better AI summaries
            enrichCommitsWithPRInfo(allCommits, owner, repo);

            repoData.setIssues(allIssues);
            repoData.setCommits(allCommits);
            repoData.setPullRequests(allPRs);

            // Filter for team member activity
            List<Issue> teamIssues = allIssues.stream().filter(i -> teamMembers.contains(i.getAuthorLogin())).collect(Collectors.toList());
            List<Commit> teamCommits = allCommits.stream().filter(c -> teamMembers.contains(c.getAuthorLogin())).collect(Collectors.toList());
            List<PullRequest> teamPRs = allPRs.stream().filter(pr -> teamMembers.contains(pr.getAuthorLogin())).collect(Collectors.toList());

            repoData.setTeamIssues(teamIssues);
            repoData.setTeamCommits(teamCommits);
            repoData.setTeamPRs(teamPRs);

            // Debug logging
            logger.debug("Repository: {}/{}", owner, repo);
            logger.debug("Date range: {} to {}", startDate, endDate);
            logger.debug("Total issues found: {}", allIssues.size());
            logger.debug("Total commits found: {}", allCommits.size());
            logger.debug("Total PRs found: {}", allPRs.size());
            logger.debug("Team members configured: {}", teamMembers);
            logger.debug("Team issues found: {}", teamIssues.size());
            logger.debug("Team commits found: {}", teamCommits.size());
            logger.debug("Team PRs found: {}", teamPRs.size());

            if (allCommits.size() > 0) {
                logger.debug("Sample of actual commit authors:");
                allCommits.stream().limit(5).forEach(c ->
                    logger.debug("  - {}: {}", c.getAuthorLogin(),
                                c.getMessage().substring(0, Math.min(50, c.getMessage().length()))));
            }

            repoDataList.add(repoData);
        }
        return repoDataList;
    }

    /**
     * Fetches issues created within the specified date range.
     * Only includes issues that were actually created in the date range (not just updated).
     */
    public List<Issue> fetchIssues(String owner, String repo, String startDate, String endDate) throws IOException, InterruptedException {
        String issuesUrl = String.format("%s/repos/%s/%s/issues?since=%s&state=all&sort=created&direction=desc&per_page=100",
                                        GITHUB_API_BASE, owner, repo, startDate);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(issuesUrl))
            .header("Authorization", "token " + githubToken)
            .build();

        logger.debug("Fetching issues for repo: {}/{}", owner, repo);
        logger.debug("Issues API request URL: {}", issuesUrl);
        logger.debug("Date range filter: {} to {}", startDate, endDate);

        if (githubToken == null || githubToken.isEmpty()) {
            System.err.println("[ERROR] GITHUB_TOKEN is null or empty. Please set the environment variable correctly.");
        }

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        String rawResponse = response.body();
        JsonNode root = objectMapper.readTree(rawResponse);

        if (root.isObject() && root.has("message")) {
            System.err.println("[ERROR] Issues API error: " + root.get("message").asText());
            System.err.println("[ERROR] Full Issues API response: " + rawResponse);
            return new ArrayList<>();
        }

        List<Issue> issues = new ArrayList<>();
        logger.debug("Filtering issues by creation date between {} and {}", startDate, endDate);

        for (JsonNode node : root) {
            Issue issue = new Issue();
            issue.setId(node.path("id").asLong());
            issue.setNumber(node.path("number").asInt());
            issue.setTitle(node.path("title").asText());
            issue.setState(node.path("state").asText());
            issue.setCreatedAt(node.path("created_at").asText());
            issue.setUpdatedAt(node.path("updated_at").asText());
            issue.setClosedAt(node.path("closed_at").asText(null));
            issue.setAuthorLogin(node.path("user").path("login").asText());
            issue.setAssignees(new ArrayList<>());
            for (JsonNode assignee : node.path("assignees")) {
                issue.getAssignees().add(assignee.path("login").asText());
            }
            issue.setLabels(new ArrayList<>());
            for (JsonNode label : node.path("labels")) {
                issue.getLabels().add(label.path("name").asText());
            }
            issue.setComments(node.path("comments").asInt());
            issue.setBody(node.path("body").asText(""));
            issue.setPullRequest(node.has("pull_request"));

            // Use consistent date filtering - only include issues created within the date range
            if (isWithinDateRange(issue.getCreatedAt(), startDate, endDate)) {
                // Filter out bot activity immediately
                if (!BotFilter.isBotIssue(issue)) {
                    issues.add(issue);
                    logger.debug("Including issue #{} - createdAt: {}", issue.getNumber(), issue.getCreatedAt());
                } else {
                    logger.debug("Filtered out bot issue #{}", issue.getNumber());
                }
            } else {
                logger.debug("Filtered out issue #{} - createdAt: {} not in range {} to {}", issue.getNumber(), issue.getCreatedAt(), startDate, endDate);
            }
        }

        logger.debug("Found {} issues in date range", issues.size());
        return issues;
    }

    /**
     * Checks if a date string is within the specified date range (inclusive).
     * Handles both ISO datetime format and simple date format.
     *
     * @param dateStr The date string to check (e.g., "2025-09-12T18:51:35Z" or "2025-09-12")
     * @param startDateStr The start date (inclusive) in format "yyyy-MM-dd"
     * @param endDateStr The end date (inclusive) in format "yyyy-MM-dd"
     * @return true if the date is within the range, false otherwise
     */
    public static boolean isWithinDateRange(String dateStr, String startDateStr, String endDateStr) {
        if (dateStr == null) return false;
        if (startDateStr == null && endDateStr == null) return true;

        // Extract just the date part from ISO datetime (e.g., "2025-09-12T18:51:35Z" -> "2025-09-12")
        String dateOnly = dateStr.contains("T") ? dateStr.substring(0, dateStr.indexOf("T")) : dateStr;

        if (startDateStr == null) {
            // Only end date specified - include if date is <= end date
            return dateOnly.compareTo(endDateStr) <= 0;
        }
        if (endDateStr == null) {
            // Only start date specified - include if date is >= start date
            return dateOnly.compareTo(startDateStr) >= 0;
        }

        // Both dates specified - include if date is within range (inclusive)
        return dateOnly.compareTo(startDateStr) >= 0 && dateOnly.compareTo(endDateStr) <= 0;
    }

    /**
     * Fetches commits within the date range.
     */
    public List<Commit> fetchCommits(String owner, String repo, String startDate, String endDate) throws IOException, InterruptedException {
        String commitsUrl = String.format("%s/repos/%s/%s/commits?since=%s&until=%s", GITHUB_API_BASE, owner, repo, startDate, endDate);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(commitsUrl))
            .header("Authorization", "token " + githubToken)
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        List<Commit> commits = new ArrayList<>();
        int botsFiltered = 0;

        for (JsonNode node : root) {
            Commit commit = new Commit();
            commit.setSha(node.path("sha").asText());
            commit.setMessage(node.path("commit").path("message").asText());
            commit.setAuthorName(node.path("commit").path("author").path("name").asText());
            commit.setAuthorEmail(node.path("commit").path("author").path("email").asText());
            commit.setAuthorLogin(node.path("author").path("login").asText(null));
            commit.setDate(node.path("commit").path("author").path("date").asText());

            // Filter out bot commits at the source - don't even store them
            if (BotFilter.isBotCommit(commit)) {
                botsFiltered++;
                continue; // Skip this commit entirely
            }

            commits.add(commit);
        }

        logger.debug("Commits: {} included, {} bot commits filtered out", commits.size(), botsFiltered);
        return commits;
    }

    /**
     * Fetches pull requests with most activity in the date range.
     */
    public List<PullRequest> fetchPullRequests(String owner, String repo, String startDate, String endDate) throws IOException, InterruptedException {
        String prsUrl = String.format("%s/repos/%s/%s/pulls?state=all&sort=updated&direction=desc", GITHUB_API_BASE, owner, repo);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(prsUrl))
            .header("Authorization", "token " + githubToken)
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        List<PullRequest> prs = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
        int botsFiltered = 0;

        for (JsonNode node : root) {
            String createdAt = node.path("created_at").asText();
            String updatedAt = node.path("updated_at").asText();
            boolean inRange = false;
            try {
                LocalDateTime created = LocalDateTime.parse(createdAt, dtf);
                LocalDateTime updated = LocalDateTime.parse(updatedAt, dtf);
                inRange = (created.isEqual(start) || created.isAfter(start)) && (created.isBefore(end) || created.isEqual(end))
                    || (updated.isEqual(start) || updated.isAfter(start)) && (updated.isBefore(end) || updated.isEqual(end));
            } catch (Exception e) {
                // Ignore parse errors, do not include
            }
            if (!inRange) continue;

            PullRequest pr = new PullRequest();
            pr.setId(node.path("id").asLong());
            pr.setNumber(node.path("number").asInt());
            pr.setTitle(node.path("title").asText());
            pr.setState(node.path("state").asText());
            pr.setCreatedAt(createdAt);
            pr.setUpdatedAt(updatedAt);
            pr.setClosedAt(node.path("closed_at").asText(null));
            pr.setMergedAt(node.path("merged_at").asText(null));
            pr.setAuthorLogin(node.path("user").path("login").asText());
            pr.setAssignees(new ArrayList<>());
            for (JsonNode assignee : node.path("assignees")) {
                pr.getAssignees().add(assignee.path("login").asText());
            }
            pr.setBody(node.path("body").asText(""));

            // Filter out bot PRs at the source - don't even store them
            if (BotFilter.isBotPR(pr)) {
                botsFiltered++;
                continue; // Skip this PR entirely
            }

            prs.add(pr);
        }

        logger.debug("PRs: {} included, {} bot PRs filtered out", prs.size(), botsFiltered);
        return prs;
    }

    /**
     * Fetches top contributors by commit count.
     */
    public List<Contributor> fetchContributors(String owner, String repo) throws IOException, InterruptedException {
        String contributorsUrl = String.format("%s/repos/%s/%s/contributors", GITHUB_API_BASE, owner, repo);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(contributorsUrl))
            .header("Authorization", "token " + githubToken)
            .build();
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        List<Contributor> contributors = new ArrayList<>();
        for (JsonNode node : root) {
            Contributor contributor = new Contributor();
            contributor.setLogin(node.path("login").asText());
            contributor.setId(node.path("id").asLong());
            contributor.setContributions(node.path("contributions").asInt());
            contributor.setAvatarUrl(node.path("avatar_url").asText());
            contributor.setUrl(node.path("url").asText());
            contributors.add(contributor);
        }
        return contributors;
    }

    // ==================== PR/ISSUE DESCRIPTION METHODS ====================

    /**
     * Fetches the description of a pull request from GitHub API.
     * @param owner Repository owner
     * @param repo Repository name
     * @param prNumber The pull request number
     * @return The PR description or null if not found/error
     */
    public String fetchPRDescription(String owner, String repo, int prNumber) {
        try {
            String url = String.format("%s/repos/%s/%s/pulls/%d",
                GITHUB_API_BASE, owner, repo, prNumber);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + githubToken)
                .header("Accept", "application/vnd.github.v3+json")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                JsonNode prNode = objectMapper.readTree(response.body());
                JsonNode bodyNode = prNode.get("body");
                return bodyNode != null && !bodyNode.isNull() ? bodyNode.asText() : "";
            }
        } catch (IOException | InterruptedException e) {
            // Log error in production code
            System.err.println("Error fetching PR description for #" + prNumber + ": " + e.getMessage());
        }
        return null;
    }

    /**
     * Legacy method for backward compatibility with repositoryOwner/repositoryName parameters.
     * @param prNumber The pull request number
     * @return The PR description or null if not found/error
     * @deprecated Use fetchPRDescription(String owner, String repo, int prNumber) instead
     */
    @Deprecated
    public String fetchPRDescription(int prNumber) {
        throw new UnsupportedOperationException("This method requires owner and repo parameters. Use fetchPRDescription(String owner, String repo, int prNumber) instead.");
    }

    /**
     * Extracts PR ID from commit message in format "title(PR_ID)".
     * Returns null if there are multiple numbers in parentheses to avoid ambiguity.
     * @param commitMessage The commit message to parse
     * @return The extracted PR ID or null if not found or ambiguous
     */
    public Integer extractPRId(String commitMessage) {
        if (commitMessage == null) return null;

        String trimmed = commitMessage.trim();

        // Check if there's a number in parentheses at the end
        Matcher endMatcher = PR_ID_PATTERN.matcher(trimmed);
        if (!endMatcher.find()) {
            return null;
        }

        // Count total occurrences of numbers in parentheses to detect ambiguity
        Pattern allParensPattern = Pattern.compile("\\(\\s*(\\d+)\\s*\\)");
        Matcher allMatcher = allParensPattern.matcher(trimmed);
        int count = 0;
        while (allMatcher.find()) {
            count++;
        }

        // Only return the PR ID if there's exactly one occurrence
        if (count == 1) {
            return Integer.parseInt(endMatcher.group(1));
        }

        return null; // Multiple occurrences - ambiguous
    }

    /**
     * Extracts PR number from commit message patterns like "Merge pull request #123" or "(#123)"
     */
    private Integer extractPRNumber(String commitMessage) {
        if (commitMessage == null) return null;

        // Pattern 1: "(#123)" at end of message - more specific pattern first
        Pattern endParenPattern = Pattern.compile("\\(#(\\d+)\\)\\s*$");
        Matcher endParenMatcher = endParenPattern.matcher(commitMessage);
        if (endParenMatcher.find()) {
            return Integer.parseInt(endParenMatcher.group(1));
        }

        // Pattern 2: "Merge pull request #123"
        Pattern mergePattern = Pattern.compile("Merge pull request #(\\d+)");
        Matcher mergeMatcher = mergePattern.matcher(commitMessage);
        if (mergeMatcher.find()) {
            return Integer.parseInt(mergeMatcher.group(1));
        }

        // Pattern 3: "(#123)" anywhere in message (more general)
        Pattern parenPattern = Pattern.compile("\\(#(\\d+)\\)");
        Matcher parenMatcher = parenPattern.matcher(commitMessage);
        if (parenMatcher.find()) {
            return Integer.parseInt(parenMatcher.group(1));
        }

        // Pattern 4: "#123" anywhere in message (least specific, as fallback)
        Pattern hashPattern = Pattern.compile("(?<!\\w)#(\\d+)(?!\\w)");
        Matcher hashMatcher = hashPattern.matcher(commitMessage);
        if (hashMatcher.find()) {
            return Integer.parseInt(hashMatcher.group(1));
        }

        return null;
    }

    /**
     * Enriches commits with pull request information by fetching PR details for commits that reference PRs.
     */
    public void enrichCommitsWithPRInfo(List<Commit> commits, String owner, String repo) throws IOException, InterruptedException {
        logger.debug("Enriching {} commits with PR information", commits.size());
        int enrichedCount = 0;

        for (Commit commit : commits) {
            try {
                // Extract PR number from commit message
                Integer prNumber = extractPRNumber(commit.getMessage());
                if (prNumber != null) {
                    commit.setPrId(prNumber);

                    // Fetch PR details
                    String prDescription = fetchPRDescription(owner, repo, prNumber);
                    if (prDescription != null && !prDescription.trim().isEmpty()) {
                        commit.setPrDescription(prDescription);
                        enrichedCount++;
                        logger.debug("Enriched commit {} with PR #{} description", commit.getSha().substring(0, 8), prNumber);
                    }
                }
            } catch (Exception e) {
                logger.debug("Failed to enrich commit {} with PR info: {}", commit.getSha(), e.getMessage());
            }
        }

        logger.debug("Successfully enriched {} commits with PR descriptions", enrichedCount);
    }
}
