package org.microsoft.github.data;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ContributorTest {
    @Test
    void testEqualsAndHashCode() {
        Contributor c1 = new Contributor();
        c1.setLogin("user1");
        c1.setId(1L);
        c1.setContributions(10);
        c1.setAvatarUrl("url1");
        c1.setUrl("apiurl1");

        Contributor c2 = new Contributor();
        c2.setLogin("user1");
        c2.setId(1L);
        c2.setContributions(10);
        c2.setAvatarUrl("url1");
        c2.setUrl("apiurl1");

        assertEquals(c1, c2);
        assertEquals(c1.hashCode(), c2.hashCode());
    }

    @Test
    void testToString() {
        Contributor contributor = new Contributor();
        contributor.setLogin("user1");
        String str = contributor.toString();
        assertTrue(str.contains("user1"));
    }
}

