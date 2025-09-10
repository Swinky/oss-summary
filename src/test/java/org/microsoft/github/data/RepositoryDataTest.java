package org.microsoft.github.data;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class RepositoryDataTest {
    @Test
    void testEqualsAndHashCode() {
        RepositoryData r1 = new RepositoryData();
        r1.setRepoName("owner/repo");
        r1.setIssues(List.of(new Issue()));
        r1.setCommits(List.of(new Commit()));
        r1.setPullRequests(List.of(new PullRequest()));
        r1.setContributors(List.of(new Contributor()));

        RepositoryData r2 = new RepositoryData();
        r2.setRepoName("owner/repo");
        r2.setIssues(List.of(new Issue()));
        r2.setCommits(List.of(new Commit()));
        r2.setPullRequests(List.of(new PullRequest()));
        r2.setContributors(List.of(new Contributor()));

        assertEquals(r1, r2);
        assertEquals(r1.hashCode(), r2.hashCode());
    }

    @Test
    void testToString() {
        RepositoryData repoData = new RepositoryData();
        repoData.setRepoName("owner/repo");
        String str = repoData.toString();
        assertTrue(str.contains("owner/repo"));
    }
}

