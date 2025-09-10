package org.microsoft;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import org.mockito.Mockito;
import static org.mockito.ArgumentMatchers.any;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.microsoft.github.data.RepositoryData;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.microsoft.github.data.Issue;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.Contributor;
import java.lang.reflect.Method;
import java.util.ArrayList;

/**
 * Unit tests for the GitHubDataFetcher class.
 * <p>
 * These tests verify that the data fetcher correctly parses and returns repository data
 * such as issues, commits, pull requests, and contributors, and handles API errors gracefully.
 */
class GitHubDataFetcherTest {
    /**
     * Test that fetchData correctly parses empty lists for issues, commits, pull requests, and contributors.
     * <p>
     * This test uses Mockito to mock the HttpClient and HttpResponse objects, simulating empty responses
     * from the GitHub API for all endpoints. It verifies that the returned RepositoryData object contains
     * empty lists for all fields.
     */
    @Test
    void testFetchDataParsesIssuesCommitsPRsContributors() throws Exception {
        // Mock HTTP client and responses
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> issuesResponse = Mockito.mock(HttpResponse.class);
        HttpResponse<String> commitsResponse = Mockito.mock(HttpResponse.class);
        HttpResponse<String> prsResponse = Mockito.mock(HttpResponse.class);
        HttpResponse<String> contributorsResponse = Mockito.mock(HttpResponse.class);

        // Simulate GitHub API responses for each endpoint
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(issuesResponse)
            .thenReturn(commitsResponse)
            .thenReturn(prsResponse)
            .thenReturn(contributorsResponse);

        Mockito.when(issuesResponse.body()).thenReturn("[]");
        Mockito.when(commitsResponse.body()).thenReturn("[]");
        Mockito.when(prsResponse.body()).thenReturn("[]");
        Mockito.when(contributorsResponse.body()).thenReturn("[]");

        // Use the new constructor to inject the mock client
        GitHubDataFetcher fetcher = new GitHubDataFetcher("dummy-token", client);
        // Call fetchData and verify the results
        List<RepositoryData> data = fetcher.fetchData(List.of("https://github.com/apache/incubator-gluten"), "2025-09-01", "2025-09-09");
        assertEquals(1, data.size());
        assertEquals("apache/incubator-gluten", data.get(0).getRepoName());
        assertTrue(data.get(0).getIssues().isEmpty());
        assertTrue(data.get(0).getCommits().isEmpty());
        assertTrue(data.get(0).getPullRequests().isEmpty());
        assertTrue(data.get(0).getContributors().isEmpty());
    }

    /**
     * Test that fetchData throws an exception when an invalid repository URL is provided.
     * <p>
     * This test verifies that the data fetcher handles API errors gracefully by throwing an exception.
     */
    @Test
    void testFetchDataHandlesApiError() {
        GitHubDataFetcher fetcher = new GitHubDataFetcher("dummy-token");
        assertThrows(Exception.class, () -> fetcher.fetchData(List.of("invalid-url"), "2025-09-01", "2025-09-09"));
    }

    /**
     * Extended test class for GitHubDataFetcher to test individual data fetch methods.
     */
    @Test
    void testFetchIssues_parsesIssuesCorrectly() throws Exception {
        String json = "[{\"id\":1,\"number\":101,\"title\":\"Issue title\",\"state\":\"open\",\"created_at\":\"2025-09-01T00:00:00Z\",\"updated_at\":\"2025-09-02T00:00:00Z\",\"closed_at\":null,\"user\":{\"login\":\"user1\"},\"assignees\":[{\"login\":\"user2\"}],\"labels\":[{\"name\":\"bug\"}],\"comments\":5,\"body\":\"Issue body\",\"pull_request\":{}}]";
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn(json);
        GitHubDataFetcher fetcher = new GitHubDataFetcher("dummy-token", client);
        Method method = GitHubDataFetcher.class.getDeclaredMethod("fetchIssues", HttpClient.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Issue> issues = (ArrayList<Issue>) method.invoke(fetcher, client, "owner", "repo", "2025-09-01", "2025-09-09");
        assertEquals(1, issues.size());
        Issue issue = issues.get(0);
        assertEquals(1L, issue.getId());
        assertEquals(101, issue.getNumber());
        assertEquals("Issue title", issue.getTitle());
        assertEquals("open", issue.getState());
        assertEquals("user1", issue.getAuthorLogin());
        assertEquals(1, issue.getAssignees().size());
        assertEquals("user2", issue.getAssignees().get(0));
        assertEquals(1, issue.getLabels().size());
        assertEquals("bug", issue.getLabels().get(0));
        assertEquals(5, issue.getComments());
        assertEquals("Issue body", issue.getBody());
        assertTrue(issue.isPullRequest());
    }

    @Test
    void testFetchIssues_handlesEmptyList() throws Exception {
        String json = "[]";
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn(json);
        GitHubDataFetcher fetcher = new GitHubDataFetcher("dummy-token");
        Method method = GitHubDataFetcher.class.getDeclaredMethod("fetchIssues", HttpClient.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Issue> issues = (ArrayList<Issue>) method.invoke(fetcher, client, "owner", "repo", "2025-09-01", "2025-09-09");
        assertTrue(issues.isEmpty());
    }

    @Test
    void testFetchCommits_handlesEmptyList() throws Exception {
        String json = "[]";
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn(json);
        GitHubDataFetcher fetcher = new GitHubDataFetcher("dummy-token");
        Method method = GitHubDataFetcher.class.getDeclaredMethod("fetchCommits", HttpClient.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Commit> commits = (ArrayList<Commit>) method.invoke(fetcher, client, "owner", "repo", "2025-09-01", "2025-09-09");
        assertTrue(commits.isEmpty());
    }

    @Test
    void testFetchPullRequests_handlesEmptyList() throws Exception {
        String json = "[]";
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn(json);
        GitHubDataFetcher fetcher = new GitHubDataFetcher("dummy-token");
        Method method = GitHubDataFetcher.class.getDeclaredMethod("fetchPullRequests", HttpClient.class, String.class, String.class, String.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<PullRequest> prs = (ArrayList<PullRequest>) method.invoke(fetcher, client, "owner", "repo", "2025-09-01", "2025-09-09");
        assertTrue(prs.isEmpty());
    }

    @Test
    void testFetchContributors_handlesEmptyList() throws Exception {
        String json = "[]";
        HttpClient client = Mockito.mock(HttpClient.class);
        HttpResponse<String> response = Mockito.mock(HttpResponse.class);
        Mockito.when(client.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class))).thenReturn(response);
        Mockito.when(response.body()).thenReturn(json);
        GitHubDataFetcher fetcher = new GitHubDataFetcher("dummy-token");
        Method method = GitHubDataFetcher.class.getDeclaredMethod("fetchContributors", HttpClient.class, String.class, String.class);
        method.setAccessible(true);
        @SuppressWarnings("unchecked")
        ArrayList<Contributor> contributors = (ArrayList<Contributor>) method.invoke(fetcher, client, "owner", "repo");
        assertTrue(contributors.isEmpty());
    }
}
