package org.microsoft.github.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CommitTest {
    @Test
    void testEqualsAndHashCode() {
        Commit c1 = new Commit();
        c1.setSha("abc");
        c1.setMessage("msg");
        c1.setAuthorName("name");
        c1.setAuthorEmail("email");
        c1.setAuthorLogin("login");
        c1.setDate("2025-09-09");

        Commit c2 = new Commit();
        c2.setSha("abc");
        c2.setMessage("msg");
        c2.setAuthorName("name");
        c2.setAuthorEmail("email");
        c2.setAuthorLogin("login");
        c2.setDate("2025-09-09");

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testToString() {
        Commit commit = new Commit();
        commit.setSha("abc");
        String str = commit.toString();
        assertTrue(str.contains("abc"));
    }
}

