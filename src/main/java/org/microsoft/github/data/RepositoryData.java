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
    private List<Contributor> contributors;

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
    public List<Contributor> getContributors() { return contributors; }
    public void setContributors(List<Contributor> contributors) { this.contributors = contributors; }

    @Override
    public String toString() {
        return "RepositoryData{" +
                "repoName='" + repoName + '\'' +
                ", issues=" + issues +
                ", commits=" + commits +
                ", pullRequests=" + pullRequests +
                ", contributors=" + contributors +
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
                Objects.equals(contributors, that.contributors);
    }

    @Override
    public int hashCode() {
        return Objects.hash(repoName, issues, commits, pullRequests, contributors);
    }
}
