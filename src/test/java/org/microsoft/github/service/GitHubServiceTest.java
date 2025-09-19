package org.microsoft.github.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GitHubServiceTest {

    @Mock
    private HttpClient httpClient;

    @Mock
    private HttpResponse<String> httpResponse;

    private GitHubService gitHubService;

    @BeforeEach
    void setUp() {
        gitHubService = new GitHubService("testtoken", httpClient);
    }

    @Test
    void testExtractPRId_validFormat() {
        // Test valid PR ID extraction
        assertEquals(Integer.valueOf(123), gitHubService.extractPRId("Fix authentication bug (123)"));
        assertEquals(Integer.valueOf(456), gitHubService.extractPRId("Add new feature (456)"));
        assertEquals(Integer.valueOf(789), gitHubService.extractPRId("Update documentation (789)"));
    }

    @Test
    void testExtractPRId_invalidFormat() {
        // Test invalid formats
        assertNull(gitHubService.extractPRId("Fix bug without PR"));
        assertNull(gitHubService.extractPRId("Fix bug (123"));
        assertNull(gitHubService.extractPRId("Fix bug 123)"));
        assertNull(gitHubService.extractPRId("Fix bug (#123)"));
        assertNull(gitHubService.extractPRId(""));
        assertNull(gitHubService.extractPRId(null));
    }

    @Test
    void testExtractPRId_multipleNumbers() {
        // Test ambiguous cases with multiple numbers in parentheses
        assertNull(gitHubService.extractPRId("Fix bug (123) and issue (456)"));
        assertNull(gitHubService.extractPRId("Multiple (111) numbers (222) here (333)"));
    }

    @Test
    void testExtractPRId_spaceInParentheses() {
        // Test with spaces in parentheses
        assertEquals(Integer.valueOf(123), gitHubService.extractPRId("Fix bug ( 123 )"));
        assertNull(gitHubService.extractPRId("Fix bug ( abc )"));
    }

    @Test
    void testFetchPRDescription_success() throws IOException, InterruptedException {
        // Mock successful API response
        String jsonResponse = "{\"body\":\"This is a test PR description\"}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        String description = gitHubService.fetchPRDescription("testowner", "testrepo", 123);
        assertEquals("This is a test PR description", description);
    }

    @Test
    void testFetchPRDescription_notFound() throws IOException, InterruptedException {
        // Mock 404 response
        when(httpResponse.statusCode()).thenReturn(404);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        String description = gitHubService.fetchPRDescription("testowner", "testrepo", 999);
        assertNull(description);
    }

    @Test
    void testFetchPRDescription_nullBody() throws IOException, InterruptedException {
        // Mock response with null body
        String jsonResponse = "{\"body\":null}";
        when(httpResponse.statusCode()).thenReturn(200);
        when(httpResponse.body()).thenReturn(jsonResponse);
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenReturn(httpResponse);

        String description = gitHubService.fetchPRDescription("testowner", "testrepo", 123);
        assertEquals("", description);
    }

    @Test
    void testFetchPRDescription_ioException() throws IOException, InterruptedException {
        // Mock IOException
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new IOException("Network error"));

        String description = gitHubService.fetchPRDescription("testowner", "testrepo", 123);
        assertNull(description);
    }

    @Test
    void testFetchPRDescription_interruptedException() throws IOException, InterruptedException {
        // Mock InterruptedException
        when(httpClient.send(any(HttpRequest.class), any(HttpResponse.BodyHandler.class)))
            .thenThrow(new InterruptedException("Thread interrupted"));

        String description = gitHubService.fetchPRDescription("testowner", "testrepo", 123);
        assertNull(description);
    }

    @Test
    void testLegacyConstructor() {
        // Test that legacy constructor works but deprecated method throws exception
        GitHubService legacyService = new GitHubService("owner", "repo", "token");
        assertNotNull(legacyService);

        // Test that the deprecated method throws UnsupportedOperationException
        assertThrows(UnsupportedOperationException.class, () -> legacyService.fetchPRDescription(123));
    }

    @Test
    void testServiceInstantiation() {
        GitHubService service1 = new GitHubService("token");
        GitHubService service2 = new GitHubService("token", HttpClient.newHttpClient());

        assertNotNull(service1);
        assertNotNull(service2);
    }
}
