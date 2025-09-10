package org.microsoft.github.data;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class IssueTest {
    @Test
    void testEqualsAndHashCode() {
        Issue i1 = new Issue();
        i1.setId(1L);
        i1.setNumber(100);
        i1.setTitle("Test Issue");
        i1.setState("open");
        i1.setCreatedAt("2025-09-01");
        i1.setUpdatedAt("2025-09-02");
        i1.setClosedAt(null);
        i1.setAuthorLogin("user1");
        i1.setAssignees(List.of("user2"));
        i1.setLabels(List.of("bug"));
        i1.setComments(5);
        i1.setBody("Body");
        i1.setPullRequest(false);

        Issue i2 = new Issue();
        i2.setId(1L);
        i2.setNumber(100);
        i2.setTitle("Test Issue");
        i2.setState("open");
        i2.setCreatedAt("2025-09-01");
        i2.setUpdatedAt("2025-09-02");
        i2.setClosedAt(null);
        i2.setAuthorLogin("user1");
        i2.setAssignees(List.of("user2"));
        i2.setLabels(List.of("bug"));
        i2.setComments(5);
        i2.setBody("Body");
        i2.setPullRequest(false);

        assertEquals(i1, i2);
        assertEquals(i1.hashCode(), i2.hashCode());
    }

    @Test
    void testToString() {
        Issue issue = new Issue();
        issue.setId(1L);
        issue.setTitle("Test");
        String str = issue.toString();
        assertTrue(str.contains("Test"));
        assertTrue(str.contains("id=1"));
    }
}

