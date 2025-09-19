package org.microsoft.report;

import org.microsoft.analysis.CommitCategorization;
import org.microsoft.github.data.Commit;
import org.microsoft.github.data.Issue;
import org.microsoft.github.data.PullRequest;
import org.microsoft.github.data.RepositoryData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Arrays;

/**
 * Generates HTML reports locally with consistent structure.
 */
public class HtmlReportGenerator {
    private static final Logger logger = LoggerFactory.getLogger(HtmlReportGenerator.class);

    public String generateReport(RepositoryData data, CommitCategorization categorization,
                                String summary, String startDate, String endDate) {
        StringBuilder html = new StringBuilder();

        // Add HTML document structure with CSS
        html.append("<!DOCTYPE html>\n<html lang=\"en\">\n<head>\n");
        html.append("<meta charset=\"UTF-8\">\n");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
        html.append("<title>OSS Summary Report - ").append(data.getRepoName()).append("</title>\n");
        html.append("<style>\n");
        html.append(getEmbeddedCSS());
        html.append("</style>\n");
        html.append("</head>\n<body>\n");

        // Filter out bot activity
        RepositoryData filteredData = filterBotActivity(data);

        // Filter the categorization to match the filtered data
        CommitCategorization filteredCategorization = filterCategorization(categorization, filteredData);

        appendHeader(html, filteredData.getRepoName(), startDate, endDate);
        appendSummary(html, summary, filteredData, filteredCategorization, startDate, endDate);
        appendTeamActivity(html, filteredData);
        appendCommitsByCategory(html, filteredCategorization, filteredData.getRepoName());
        appendOpenPRs(html, filteredData.getPullRequests(), filteredData.getRepoName());
        appendIssues(html, filteredData.getIssues(), filteredData.getRepoName(), startDate, endDate);

        html.append("</body>\n</html>");
        return html.toString();
    }

    /**
     * Returns embedded CSS for the HTML report.
     * This includes styling for commit summaries and responsive design.
     */
    private String getEmbeddedCSS() {
        return """
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; line-height: 1.6; color: #333; max-width: 1200px; margin: 0 auto; padding: 20px; background-color: #f9f9f9; }
            h1 { color: #0366d6; border-bottom: 3px solid #0366d6; padding-bottom: 10px; margin-bottom: 30px; }
            h2 { color: #24292e; border-bottom: 1px solid #e1e4e8; padding-bottom: 8px; margin-top: 30px; margin-bottom: 15px; }
            h3 { color: #586069; margin-top: 20px; margin-bottom: 8px; }
            /* Plain commit list (ordered) */
            .commit-list { margin: 0 0 12px 1.5em; padding: 0; }
            .commit-list.empty { margin-left: 0; }
            .commit-list.empty > li { list-style: none; padding: 2px 0; color: #6a737d; font-style: italic; }
            a { color: #0366d6; text-decoration: none; }
            a:hover { text-decoration: underline; }
            em { color: #6a737d; font-style: italic; }
            """;
    }

    private void appendHeader(StringBuilder html, String repoName, String startDate, String endDate) {
        html.append(String.format("<h1>%s OSS Updates (%s to %s)</h1>\n",
                                repoName, startDate, endDate));
    }

    private void appendSummary(StringBuilder html, String summary, RepositoryData data,
                              CommitCategorization categorization, String startDate, String endDate) {
        html.append("<h2>Overall Summary</h2>\n");
        // Don't double-escape if summary is already escaped - check for existing HTML entities
        String summaryContent = summary;
        if (summary != null && !summary.contains("&lt;") && !summary.contains("&gt;") && !summary.contains("&amp;")) {
            summaryContent = escapeHtml(summary);
        }
        html.append(String.format("<p>%s</p>\n", summaryContent));

        html.append("<div style=\"flex: 1;\">\n<ul>\n");
        html.append(String.format("<li><b>Total commits:</b> %d</li>\n", categorization.getTotalCount()));
        html.append(String.format("<li><b>Number of PRs created/updated:</b> %d</li>\n",
                                data.getPullRequests() != null ? data.getPullRequests().size() : 0));
        html.append(String.format("<li><b>Number of issues reported:</b> %d</li>\n",
                                data.getIssues() != null ? data.getIssues().size() : 0));
        html.append(String.format("<li><b>Count per commit type:</b> Bug Fixes: %d, New Features: %d, Improvements: %d, Others: %d</li>\n",
                                categorization.getBugFixes().size(),
                                categorization.getFeatures().size(),
                                categorization.getImprovements().size(),
                                categorization.getOthers().size()));
        html.append(String.format("<li><b>Total commits by Microsoft Team:</b> %d</li>\n",
                                data.getTeamCommits() != null ? data.getTeamCommits().size() : 0));
        html.append("</ul>\n</div>\n<hr>\n");
    }

    private void appendTeamActivity(StringBuilder html, RepositoryData data) {
        html.append("<h2>Microsoft Team Activity</h2>\n");

        html.append("<h3>Pull Requests</h3>\n<ul>\n");
        if (data.getTeamPRs() != null && !data.getTeamPRs().isEmpty()) {
            data.getTeamPRs().forEach(pr -> {
                String url = String.format("https://github.com/%s/pull/%d", data.getRepoName(), pr.getNumber());
                // Replaced Unicode en dash with ASCII hyphen for broader encoding compatibility
                html.append(String.format("<li><a href=\"%s\">[#%d] %s</a> - by %s</li>\n",
                                        url, pr.getNumber(),
                                        escapeHtml(pr.getTitle()),
                                        escapeHtml(pr.getAuthorLogin())));
            });
        } else {
            html.append("<li><em>No Microsoft team activity found for pull requests in this period.</em></li>\n");
        }
        html.append("</ul>\n");

        html.append("<h3>Commits</h3>\n<ul>\n");
        if (data.getTeamCommits() != null && !data.getTeamCommits().isEmpty()) {
            data.getTeamCommits().forEach(commit -> {
                String url = String.format("https://github.com/%s/commit/%s", data.getRepoName(), commit.getSha());
                html.append(String.format("<li><a href=\"%s\">%s</a> - by %s</li>\n",
                                        url,
                                        escapeHtml(truncate(commit.getMessage(), 80)),
                                        escapeHtml(commit.getAuthorLogin())));
            });
        } else {
            html.append("<li><em>No Microsoft team activity found for commits in this period.</em></li>\n");
        }
        html.append("</ul>\n<hr>\n");
    }

    private void appendIssues(StringBuilder html, List<Issue> issues, String repoName, String startDate, String endDate) {
        html.append("<h2>New Issues Reported that are open</h2>\n<ol>\n");

        logger.debug("HtmlReportGenerator.appendIssues - Input issues: {}", (issues != null ? issues.size() : 0));

        if (issues != null && !issues.isEmpty()) {
            // Filter for open issues only and sort by creation date (newest first)
            List<Issue> openIssues = issues.stream()
                .filter(issue -> "open".equalsIgnoreCase(issue.getState())) // Only open issues
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt())) // newest first
                .collect(Collectors.toList());

            logger.debug("Displaying {} open issues within date range (filtered from {} total issues)",
                    openIssues.size(), issues.size());

            if (!openIssues.isEmpty()) {
                openIssues.forEach(issue -> {
                    String url = String.format("https://github.com/%s/issues/%d", repoName, issue.getNumber());
                    String createdDate = formatDate(issue.getCreatedAt());
                    html.append(String.format("<li><a href=\"%s\">#%d: %s</a> - created %s by %s</li>\n",
                                            url,
                                            issue.getNumber(),
                                            escapeHtml(issue.getTitle()),
                                            createdDate,
                                            escapeHtml(issue.getAuthorLogin() != null ? issue.getAuthorLogin() : "unknown")));
                });
            } else {
                html.append("<li><em>No open issues that were reported in this period.</em></li>\n");
            }
        } else {
            html.append("<li><em>No open issues that were reported in this period.</em></li>\n");
        }
        html.append("</ol>\n<hr>\n");
    }

    private void appendOpenPRs(StringBuilder html, List<PullRequest> pullRequests, String repoName) {
        html.append("<h2>Open Pull Requests</h2>\n (That were created in this reporting period)\n");

        if (pullRequests != null && !pullRequests.isEmpty()) {
            List<PullRequest> openPRs = pullRequests.stream()
                .filter(pr -> "open".equalsIgnoreCase(pr.getState())) // Filter for open PRs
                .collect(Collectors.toList());

            if (!openPRs.isEmpty()) {
                html.append("<ol>\n");
                openPRs.forEach(pr -> {
                    String url = String.format("https://github.com/%s/pull/%d", repoName, pr.getNumber());
                    html.append(String.format("<li><a href=\"%s\">#%d: %s</a> - by %s</li>\n",
                                            url, pr.getNumber(),
                                            escapeHtml(pr.getTitle()),
                                            escapeHtml(pr.getAuthorLogin())));
                });
                html.append("</ol>\n");
            } else {
                html.append("<p><em>No open pull requests found.</em></p>\n");
            }
        } else {
            html.append("<p><em>No pull requests found.</em></p>\n");
        }
        html.append("<hr>\n");
    }

    private void appendCommitsByCategory(StringBuilder html, CommitCategorization categorization, String repoName) {
        html.append("<h2>Commits by Category</h2>\n");

        appendCommitSection(html, "Important Bug Fixes", categorization.getBugFixes(), repoName);
        appendCommitSection(html, "Features", categorization.getFeatures(), repoName);
        appendCommitSection(html, "Improvements", categorization.getImprovements(), repoName);
        appendCommitSection(html, "Others", categorization.getOthers(), repoName);
    }

    private void appendCommitSection(StringBuilder html, String title, List<Commit> commits, String repoName) {
        boolean hasCommits = commits != null && !commits.isEmpty();
        html.append(String.format("<h3>%s</h3>\n", title));
        html.append(String.format("<ol class='commit-list%s'>\n", hasCommits ? "" : " empty"));
        if (hasCommits) {
            commits.forEach(commit -> {
                String url = String.format("https://github.com/%s/commit/%s", repoName, commit.getSha());
                html.append("<li>");
                html.append(String.format("<a href=\"%s\">%s</a> - by %s",
                        url,
                        escapeHtml(truncate(commit.getMessage(), 100)),
                        escapeHtml(commit.getAuthorLogin())));

                // Append AI summary as a single line (two sentences combined)
                if (commit.getSummary() != null && !commit.getSummary().trim().isEmpty()) {
                    String[] rawLines = commit.getSummary().split("\n");
                    List<String> nonEmpty = java.util.Arrays.stream(rawLines)
                        .map(String::trim)
                        .filter(l -> !l.isEmpty())
                        .toList();
                    String first = nonEmpty.size() > 0 ? nonEmpty.get(0) : null;
                    String second = nonEmpty.size() > 1 ? nonEmpty.get(1) : null;
                    if (first != null) {
                        StringBuilder oneLine = new StringBuilder();
                        oneLine.append(first);
                        if (second != null) {
                            if (!first.endsWith(".") && !first.endsWith("!") && !first.endsWith("?")) {
                                // ensure proper sentence separation
                                if (!first.endsWith(".")) oneLine.append(".");
                            }
                            oneLine.append(" ").append(second);
                        }
                        html.append("<br>").append(escapeHtml(oneLine.toString())) ;
                    }
                }

                // Append description italicized if present
                String description = getCommitDescription(commit);
                if (description != null && !description.trim().isEmpty()) {
                    html.append("<br><em>").append(escapeHtml(truncate(description, 150))).append("</em>");
                }

                html.append("</li>\n");
            });
        } else {
            html.append("<li><em>No commits in this category.</em></li>\n");
        }
        html.append("</ol>\n");
    }

    private RepositoryData filterBotActivity(RepositoryData data) {
        // Create a new RepositoryData with filtered lists
        RepositoryData filtered = new RepositoryData();
        filtered.setRepoName(data.getRepoName());

        // Filter commits - only filter out actual bots, keep human users
        if (data.getCommits() != null) {
            List<Commit> filteredCommits = data.getCommits().stream()
                .filter(commit -> !isBotCommit(commit))
                .collect(Collectors.toList());
            filtered.setCommits(filteredCommits);
        }

        // Filter PRs - only filter out actual bots, keep human users
        if (data.getPullRequests() != null) {
            List<PullRequest> filteredPRs = data.getPullRequests().stream()
                .filter(pr -> !isBotPR(pr))
                .collect(Collectors.toList());
            filtered.setPullRequests(filteredPRs);
        }

        // Keep issues as-is (usually not created by bots we want to filter)
        filtered.setIssues(data.getIssues());

        // Filter team data - only filter out actual bots, keep human users
        if (data.getTeamCommits() != null) {
            List<Commit> filteredTeamCommits = data.getTeamCommits().stream()
                .filter(commit -> !isBotCommit(commit))
                .collect(Collectors.toList());
            filtered.setTeamCommits(filteredTeamCommits);
        }

        if (data.getTeamPRs() != null) {
            List<PullRequest> filteredTeamPRs = data.getTeamPRs().stream()
                .filter(pr -> !isBotPR(pr))
                .collect(Collectors.toList());
            filtered.setTeamPRs(filteredTeamPRs);
        }

        filtered.setTeamIssues(data.getTeamIssues());

        return filtered;
    }

    private boolean isBotCommit(Commit commit) {
        String authorLogin = commit.getAuthorLogin();
        if (authorLogin == null) return false;
        String lowerLogin = authorLogin.toLowerCase();
        // Filter out known bot patterns: accounts ending with "bot" or containing "bot" in specific contexts
        return lowerLogin.endsWith("bot") ||
               lowerLogin.contains("perfbot") ||
               lowerLogin.equals("testbot") ||
               (lowerLogin.contains("gluten") && lowerLogin.contains("bot"));
    }

    private boolean isBotPR(PullRequest pr) {
        String authorLogin = pr.getAuthorLogin();
        if (authorLogin == null) return false;
        String lowerLogin = authorLogin.toLowerCase();
        // Filter out known bot patterns: accounts ending with "bot" or containing "bot" in specific contexts
        return lowerLogin.endsWith("bot") ||
               lowerLogin.contains("perfbot") ||
               lowerLogin.equals("testbot") ||
               (lowerLogin.contains("gluten") && lowerLogin.contains("bot"));
    }

    private int countBugIssues(List<Issue> issues) {
        if (issues == null) return 0;
        return (int) issues.stream()
            .filter(issue -> {
                String title = issue.getTitle();
                String body = issue.getBody();
                return (title != null && title.toLowerCase().contains("bug")) ||
                       (body != null && body.toLowerCase().contains("bug"));
            })
            .count();
    }

    private String getCommitDescription(Commit commit) {
        String message = commit.getMessage();
        if (message == null) return "";

        // Extract description (everything after first line)
        String[] lines = message.split("\n", 2);
        return lines.length > 1 ? lines[1].trim() : "";
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return "";
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#x27;");
    }

    private CommitCategorization filterCategorization(CommitCategorization categorization, RepositoryData filteredData) {
        // Filter each category based on the filtered commits
        List<Commit> filteredBugFixes = filterCommitsBySha(categorization.getBugFixes(), filteredData.getCommits());
        List<Commit> filteredFeatures = filterCommitsBySha(categorization.getFeatures(), filteredData.getCommits());
        List<Commit> filteredImprovements = filterCommitsBySha(categorization.getImprovements(), filteredData.getCommits());
        List<Commit> filteredOthers = filterCommitsBySha(categorization.getOthers(), filteredData.getCommits());

        return new CommitCategorization(filteredBugFixes, filteredFeatures, filteredImprovements, filteredOthers);
    }

    private List<Commit> filterCommitsBySha(List<Commit> commits, List<Commit> allCommits) {
        if (commits == null || allCommits == null) return commits;
        // Get the SHAs of the commits that are not filtered out
        List<String> allCommitShas = allCommits.stream().map(Commit::getSha).toList();
        return commits.stream()
            .filter(commit -> allCommitShas.contains(commit.getSha()))
            .collect(Collectors.toList());
    }

    private boolean isWithinDateRange(String dateStr, String startDateStr, String endDateStr) {
        if (dateStr == null) return false;
        if (startDateStr == null && endDateStr == null) return true;

        // Extract just the date part from ISO datetime (e.g., "2025-09-12T18:51:35Z" -> "2025-09-12")
        String dateOnly = dateStr.contains("T") ? dateStr.substring(0, dateStr.indexOf("T")) : dateStr;

        if (startDateStr == null) {
            // Only end date specified - include if date is <= end date
            return dateOnly.compareTo(endDateStr) <= 0;
        }
        if (endDateStr == null) {
            // Only start date specified - include if date is >= start date
            return dateOnly.compareTo(startDateStr) >= 0;
        }

        // Both dates specified - include if date is within range (inclusive)
        return dateOnly.compareTo(startDateStr) >= 0 && dateOnly.compareTo(endDateStr) <= 0;
    }

    private String formatDate(String dateStr) {
        // Simplified date formatting (assuming input is in ISO format "yyyy-MM-dd")
        if (dateStr == null) return "";
        return dateStr; // In a real scenario, convert to desired output format
    }
}
