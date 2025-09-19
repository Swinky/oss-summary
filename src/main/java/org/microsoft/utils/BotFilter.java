package org.microsoft.utils;

import org.microsoft.github.data.Commit;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.Issue;

/**
 * Utility class for filtering out bot activity from GitHub data.
 * This helps reduce API costs and processing time by filtering bots early in the pipeline.
 */
public class BotFilter {

    /**
     * Checks if a commit is from a bot account.
     * @param commit The commit to check
     * @return true if the commit is from a bot, false otherwise
     */
    public static boolean isBotCommit(Commit commit) {
        String authorLogin = commit.getAuthorLogin();
        if (authorLogin == null) return false;
        return isBotUser(authorLogin);
    }

    /**
     * Checks if a pull request is from a bot account.
     * @param pr The pull request to check
     * @return true if the PR is from a bot, false otherwise
     */
    public static boolean isBotPR(PullRequest pr) {
        String authorLogin = pr.getAuthorLogin();
        if (authorLogin == null) return false;
        return isBotUser(authorLogin);
    }

    /**
     * Checks if an issue is from a bot account.
     * @param issue The issue to check
     * @return true if the issue is from a bot, false otherwise
     */
    public static boolean isBotIssue(Issue issue) {
        String authorLogin = issue.getAuthorLogin();
        if (authorLogin == null) return false;
        return isBotUser(authorLogin);
    }

    /**
     * Centralized logic for determining if a username belongs to a bot.
     * @param username The GitHub username to check
     * @return true if the username appears to be a bot, false otherwise
     */
    private static boolean isBotUser(String username) {
        String lowerLogin = username.toLowerCase();

        // Filter out known bot patterns
        return lowerLogin.endsWith("bot") ||
               lowerLogin.contains("perfbot") ||
               lowerLogin.equals("testbot") ||
               (lowerLogin.contains("gluten") && lowerLogin.contains("bot")) ||
               lowerLogin.contains("dependabot") ||
               lowerLogin.contains("renovate") ||
               lowerLogin.contains("github-actions") ||
               lowerLogin.equals("codecov-io") ||
               lowerLogin.equals("coveralls");
    }
}
