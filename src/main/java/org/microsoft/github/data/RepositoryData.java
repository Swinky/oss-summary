package org.microsoft.github.data;

import java.util.List;
import java.util.Objects;

/**
 * Aggregates all fetched data for a GitHub repository.
 */
public class RepositoryData {
    /** Repository name (owner/repo) */
    private String repoName;
    /** List of issues */
    private List<Issue> issues;
    /** List of commits */
    private List<Commit> commits;
    /** List of pull requests */
    private List<PullRequest> pullRequests;
    /** List of contributors */
    //private List<Contributor> contributors;
    /** List of team member issues */
    private List<Issue> teamIssues;
    /** List of team member commits */
    private List<Commit> teamCommits;
    /** List of team member pull requests */
    private List<PullRequest> teamPRs;

    public RepositoryData() {}

    // Getters and setters
    public String getRepoName() { return repoName; }
    public void setRepoName(String repoName) { this.repoName = repoName; }
    public List<Issue> getIssues() { return issues; }
    public void setIssues(List<Issue> issues) { this.issues = issues; }
    public List<Commit> getCommits() { return commits; }
    public void setCommits(List<Commit> commits) { this.commits = commits; }
    public List<PullRequest> getPullRequests() { return pullRequests; }
    public void setPullRequests(List<PullRequest> pullRequests) { this.pullRequests = pullRequests; }
    //public List<Contributor> getContributors() { return contributors; }
    //public void setContributors(List<Contributor> contributors) { this.contributors = contributors; }
    public List<Issue> getTeamIssues() { return teamIssues; }
    public void setTeamIssues(List<Issue> teamIssues) { this.teamIssues = teamIssues; }
    public List<Commit> getTeamCommits() { return teamCommits; }
    public void setTeamCommits(List<Commit> teamCommits) { this.teamCommits = teamCommits; }
    public List<PullRequest> getTeamPRs() { return teamPRs; }
    public void setTeamPRs(List<PullRequest> teamPRs) { this.teamPRs = teamPRs; }

    /**
     * Removes any commit or pull request where the author login contains "bot" (case-insensitive).
     * This method filters out bot-generated activity from all lists including team lists.
     */
    public void removeBotActivity() {
        if (commits != null) {
            commits.removeIf(commit -> commit.getAuthorLogin() != null &&
                            commit.getAuthorLogin().toLowerCase().contains("bot"));
        }
        if (pullRequests != null) {
            pullRequests.removeIf(pr -> pr.getAuthorLogin() != null &&
                                 pr.getAuthorLogin().toLowerCase().contains("bot"));
        }
        if (teamCommits != null) {
            teamCommits.removeIf(commit -> commit.getAuthorLogin() != null &&
                               commit.getAuthorLogin().toLowerCase().contains("bot"));
        }
        if (teamPRs != null) {
            teamPRs.removeIf(pr -> pr.getAuthorLogin() != null &&
                            pr.getAuthorLogin().toLowerCase().contains("bot"));
        }
    }

    @Override
    public String toString() {
        return "RepositoryData{" +
                "repoName='" + repoName + '\'' +
                ", issues=" + issues +
                ", commits=" + commits +
                ", pullRequests=" + pullRequests +
                ", teamIssues=" + teamIssues +
                ", teamCommits=" + teamCommits +
                ", teamPRs=" + teamPRs +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RepositoryData that = (RepositoryData) o;
        return Objects.equals(repoName, that.repoName) &&
                Objects.equals(issues, that.issues) &&
                Objects.equals(commits, that.commits) &&
                Objects.equals(pullRequests, that.pullRequests) &&
                Objects.equals(teamIssues, that.teamIssues) &&
                Objects.equals(teamCommits, that.teamCommits) &&
                Objects.equals(teamPRs, that.teamPRs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repoName, issues, commits, pullRequests, teamIssues, teamCommits, teamPRs);
    }
}
