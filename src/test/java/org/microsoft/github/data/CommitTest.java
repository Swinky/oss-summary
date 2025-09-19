package org.microsoft.github.data;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.microsoft.github.service.GitHubService;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CommitTest {

    @Mock
    private GitHubService gitHubService;

    private Commit commit;

    @BeforeEach
    void setUp() {
        commit = new Commit();
        commit.setSha("abc123");
        commit.setMessage("Fix authentication bug(456)");
        commit.setAuthorName("John Doe");
        commit.setAuthorEmail("john@example.com");
        commit.setAuthorLogin("johndoe");
        commit.setDate("2025-09-18");
    }

    @Test
    void testEqualsAndHashCode() {
        Commit c1 = new Commit();
        c1.setSha("abc");
        c1.setMessage("msg");
        c1.setAuthorName("name");
        c1.setAuthorEmail("email");
        c1.setAuthorLogin("login");
        c1.setDate("2025-09-09");
        c1.setSummary("summary");
        c1.setPrDescription("pr desc");
        c1.setPrId(123);

        Commit c2 = new Commit();
        c2.setSha("abc");
        c2.setMessage("msg");
        c2.setAuthorName("name");
        c2.setAuthorEmail("email");
        c2.setAuthorLogin("login");
        c2.setDate("2025-09-09");
        c2.setSummary("summary");
        c2.setPrDescription("pr desc");
        c2.setPrId(123);

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testToString() {
        Commit commit = new Commit();
        commit.setSha("abc");
        commit.setPrId(123);
        commit.setPrDescription("Test PR description");
        String str = commit.toString();
        assertTrue(str.contains("abc"));
        assertTrue(str.contains("123"));
        assertTrue(str.contains("Test PR description"));
    }

    @Test
    void testEnrichWithPRDescription_success() {
        String prDescription = "This PR fixes the authentication bug by updating the token validation logic.";

        when(gitHubService.extractPRId("Fix authentication bug(456)")).thenReturn(456);
        when(gitHubService.fetchPRDescription(456)).thenReturn(prDescription);

        commit.enrichWithPRDescription(gitHubService);

        assertEquals(Integer.valueOf(456), commit.getPrId());
        assertEquals(prDescription, commit.getPrDescription());
        assertTrue(commit.getMessage().contains("PR Description:"));
        assertTrue(commit.getMessage().contains(prDescription));
    }

    @Test
    void testEnrichWithPRDescription_noPRId() {
        commit.setMessage("Regular commit message without PR");

        when(gitHubService.extractPRId("Regular commit message without PR")).thenReturn(null);

        commit.enrichWithPRDescription(gitHubService);

        assertNull(commit.getPrId());
        assertNull(commit.getPrDescription());
        assertEquals("Regular commit message without PR", commit.getMessage());
    }

    @Test
    void testEnrichWithPRDescription_nullMessage() {
        commit.setMessage(null);

        commit.enrichWithPRDescription(gitHubService);

        assertNull(commit.getPrId());
        assertNull(commit.getPrDescription());
        assertNull(commit.getMessage());
        verifyNoInteractions(gitHubService);
    }

    @Test
    void testEnrichWithPRDescription_emptyPRDescription() {
        when(gitHubService.extractPRId("Fix authentication bug(456)")).thenReturn(456);
        when(gitHubService.fetchPRDescription(456)).thenReturn("");

        String originalMessage = commit.getMessage();
        commit.enrichWithPRDescription(gitHubService);

        assertNull(commit.getPrId());
        assertNull(commit.getPrDescription());
        assertEquals(originalMessage, commit.getMessage());
    }

    @Test
    void testEnrichWithPRDescription_nullPRDescription() {
        when(gitHubService.extractPRId("Fix authentication bug(456)")).thenReturn(456);
        when(gitHubService.fetchPRDescription(456)).thenReturn(null);

        String originalMessage = commit.getMessage();
        commit.enrichWithPRDescription(gitHubService);

        assertNull(commit.getPrId());
        assertNull(commit.getPrDescription());
        assertEquals(originalMessage, commit.getMessage());
    }

    @Test
    void testEnrichWithPRDescription_whitespaceOnlyDescription() {
        when(gitHubService.extractPRId("Fix authentication bug(456)")).thenReturn(456);
        when(gitHubService.fetchPRDescription(456)).thenReturn("   \n\t   ");

        String originalMessage = commit.getMessage();
        commit.enrichWithPRDescription(gitHubService);

        assertNull(commit.getPrId());
        assertNull(commit.getPrDescription());
        assertEquals(originalMessage, commit.getMessage());
    }

    @Test
    void testGettersAndSetters() {
        commit.setPrId(789);
        commit.setPrDescription("Test description");
        commit.setSummary("Test summary");

        assertEquals(Integer.valueOf(789), commit.getPrId());
        assertEquals("Test description", commit.getPrDescription());
        assertEquals("Test summary", commit.getSummary());
    }

    @Test
    void testEqualsWithDifferentPRFields() {
        Commit c1 = createCommitWithAllFields();
        Commit c2 = createCommitWithAllFields();

        assertEquals(c1, c2);

        c2.setPrId(999);
        assertNotEquals(c1, c2);

        c2.setPrId(c1.getPrId());
        c2.setPrDescription("Different description");
        assertNotEquals(c1, c2);
    }

    private Commit createCommitWithAllFields() {
        Commit c = new Commit();
        c.setSha("abc");
        c.setMessage("msg");
        c.setAuthorName("name");
        c.setAuthorEmail("email");
        c.setAuthorLogin("login");
        c.setDate("2025-09-09");
        c.setSummary("summary");
        c.setPrDescription("pr desc");
        c.setPrId(123);
        return c;
    }
}
