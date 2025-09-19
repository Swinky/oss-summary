package org.microsoft.utils;

import org.junit.jupiter.api.Test;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.Issue;

import static org.junit.jupiter.api.Assertions.*;

class BotFilterTest {

    @Test
    void shouldIdentifyBotCommits() {
        // Test various bot patterns
        assertTrue(BotFilter.isBotCommit(createCommit("dependabot")), "Should identify dependabot as bot");
        assertTrue(BotFilter.isBotCommit(createCommit("renovate-bot")), "Should identify renovate-bot as bot");
        assertTrue(BotFilter.isBotCommit(createCommit("github-actions")), "Should identify github-actions as bot");
        assertTrue(BotFilter.isBotCommit(createCommit("GlutenPerfBot")), "Should identify GlutenPerfBot as bot");
        assertTrue(BotFilter.isBotCommit(createCommit("testbot")), "Should identify testbot as bot");
        assertTrue(BotFilter.isBotCommit(createCommit("codecov-io")), "Should identify codecov-io as bot");
        assertTrue(BotFilter.isBotCommit(createCommit("coveralls")), "Should identify coveralls as bot");
        assertTrue(BotFilter.isBotCommit(createCommit("mybot")), "Should identify accounts ending with 'bot' as bot");

        // Test case insensitivity
        assertTrue(BotFilter.isBotCommit(createCommit("DEPENDABOT")), "Should be case insensitive");
        assertTrue(BotFilter.isBotCommit(createCommit("DependaBot")), "Should be case insensitive");
    }

    @Test
    void shouldNotIdentifyHumanCommits() {
        // Test human usernames
        assertFalse(BotFilter.isBotCommit(createCommit("john-doe")), "Should not identify human users as bots");
        assertFalse(BotFilter.isBotCommit(createCommit("alice123")), "Should not identify human users as bots");
        assertFalse(BotFilter.isBotCommit(createCommit("mskapilks")), "Should not identify team members as bots");
        assertFalse(BotFilter.isBotCommit(createCommit("zhli1142015")), "Should not identify team members as bots");
        assertFalse(BotFilter.isBotCommit(createCommit("developer")), "Should not identify regular users as bots");

        // Test edge cases
        assertFalse(BotFilter.isBotCommit(createCommit("robotics-engineer")), "Should not identify users with 'bot' in middle");
        assertFalse(BotFilter.isBotCommit(createCommit("abbott")), "Should not identify users ending with 'bott' as bots");
    }

    @Test
    void shouldHandleNullAuthorLogin() {
        Commit commit = new Commit();
        commit.setAuthorLogin(null);
        assertFalse(BotFilter.isBotCommit(commit), "Should handle null author login gracefully");
    }

    @Test
    void shouldIdentifyBotPullRequests() {
        // Test bot PR identification
        assertTrue(BotFilter.isBotPR(createPR("dependabot")), "Should identify dependabot PRs as bot");
        assertTrue(BotFilter.isBotPR(createPR("renovate-bot")), "Should identify renovate PRs as bot");
        assertTrue(BotFilter.isBotPR(createPR("github-actions")), "Should identify github-actions PRs as bot");

        // Test human PRs
        assertFalse(BotFilter.isBotPR(createPR("human-developer")), "Should not identify human PRs as bot");
        assertFalse(BotFilter.isBotPR(createPR("alice")), "Should not identify human PRs as bot");
    }

    @Test
    void shouldIdentifyBotIssues() {
        // Test bot issue identification
        assertTrue(BotFilter.isBotIssue(createIssue("dependabot")), "Should identify dependabot issues as bot");
        assertTrue(BotFilter.isBotIssue(createIssue("github-actions")), "Should identify github-actions issues as bot");

        // Test human issues
        assertFalse(BotFilter.isBotIssue(createIssue("bug-reporter")), "Should not identify human issues as bot");
        assertFalse(BotFilter.isBotIssue(createIssue("user123")), "Should not identify human issues as bot");
    }

    @Test
    void shouldHandleNullAuthorLoginInPRs() {
        PullRequest pr = new PullRequest();
        pr.setAuthorLogin(null);
        assertFalse(BotFilter.isBotPR(pr), "Should handle null PR author login gracefully");
    }

    @Test
    void shouldHandleNullAuthorLoginInIssues() {
        Issue issue = new Issue();
        issue.setAuthorLogin(null);
        assertFalse(BotFilter.isBotIssue(issue), "Should handle null issue author login gracefully");
    }

    @Test
    void shouldIdentifyProjectSpecificBots() {
        // Test project-specific bot patterns
        assertTrue(BotFilter.isBotCommit(createCommit("glutenbot")), "Should identify gluten-related bots");
        assertTrue(BotFilter.isBotCommit(createCommit("gluten-perf-bot")), "Should identify gluten performance bots");
        assertTrue(BotFilter.isBotCommit(createCommit("GlutenTestBot")), "Should identify gluten test bots");
    }

    @Test
    void shouldHandleCommonBotNamingPatterns() {
        // Test common bot naming conventions
        assertTrue(BotFilter.isBotCommit(createCommit("auto-bot")), "Should identify auto-bot");
        assertTrue(BotFilter.isBotCommit(createCommit("ci-bot")), "Should identify ci-bot");
        assertTrue(BotFilter.isBotCommit(createCommit("release-bot")), "Should identify release-bot");
        assertTrue(BotFilter.isBotCommit(createCommit("update-bot")), "Should identify update-bot");
        assertTrue(BotFilter.isBotCommit(createCommit("security-bot")), "Should identify security-bot");
    }

    // Helper methods to create test objects
    private Commit createCommit(String authorLogin) {
        Commit commit = new Commit();
        commit.setAuthorLogin(authorLogin);
        commit.setMessage("Test commit message");
        commit.setSha("abc123");
        return commit;
    }

    private PullRequest createPR(String authorLogin) {
        PullRequest pr = new PullRequest();
        pr.setAuthorLogin(authorLogin);
        pr.setTitle("Test PR title");
        pr.setNumber(123);
        return pr;
    }

    private Issue createIssue(String authorLogin) {
        Issue issue = new Issue();
        issue.setAuthorLogin(authorLogin);
        issue.setTitle("Test issue title");
        issue.setNumber(456);
        return issue;
    }
}
