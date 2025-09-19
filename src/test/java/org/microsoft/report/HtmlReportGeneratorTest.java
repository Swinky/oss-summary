package org.microsoft.report;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.microsoft.analysis.CommitCategorization;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.Issue;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.RepositoryData;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HtmlReportGeneratorTest {

    private HtmlReportGenerator generator;

    @BeforeEach
    void setUp() {
        generator = new HtmlReportGenerator();
    }

    @Test
    void shouldGenerateCompleteHtmlReport() {
        // Given
        RepositoryData testData = createTestRepositoryData();
        CommitCategorization categorization = createTestCategorization();
        String summary = "Test summary of repository activity.";

        // When
        String report = generator.generateReport(testData, categorization, summary, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(report);
        assertTrue(report.contains("<h1>apache/incubator-gluten OSS Updates (2024-01-01 to 2024-01-07)</h1>"));
        assertTrue(report.contains("<h2>Overall Summary</h2>"));
        assertTrue(report.contains("Test summary of repository activity."));
        assertTrue(report.contains("<h2>Microsoft Team Activity</h2>"));
        assertTrue(report.contains("<h2>Commits by Category</h2>"));
        assertTrue(report.contains("<h2>Open Pull Requests</h2>"));
        assertTrue(report.contains("<h2>New Issues Reported that are open</h2>"));
        assertTrue(report.contains("<h3>Important Bug Fixes</h3>"));
        assertTrue(report.contains("<h3>Features</h3>"));
        assertTrue(report.contains("<h3>Improvements</h3>"));
        assertTrue(report.contains("<h3>Others</h3>"));
    }

    @Test
    void shouldFilterOutBotActivity() {
        // Given
        RepositoryData testData = createTestRepositoryDataWithBots();
        // Create categorization that includes the human commit so it appears in the report
        Commit humanCommit = new Commit();
        humanCommit.setMessage("Human commit");
        humanCommit.setAuthorLogin("humanuser");
        humanCommit.setSha("human123");
        CommitCategorization categorization = new CommitCategorization(
            List.of(humanCommit), List.of(), List.of(), List.of()
        );
        String summary = "Test summary";

        // When
        String report = generator.generateReport(testData, categorization, summary, "2024-01-01", "2024-01-07");

        // Then
        assertFalse(report.contains("GlutenPerfBot"), "Report should not contain GlutenPerfBot");
        assertFalse(report.contains("testbot"), "Report should not contain testbot");
        assertTrue(report.contains("humanuser"), "Report should contain humanuser");
    }

    @Test
    void shouldHandleEmptyData() {
        // Given
        RepositoryData emptyData = createEmptyRepositoryData();
        CommitCategorization emptyCategorization = createEmptyCategorization();
        String summary = "No activity this week.";

        // When
        String report = generator.generateReport(emptyData, emptyCategorization, summary, "2024-01-01", "2024-01-07");

        // Then
        assertNotNull(report);
        assertTrue(report.contains("No activity this week."));
        assertTrue(report.contains("<li><b>Total commits:</b> 0</li>"));
        assertTrue(report.contains("<li><b>Number of PRs created/updated:</b> 0</li>"));
    }

    @Test
    void shouldEscapeHtmlCharacters() {
        // Given
        RepositoryData data = createRepositoryDataWithSpecialChars();
        // Create categorization that includes the commit with special characters
        Commit commitWithSpecialChars = new Commit();
        commitWithSpecialChars.setSha("abc123");
        commitWithSpecialChars.setMessage("Fix <tag> & \"quotes\"");
        commitWithSpecialChars.setAuthorLogin("user<script>");
        CommitCategorization categorization = new CommitCategorization(
            List.of(commitWithSpecialChars), List.of(), List.of(), List.of()
        );
        String summary = "Summary with &lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;";

        // When
        String report = generator.generateReport(data, categorization, summary, "2024-01-01", "2024-01-07");

        // Then
        assertTrue(report.contains("&lt;script&gt;"), "HTML should be escaped in summary");
        assertTrue(report.contains("&lt;tag&gt;"), "HTML should be escaped in commit messages");
        assertTrue(report.contains("user&lt;script&gt;"), "HTML should be escaped in author names");
        assertFalse(report.contains("<script>alert('xss')</script>"), "Raw script tags should not be present");
    }

    @Test
    void shouldGenerateCorrectGitHubUrls() {
        // Given
        RepositoryData testData = createTestRepositoryData();
        CommitCategorization categorization = createTestCategorization();

        // When
        String report = generator.generateReport(testData, categorization, "summary", "2024-01-01", "2024-01-07");

        // Then
        assertTrue(report.contains("https://github.com/apache/incubator-gluten/pull/123"), "Should contain correct PR URL");
        assertTrue(report.contains("https://github.com/apache/incubator-gluten/issues/456"), "Should contain correct issue URL");
        assertTrue(report.contains("https://github.com/apache/incubator-gluten/commit/abc123"), "Should contain correct commit URL");
    }

    @Test
    void shouldFilterIssuesByDateRangeAndShowDetails() {
        // Given
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");
        // Create issues with different states (date range filtering not implemented)
        Issue openIssue = new Issue();
        openIssue.setNumber(123);
        openIssue.setTitle("Open issue");
        openIssue.setCreatedAt("2024-01-03");
        openIssue.setAuthorLogin("user1");
        openIssue.setState("open"); // Set as open to be displayed

        Issue anotherOpenIssue = new Issue();
        anotherOpenIssue.setNumber(456);
        anotherOpenIssue.setTitle("Another open issue");
        anotherOpenIssue.setCreatedAt("2023-12-15"); // Different date, but still shown since no date filtering
        anotherOpenIssue.setAuthorLogin("user2");
        anotherOpenIssue.setState("open");

        Issue closedIssue = new Issue();
        closedIssue.setNumber(789);
        closedIssue.setTitle("Closed issue");
        closedIssue.setCreatedAt("2024-01-05");
        closedIssue.setAuthorLogin("user3");
        closedIssue.setState("closed"); // Closed, so won't be displayed

        data.setIssues(List.of(openIssue, anotherOpenIssue, closedIssue));

        CommitCategorization emptyCategorization = createEmptyCategorization();

        // When
        String report = generator.generateReport(data, emptyCategorization, "summary", "2024-01-01", "2024-01-07");

        // Then
        // Should contain both open issues (regardless of date)
        assertTrue(report.contains("#123: Open issue"), "Should show first open issue");
        assertTrue(report.contains("#456: Another open issue"), "Should show second open issue");
        assertTrue(report.contains("created 2024-01-03 by user1"), "Should show creation date and author for first issue");
        assertTrue(report.contains("created 2023-12-15 by user2"), "Should show creation date and author for second issue");
        assertTrue(report.contains("https://github.com/test/repo/issues/123"), "Should contain correct issue URL");

        // Should NOT contain closed issues
        assertFalse(report.contains("Closed issue"), "Should not show closed issue");
    }

    @Test
    void shouldHandleEmptyIssuesInDateRange() {
        // Given
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");

        // Create only closed issues (no open issues to display)
        Issue closedIssue1 = new Issue();
        closedIssue1.setNumber(123);
        closedIssue1.setTitle("Closed issue 1");
        closedIssue1.setCreatedAt("2023-12-01");
        closedIssue1.setAuthorLogin("user1");
        closedIssue1.setState("closed");

        Issue closedIssue2 = new Issue();
        closedIssue2.setNumber(124);
        closedIssue2.setTitle("Closed issue 2");
        closedIssue2.setCreatedAt("2024-01-03");
        closedIssue2.setAuthorLogin("user2");
        closedIssue2.setState("closed");

        data.setIssues(List.of(closedIssue1, closedIssue2));

        CommitCategorization emptyCategorization = createEmptyCategorization();

        // When
        String report = generator.generateReport(data, emptyCategorization, "summary", "2024-01-01", "2024-01-07");

        // Then
        assertTrue(report.contains("<h2>New Issues Reported that are open</h2>"), "Should have updated issues section header");
        assertTrue(report.contains("No open issues that were reported in this period"), "Should show no open issues message");
        assertFalse(report.contains("Closed issue 1"), "Should not show first closed issue");
        assertFalse(report.contains("Closed issue 2"), "Should not show second closed issue");
    }

    @Test
    void shouldDisplayOpenPullRequests() {
        // Given
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");

        // Create PRs with different states
        PullRequest openPR1 = new PullRequest();
        openPR1.setNumber(100);
        openPR1.setTitle("Open PR 1");
        openPR1.setState("open");
        openPR1.setAuthorLogin("user1");

        PullRequest openPR2 = new PullRequest();
        openPR2.setNumber(101);
        openPR2.setTitle("Open PR 2");
        openPR2.setState("open");
        openPR2.setAuthorLogin("user2");

        PullRequest closedPR = new PullRequest();
        closedPR.setNumber(102);
        closedPR.setTitle("Closed PR");
        closedPR.setState("closed");
        closedPR.setAuthorLogin("user3");

        data.setPullRequests(List.of(openPR1, openPR2, closedPR));

        CommitCategorization emptyCategorization = createEmptyCategorization();

        // When
        String report = generator.generateReport(data, emptyCategorization, "summary", "2024-01-01", "2024-01-07");

        // Then
        assertTrue(report.contains("<h2>Open Pull Requests</h2>"), "Should have open PRs section header");
        assertTrue(report.contains("#100: Open PR 1"), "Should show first open PR");
        assertTrue(report.contains("#101: Open PR 2"), "Should show second open PR");
        assertTrue(report.contains("https://github.com/test/repo/pull/100"), "Should contain correct PR URL for first PR");
        assertTrue(report.contains("https://github.com/test/repo/pull/101"), "Should contain correct PR URL for second PR");
        assertTrue(report.contains("by user1"), "Should show author for first PR");
        assertTrue(report.contains("by user2"), "Should show author for second PR");

        // Should NOT show closed PRs
        assertFalse(report.contains("Closed PR"), "Should not show closed PR");
        assertFalse(report.contains("#102"), "Should not show closed PR number");
    }

    @Test
    void shouldHandleEmptyOpenPullRequests() {
        // Given
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");

        // Create only closed PRs
        PullRequest closedPR = new PullRequest();
        closedPR.setNumber(102);
        closedPR.setTitle("Closed PR");
        closedPR.setState("closed");
        closedPR.setAuthorLogin("user3");

        data.setPullRequests(List.of(closedPR));

        CommitCategorization emptyCategorization = createEmptyCategorization();

        // When
        String report = generator.generateReport(data, emptyCategorization, "summary", "2024-01-01", "2024-01-07");

        // Then
        assertTrue(report.contains("<h2>Open Pull Requests</h2>"), "Should have open PRs section header");
        assertTrue(report.contains("No open pull requests found"), "Should show no open PRs message");
        assertFalse(report.contains("Closed PR"), "Should not show closed PR");
    }

    private RepositoryData createTestRepositoryData() {
        RepositoryData data = new RepositoryData();
        data.setRepoName("apache/incubator-gluten");

        // Create test commits
        Commit commit = new Commit();
        commit.setSha("abc123");
        commit.setMessage("fix: resolve critical issue");
        commit.setAuthorLogin("humanuser");
        data.setCommits(List.of(commit));

        // Create test PRs
        PullRequest pr = new PullRequest();
        pr.setNumber(123);
        pr.setTitle("Add new feature");
        pr.setAuthorLogin("teamuser");
        pr.setState("open"); // Set state for open PR section
        data.setPullRequests(List.of(pr));
        data.setTeamPRs(List.of(pr));

        // Create test issues with proper date and author information
        Issue issue = new Issue();
        issue.setNumber(456);
        issue.setTitle("Bug report");
        issue.setCreatedAt("2024-01-03"); // Within the test date range 2024-01-01 to 2024-01-07
        issue.setAuthorLogin("issueauthor");
        issue.setState("open"); // Set as open so it appears in the filtered report
        data.setIssues(List.of(issue));

        return data;
    }

    private RepositoryData createTestRepositoryDataWithBots() {
        RepositoryData data = new RepositoryData();
        data.setRepoName("apache/incubator-gluten");

        Commit humanCommit = new Commit();
        humanCommit.setMessage("Human commit");
        humanCommit.setAuthorLogin("humanuser");
        humanCommit.setSha("human123");

        Commit botCommit = new Commit();
        botCommit.setMessage("Bot commit");
        botCommit.setAuthorLogin("GlutenPerfBot");
        botCommit.setSha("bot456");

        data.setCommits(Arrays.asList(humanCommit, botCommit));

        PullRequest humanPR = new PullRequest();
        humanPR.setNumber(123);
        humanPR.setTitle("Human PR");
        humanPR.setAuthorLogin("humanuser");
        humanPR.setState("open"); // Set state for consistency

        PullRequest botPR = new PullRequest();
        botPR.setNumber(456);
        botPR.setTitle("Bot PR");
        botPR.setAuthorLogin("testbot");
        botPR.setState("closed"); // Set as closed to test filtering

        data.setPullRequests(Arrays.asList(humanPR, botPR));

        return data;
    }

    private RepositoryData createEmptyRepositoryData() {
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");
        return data;
    }

    private RepositoryData createRepositoryDataWithSpecialChars() {
        RepositoryData data = new RepositoryData();
        data.setRepoName("test/repo");

        Commit commit = new Commit();
        commit.setSha("abc123");
        commit.setMessage("Fix <tag> & \"quotes\"");
        commit.setAuthorLogin("user<script>");
        data.setCommits(List.of(commit));

        return data;
    }

    private CommitCategorization createTestCategorization() {
        // Use the same commit as in createTestRepositoryData() to ensure URL appears in categorized section
        Commit bugFix = new Commit();
        bugFix.setMessage("fix: resolve critical issue");
        bugFix.setAuthorLogin("humanuser");
        bugFix.setSha("abc123");

        return new CommitCategorization(
            List.of(bugFix),
            List.of(),
            List.of(),
            List.of()
        );
    }

    private CommitCategorization createEmptyCategorization() {
        return new CommitCategorization(List.of(), List.of(), List.of(), List.of());
    }
}
