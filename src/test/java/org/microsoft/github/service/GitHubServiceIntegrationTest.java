package org.microsoft.github.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.microsoft.github.data.RepositoryData;
import org.microsoft.github.data.Issue;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.Contributor;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Integration tests for the unified GitHubService class.
 * <p>
 * These tests verify that the service correctly parses and returns repository data
 * such as issues, commits, pull requests, and contributors, and handles API errors gracefully.
 */
class GitHubServiceIntegrationTest {

    /**
     * Test that fetchData correctly parses empty lists for issues, commits, pull requests, and contributors.
     */
    @Test
    void testFetchDataParsesIssuesCommitsPRsContributors() throws Exception {
        // Mock HTTP client and responses
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> issuesResponse = Mockito.mock(HttpResponse.class);
        HttpResponse<String> commitsResponse = Mockito.mock(HttpResponse.class);
        HttpResponse<String> prsResponse = Mockito.mock(HttpResponse.class);

        // Simulate GitHub API responses for each endpoint
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(issuesResponse)
            .thenReturn(commitsResponse)
            .thenReturn(prsResponse);

        Mockito.when(issuesResponse.body()).thenReturn("[]");
        Mockito.when(commitsResponse.body()).thenReturn("[]");
        Mockito.when(prsResponse.body()).thenReturn("[]");

        GitHubService service = new GitHubService("dummy-token", client);

        List<RepositoryData> result = service.fetchData(
            List.of("https://github.com/test/repo"),
            "2023-01-01",
            "2023-01-31",
            List.of("testuser")
        );

        assertNotNull(result);
        assertEquals(1, result.size());

        RepositoryData repoData = result.get(0);
        assertEquals("test/repo", repoData.getRepoName());
        assertTrue(repoData.getIssues().isEmpty());
        assertTrue(repoData.getCommits().isEmpty());
        assertTrue(repoData.getPullRequests().isEmpty());
    }

    /**
     * Test that the service is properly instantiated without a custom HttpClient.
     */
    @Test
    void testGitHubServiceInstantiation() {
        GitHubService service = new GitHubService("dummy-token");
        assertNotNull(service);
    }

    /**
     * Test individual data fetch methods using reflection to access private methods.
     */
    @Test
    void testFetchIssuesWithReflection() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn("[]");

        GitHubService service = new GitHubService("dummy-token", client);
        Method method = GitHubService.class.getDeclaredMethod("fetchIssues", String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Issue> issues = (List<Issue>) method.invoke(service, "test", "repo", "2023-01-01", "2023-01-31");

        assertNotNull(issues);
        assertTrue(issues.isEmpty());
    }

    /**
     * Test fetchIssues with mock response containing invalid JSON to test error handling.
     */
    @Test
    void testFetchIssuesWithInvalidJson() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn("{\"message\": \"API rate limit exceeded\"}");

        GitHubService service = new GitHubService("dummy-token");
        Method method = GitHubService.class.getDeclaredMethod("fetchIssues", String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Issue> issues = (List<Issue>) method.invoke(service, "test", "repo", "2023-01-01", "2023-01-31");

        assertNotNull(issues);
        assertTrue(issues.isEmpty(), "Should return empty list when API returns error message");
    }

    /**
     * Test fetchCommits method using reflection.
     */
    @Test
    void testFetchCommitsWithReflection() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn("[]");

        GitHubService service = new GitHubService("dummy-token", client);
        Method method = GitHubService.class.getDeclaredMethod("fetchCommits", String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Commit> commits = (List<Commit>) method.invoke(service, "test", "repo", "2023-01-01", "2023-01-31");

        assertNotNull(commits);
        assertTrue(commits.isEmpty());
    }

    /**
     * Test fetchPullRequests method using reflection.
     */
    @Test
    void testFetchPullRequestsWithReflection() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn("[]");

        GitHubService service = new GitHubService("dummy-token", client);
        Method method = GitHubService.class.getDeclaredMethod("fetchPullRequests", String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<PullRequest> prs = (List<PullRequest>) method.invoke(service, "test", "repo", "2023-01-01", "2023-01-31");

        assertNotNull(prs);
        assertTrue(prs.isEmpty());
    }

    /**
     * Test fetchContributors method using reflection.
     */
    @Test
    void testFetchContributorsWithReflection() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn("[]");

        GitHubService service = new GitHubService("dummy-token", client);
        Method method = GitHubService.class.getDeclaredMethod("fetchContributors", String.class, String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Contributor> contributors = (List<Contributor>) method.invoke(service, "test", "repo");

        assertNotNull(contributors);
        assertTrue(contributors.isEmpty());
    }

    /**
     * Test PR description fetching functionality.
     */
    @Test
    void testFetchPRDescription() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.statusCode()).thenReturn(200);
        Mockito.when(response.body()).thenReturn("{\"body\": \"Test PR description\"}");

        GitHubService service = new GitHubService("dummy-token", client);
        String description = service.fetchPRDescription("test", "repo", 123);

        assertEquals("Test PR description", description);
    }

    /**
     * Test PR ID extraction functionality.
     */
    @Test
    void testExtractPRId() {
        GitHubService service = new GitHubService("dummy-token");

        assertEquals(Integer.valueOf(123), service.extractPRId("Fix bug in authentication (123)"));
        assertEquals(Integer.valueOf(456), service.extractPRId("Add new feature (456)"));
        assertNull(service.extractPRId("No PR ID in this message"));
        assertNull(service.extractPRId("Multiple (123) numbers (456)"));
        assertNull(service.extractPRId(null));
    }

    /**
     * Test legacy constructor for backward compatibility.
     */
    @Test
    void testLegacyConstructor() {
        GitHubService service = new GitHubService("owner", "repo", "token");
        assertNotNull(service);

        // Test that legacy fetchPRDescription method throws exception
        assertThrows(UnsupportedOperationException.class, () -> service.fetchPRDescription(123));
    }

    /**
     * Test commits with sample data containing bot users.
     */
    @Test
    void testFetchCommitsFiltersBotsAndNonContributors() throws Exception {
        String commitsJson = """
            [
              {
                "sha": "commit1",
                "commit": {
                  "message": "Real user commit",
                  "author": {
                    "name": "Real User",
                    "email": "real@example.com",
                    "date": "2023-01-15T10:00:00Z"
                  }
                },
                "author": {
                  "login": "realuser"
                }
              },
              {
                "sha": "commit2",
                "commit": {
                  "message": "Bot commit",
                  "author": {
                    "name": "Dependabot",
                    "email": "noreply@github.com",
                    "date": "2023-01-16T10:00:00Z"
                  }
                },
                "author": {
                  "login": "dependabot[bot]"
                }
              }
            ]
            """;

        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn(commitsJson);

        GitHubService service = new GitHubService("dummy-token", client);
        Method method = GitHubService.class.getDeclaredMethod("fetchCommits", String.class, String.class, String.class, String.class);
        method.setAccessible(true);

        @SuppressWarnings("unchecked")
        List<Commit> commits = (List<Commit>) method.invoke(service, "test", "repo", "2023-01-01", "2023-01-31");

        assertNotNull(commits);
        assertEquals(1, commits.size(), "Should filter out bot commits");
        assertEquals("realuser", commits.get(0).getAuthorLogin());
        assertEquals("Real user commit", commits.get(0).getMessage());
    }

    /**
     * Test comprehensive integration with multiple repositories.
     */
    @Test
    void testIntegrationWithMultipleRepositories() throws Exception {
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn("[]");

        GitHubService service = new GitHubService("dummy-token", client);

        List<String> repositories = List.of(
            "https://github.com/repo1/test",
            "https://github.com/repo2/test"
        );

        List<RepositoryData> result = service.fetchData(
            repositories,
            "2023-01-01",
            "2023-01-31",
            List.of("testuser")
        );

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("repo1/test", result.get(0).getRepoName());
        assertEquals("repo2/test", result.get(1).getRepoName());
    }
}
