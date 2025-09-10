package org.microsoft.github.data;

import java.util.List;
import java.util.Objects;

/**
 * Represents a GitHub pull request.
 */
public class PullRequest {
    /** Unique pull request ID */
    private long id;
    /** Pull request number */
    private int number;
    /** Pull request title */
    private String title;
    /** Pull request state (open, closed, merged) */
    private String state;
    /** Creation timestamp (ISO 8601) */
    private String createdAt;
    /** Last update timestamp (ISO 8601) */
    private String updatedAt;
    /** Close timestamp (ISO 8601), if closed */
    private String closedAt;
    /** Merge timestamp (ISO 8601), if merged */
    private String mergedAt;
    /** Login of the pull request author */
    private String authorLogin;
    /** List of assignee logins */
    private List<String> assignees;
    /** List of label names */
    private List<String> labels;
    /** Number of comments */
    private int comments;
    /** Pull request body text */
    private String body;
    /** Number of commits in the pull request */
    private int commits;
    /** Number of additions */
    private int additions;
    /** Number of deletions */
    private int deletions;
    /** Number of changed files */
    private int changedFiles;

    public PullRequest() {}

    // Getters and setters
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getNumber() { return number; }
    public void setNumber(int number) { this.number = number; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getState() { return state; }
    public void setState(String state) { this.state = state; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public String getClosedAt() { return closedAt; }
    public void setClosedAt(String closedAt) { this.closedAt = closedAt; }
    public String getMergedAt() { return mergedAt; }
    public void setMergedAt(String mergedAt) { this.mergedAt = mergedAt; }
    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }
    public List<String> getAssignees() { return assignees; }
    public void setAssignees(List<String> assignees) { this.assignees = assignees; }
    public List<String> getLabels() {
        if (labels == null) {
            labels = new java.util.ArrayList<>();
        }
        return labels;
    }
    public void setLabels(List<String> labels) { this.labels = labels; }
    public int getComments() { return comments; }
    public void setComments(int comments) { this.comments = comments; }
    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }
    public int getCommits() { return commits; }
    public void setCommits(int commits) { this.commits = commits; }
    public int getAdditions() { return additions; }
    public void setAdditions(int additions) { this.additions = additions; }
    public int getDeletions() { return deletions; }
    public void setDeletions(int deletions) { this.deletions = deletions; }
    public int getChangedFiles() { return changedFiles; }
    public void setChangedFiles(int changedFiles) { this.changedFiles = changedFiles; }

    @Override
    public String toString() {
        return "PullRequest{" +
                "id=" + id +
                ", number=" + number +
                ", title='" + title + '\'' +
                ", state='" + state + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", closedAt='" + closedAt + '\'' +
                ", mergedAt='" + mergedAt + '\'' +
                ", authorLogin='" + authorLogin + '\'' +
                ", assignees=" + assignees +
                ", labels=" + labels +
                ", comments=" + comments +
                ", body='" + body + '\'' +
                ", commits=" + commits +
                ", additions=" + additions +
                ", deletions=" + deletions +
                ", changedFiles=" + changedFiles +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PullRequest that = (PullRequest) o;
        return id == that.id &&
                number == that.number &&
                comments == that.comments &&
                commits == that.commits &&
                additions == that.additions &&
                deletions == that.deletions &&
                changedFiles == that.changedFiles &&
                Objects.equals(title, that.title) &&
                Objects.equals(state, that.state) &&
                Objects.equals(createdAt, that.createdAt) &&
                Objects.equals(updatedAt, that.updatedAt) &&
                Objects.equals(closedAt, that.closedAt) &&
                Objects.equals(mergedAt, that.mergedAt) &&
                Objects.equals(authorLogin, that.authorLogin) &&
                Objects.equals(assignees, that.assignees) &&
                Objects.equals(labels, that.labels) &&
                Objects.equals(body, that.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number, title, state, createdAt, updatedAt, closedAt, mergedAt, authorLogin, assignees, labels, comments, body, commits, additions, deletions, changedFiles);
    }
}
