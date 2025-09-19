import org.microsoft.analysis.CommitCategorization;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.RepositoryData;
import org.microsoft.report.HtmlReportGenerator;
import java.util.Arrays;
import java.util.List;

public class debug_test {
    public static void main(String[] args) {
        HtmlReportGenerator generator = new HtmlReportGenerator();

        // Test 1: HTML Escaping
        System.out.println("=== Test 1: HTML Escaping ===");
        RepositoryData data1 = new RepositoryData();
        data1.setRepoName("test/repo");

        Commit commit1 = new Commit();
        commit1.setSha("abc123");
        commit1.setMessage("Fix <tag> & \"quotes\"");
        commit1.setAuthorLogin("user<script>");
        data1.setCommits(List.of(commit1));

        CommitCategorization categorization1 = new CommitCategorization(List.of(), List.of(), List.of(), List.of());
        String summary1 = "Summary with &lt;script&gt;alert(&#x27;xss&#x27;)&lt;/script&gt;";

        String report1 = generator.generateReport(data1, categorization1, summary1, "2024-01-01", "2024-01-07");

        boolean containsEscapedScript = report1.contains("&lt;script&gt;");
        boolean containsEscapedTag = report1.contains("&lt;tag&gt;");
        boolean containsEscapedUser = report1.contains("user&lt;script&gt;");
        boolean noRawScript = !report1.contains("<script>alert('xss')</script>");

        System.out.println("Contains escaped script: " + containsEscapedScript);
        System.out.println("Contains escaped tag: " + containsEscapedTag);
        System.out.println("Contains escaped user: " + containsEscapedUser);
        System.out.println("No raw script: " + noRawScript);
        System.out.println("HTML Escaping Test PASSED: " + (containsEscapedScript && containsEscapedTag && containsEscapedUser && noRawScript));

        // Test 2: Bot Filtering
        System.out.println("\n=== Test 2: Bot Filtering ===");
        RepositoryData data2 = new RepositoryData();
        data2.setRepoName("apache/incubator-gluten");

        Commit humanCommit = new Commit();
        humanCommit.setMessage("Human commit");
        humanCommit.setAuthorLogin("humanuser");
        humanCommit.setSha("human123");

        Commit botCommit = new Commit();
        botCommit.setMessage("Bot commit");
        botCommit.setAuthorLogin("GlutenPerfBot");
        botCommit.setSha("bot456");

        data2.setCommits(Arrays.asList(humanCommit, botCommit));

        PullRequest humanPR = new PullRequest();
        humanPR.setNumber(123);
        humanPR.setTitle("Human PR");
        humanPR.setAuthorLogin("humanuser");

        PullRequest botPR = new PullRequest();
        botPR.setNumber(456);
        botPR.setTitle("Bot PR");
        botPR.setAuthorLogin("testbot");

        data2.setPullRequests(Arrays.asList(humanPR, botPR));

        // Create categorization with human commit to make it appear in the report
        CommitCategorization categorization2 = new CommitCategorization(List.of(humanCommit), List.of(), List.of(), List.of());

        String report2 = generator.generateReport(data2, categorization2, "Test summary", "2024-01-01", "2024-01-07");

        boolean containsHuman = report2.contains("humanuser");
        boolean noGlutenPerfBot = !report2.contains("GlutenPerfBot");
        boolean noTestBot = !report2.contains("testbot");

        System.out.println("Contains humanuser: " + containsHuman);
        System.out.println("No GlutenPerfBot: " + noGlutenPerfBot);
        System.out.println("No testbot: " + noTestBot);
        System.out.println("Bot Filtering Test PASSED: " + (containsHuman && noGlutenPerfBot && noTestBot));

        if (containsHuman && noGlutenPerfBot && noTestBot && containsEscapedScript && containsEscapedTag && containsEscapedUser && noRawScript) {
            System.out.println("\n✅ ALL TESTS PASSED!");
        } else {
            System.out.println("\n❌ SOME TESTS FAILED!");
        }
    }
}
