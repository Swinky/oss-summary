package org.microsoft;

import java.net.http.*;
import java.net.URI;
import java.util.*;
import java.io.IOException;
import org.microsoft.github.data.RepositoryData;
import org.microsoft.github.data.Issue;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.Contributor;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Fetches GitHub repository data such as issues, commits, pull requests, and contributors.
 */
public class GitHubDataFetcher {
    private final String githubToken;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient client;

    /**
     * Constructs a GitHubDataFetcher with the provided GitHub token and default HttpClient.
     * @param githubToken GitHub personal access token for authentication
     */
    public GitHubDataFetcher(String githubToken) {
        this.githubToken = githubToken;
        this.client = HttpClient.newHttpClient();
    }

    /**
     * Constructs a GitHubDataFetcher with the provided GitHub token and custom HttpClient (for testing).
     * @param githubToken GitHub personal access token for authentication
     * @param client HttpClient instance to use
     */
    public GitHubDataFetcher(String githubToken, HttpClient client) {
        this.githubToken = githubToken;
        this.client = client;
    }

    /**
     * Fetches data for each repository in the given list within the specified date range.
     * @param repos List of repository URLs
     * @param startDate Start date (ISO 8601 format)
     * @param endDate End date (ISO 8601 format)
     * @return List of RepositoryData objects populated with fetched data
     */
    public List<RepositoryData> fetchData(List<String> repos, String startDate, String endDate) throws IOException, InterruptedException {
        List<RepositoryData> repoDataList = new ArrayList<>();
        for (String repoUrl : repos) {
            String[] parts = repoUrl.replace("https://github.com/", "").split("/");
            String owner = parts[0];
            String repo = parts[1];
            RepositoryData repoData = new RepositoryData();
            repoData.setRepoName(owner + "/" + repo);
            repoData.setIssues(fetchIssues(client, owner, repo, startDate, endDate));
            repoData.setCommits(fetchCommits(client, owner, repo, startDate, endDate));
            repoData.setPullRequests(fetchPullRequests(client, owner, repo, startDate, endDate));
            repoData.setContributors(fetchContributors(client, owner, repo));
            repoDataList.add(repoData);
        }
        return repoDataList;
    }

    /**
     * Fetches issues opened or updated since the start date.
     */
    private List<Issue> fetchIssues(HttpClient client, String owner, String repo, String startDate, String endDate) throws IOException, InterruptedException {
        String issuesUrl = String.format("https://api.github.com/repos/%s/%s/issues?since=%s&state=all", owner, repo, startDate);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(issuesUrl))
            .header("Authorization", "token " + githubToken)
            .build();
        System.out.println("[DEBUG] Fetching issues for repo: " + owner + "/" + repo);
        System.out.println("[DEBUG] Issues API request URL: " + issuesUrl);
        if (githubToken == null || githubToken.isEmpty()) {
            System.err.println("[ERROR] GITHUB_TOKEN is null or empty. Please set the environment variable correctly.");
        }
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        String rawResponse = response.body();
        JsonNode root = objectMapper.readTree(rawResponse);
        if (root.isObject() && root.has("message")) {
            System.err.println("[ERROR] Issues API error: " + root.get("message").asText());
            System.err.println("[ERROR] Full Issues API response: " + rawResponse);
        }
        List<Issue> issues = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
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
            // Date filtering
            boolean inRange = false;
            try {
                LocalDateTime created = LocalDateTime.parse(issue.getCreatedAt(), dtf);
                LocalDateTime updated = LocalDateTime.parse(issue.getUpdatedAt(), dtf);
                inRange = (created.isEqual(start) || created.isAfter(start)) && (created.isBefore(end) || created.isEqual(end))
                    || (updated.isEqual(start) || updated.isAfter(start)) && (updated.isBefore(end) || updated.isEqual(end));
            } catch (Exception e) {
                // Ignore parse errors, do not include
            }
            if (inRange) {
                issues.add(issue);
            }
        }
        return issues;
    }

    /**
     * Fetches commits within the date range.
     */
    private List<Commit> fetchCommits(HttpClient client, String owner, String repo, String startDate, String endDate) throws IOException, InterruptedException {
        String commitsUrl = String.format("https://api.github.com/repos/%s/%s/commits?since=%s&until=%s", owner, repo, startDate, endDate);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(commitsUrl))
            .header("Authorization", "token " + githubToken)
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        List<Commit> commits = new ArrayList<>();
        for (JsonNode node : root) {
            Commit commit = new Commit();
            commit.setSha(node.path("sha").asText());
            commit.setMessage(node.path("commit").path("message").asText());
            commit.setAuthorName(node.path("commit").path("author").path("name").asText());
            commit.setAuthorEmail(node.path("commit").path("author").path("email").asText());
            commit.setAuthorLogin(node.path("author").path("login").asText(null));
            commit.setDate(node.path("commit").path("author").path("date").asText());
            commits.add(commit);
        }
        return commits;
    }

    /**
     * Fetches pull requests with most activity in the date range.
     */
    private List<PullRequest> fetchPullRequests(HttpClient client, String owner, String repo, String startDate, String endDate) throws IOException, InterruptedException {
        String prsUrl = String.format("https://api.github.com/repos/%s/%s/pulls?state=all&sort=updated&direction=desc", owner, repo);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(prsUrl))
            .header("Authorization", "token " + githubToken)
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        JsonNode root = objectMapper.readTree(response.body());
        List<PullRequest> prs = new ArrayList<>();
        DateTimeFormatter dtf = DateTimeFormatter.ISO_DATE_TIME;
        LocalDateTime start = LocalDateTime.parse(startDate + "T00:00:00");
        LocalDateTime end = LocalDateTime.parse(endDate + "T23:59:59");
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
            pr.setLabels(new ArrayList<>());
            for (JsonNode label : node.path("labels")) {
                pr.getLabels().add(label.path("name").asText());
            }
            pr.setComments(node.path("comments").asInt());
            pr.setBody(node.path("body").asText(""));
            pr.setCommits(node.path("commits").asInt(0));
            pr.setAdditions(node.path("additions").asInt(0));
            pr.setDeletions(node.path("deletions").asInt(0));
            pr.setChangedFiles(node.path("changed_files").asInt(0));
            prs.add(pr);
        }
        return prs;
    }

    /**
     * Fetches top contributors by commit count.
     */
    private List<Contributor> fetchContributors(HttpClient client, String owner, String repo) throws IOException, InterruptedException {
        String contributorsUrl = String.format("https://api.github.com/repos/%s/%s/contributors", owner, repo);
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(contributorsUrl))
            .header("Authorization", "token " + githubToken)
            .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
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
}
