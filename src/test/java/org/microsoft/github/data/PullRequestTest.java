package org.microsoft.github.data;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class PullRequestTest {
    @Test
    void testEqualsAndHashCode() {
        PullRequest p1 = new PullRequest();
        p1.setId(1L);
        p1.setNumber(10);
        p1.setTitle("PR");
        p1.setState("open");
        p1.setCreatedAt("2025-09-01");
        p1.setUpdatedAt("2025-09-02");
        p1.setClosedAt(null);
        p1.setMergedAt(null);
        p1.setAuthorLogin("user1");
        p1.setAssignees(List.of("user2"));
        p1.setLabels(List.of("enhancement"));
        p1.setComments(2);
        p1.setBody("Body");
        p1.setCommits(3);
        p1.setAdditions(10);
        p1.setDeletions(5);
        p1.setChangedFiles(2);

        PullRequest p2 = new PullRequest();
        p2.setId(1L);
        p2.setNumber(10);
        p2.setTitle("PR");
        p2.setState("open");
        p2.setCreatedAt("2025-09-01");
        p2.setUpdatedAt("2025-09-02");
        p2.setClosedAt(null);
        p2.setMergedAt(null);
        p2.setAuthorLogin("user1");
        p2.setAssignees(List.of("user2"));
        p2.setLabels(List.of("enhancement"));
        p2.setComments(2);
        p2.setBody("Body");
        p2.setCommits(3);
        p2.setAdditions(10);
        p2.setDeletions(5);
        p2.setChangedFiles(2);

        assertEquals(p1, p2);
        assertEquals(p1.hashCode(), p2.hashCode());
    }

    @Test
    void testToString() {
        PullRequest pr = new PullRequest();
        pr.setId(1L);
        pr.setTitle("PR");
        String str = pr.toString();
        assertTrue(str.contains("PR"));
        assertTrue(str.contains("id=1"));
    }
}

