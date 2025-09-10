package org.microsoft.github.data;

import java.util.List;
import java.util.Objects;

/**
 * Represents a GitHub issue.
 */
public class Issue {
    /** Unique issue ID */
    private long id;
    /** Issue number in the repository */
    private int number;
    /** Issue title */
    private String title;
    /** Issue state (open, closed) */
    private String state;
    /** Creation timestamp (ISO 8601) */
    private String createdAt;
    /** Last update timestamp (ISO 8601) */
    private String updatedAt;
    /** Close timestamp (ISO 8601), if closed */
    private String closedAt;
    /** Login of the issue author */
    private String authorLogin;
    /** List of assignee logins */
    private List<String> assignees;
    /** List of label names */
    private List<String> labels;
    /** Number of comments */
    private int comments;
    /** Issue body text */
    private String body;
    /** True if this issue is a pull request */
    private boolean isPullRequest;

    public Issue() {}

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
    public boolean isPullRequest() { return isPullRequest; }
    public void setPullRequest(boolean pullRequest) { isPullRequest = pullRequest; }

    @Override
    public String toString() {
        return "Issue{" +
                "id=" + id +
                ", number=" + number +
                ", title='" + title + '\'' +
                ", state='" + state + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", updatedAt='" + updatedAt + '\'' +
                ", closedAt='" + closedAt + '\'' +
                ", authorLogin='" + authorLogin + '\'' +
                ", assignees=" + assignees +
                ", labels=" + labels +
                ", comments=" + comments +
                ", body='" + body + '\'' +
                ", isPullRequest=" + isPullRequest +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Issue issue = (Issue) o;
        return id == issue.id &&
                number == issue.number &&
                comments == issue.comments &&
                isPullRequest == issue.isPullRequest &&
                Objects.equals(title, issue.title) &&
                Objects.equals(state, issue.state) &&
                Objects.equals(createdAt, issue.createdAt) &&
                Objects.equals(updatedAt, issue.updatedAt) &&
                Objects.equals(closedAt, issue.closedAt) &&
                Objects.equals(authorLogin, issue.authorLogin) &&
                Objects.equals(assignees, issue.assignees) &&
                Objects.equals(labels, issue.labels) &&
                Objects.equals(body, issue.body);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, number, title, state, createdAt, updatedAt, closedAt, authorLogin, assignees, labels, comments, body, isPullRequest);
    }
}
