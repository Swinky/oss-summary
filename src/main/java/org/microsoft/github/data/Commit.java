package org.microsoft.github.data;

import org.microsoft.github.service.GitHubService;
import java.util.Objects;

/**
 * Represents a GitHub commit.
 */
public class Commit {
    /** Commit SHA */
    private String sha;
    /** Commit message */
    private String message;
    /** Author name */
    private String authorName;
    /** Author email */
    private String authorEmail;
    /** Author login (if available) */
    private String authorLogin;
    /** Commit date (ISO 8601) */
    private String date;
    /** AI-generated 2-line summary of the commit */
    private String summary;
    /** PR description fetched from GitHub API */
    private String prDescription;
    /** PR ID extracted from commit message */
    private Integer prId;

    public Commit() {}

    // Getters and setters
    public String getSha() { return sha; }
    public void setSha(String sha) { this.sha = sha; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getAuthorEmail() { return authorEmail; }
    public void setAuthorEmail(String authorEmail) { this.authorEmail = authorEmail; }
    public String getAuthorLogin() { return authorLogin; }
    public void setAuthorLogin(String authorLogin) { this.authorLogin = authorLogin; }
    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    /**
     * Gets the AI-generated summary for this commit.
     * @return 2-line summary explaining what changed and why it matters
     */
    public String getSummary() { return summary; }

    /**
     * Sets the AI-generated summary for this commit.
     * @param summary 2-line summary explaining what changed and why it matters
     */
    public void setSummary(String summary) { this.summary = summary; }

    public String getPrDescription() { return prDescription; }
    public void setPrDescription(String prDescription) { this.prDescription = prDescription; }
    public Integer getPrId() { return prId; }
    public void setPrId(Integer prId) { this.prId = prId; }

    /**
     * Fetches PR description and appends it to commit message.
     * Expects commit message format: "title(pull request id)"
     * @param gitHubService GitHub API service for fetching PR data
     */
    public void enrichWithPRDescription(GitHubService gitHubService) {
        if (message == null) return;

        Integer prNumber = gitHubService.extractPRId(message);
        if (prNumber != null) {
            String description = gitHubService.fetchPRDescription(prNumber);
            if (description != null && !description.trim().isEmpty()) {
                this.prId = prNumber;
                this.prDescription = description;
                this.message = message + "\n\nPR Description:\n" + description;
            }
        }
    }

    @Override
    public String toString() {
        return "Commit{" +
                "sha='" + sha + '\'' +
                ", message='" + message + '\'' +
                ", authorName='" + authorName + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", authorLogin='" + authorLogin + '\'' +
                ", date='" + date + '\'' +
                ", summary='" + summary + '\'' +
                ", prDescription='" + prDescription + '\'' +
                ", prId=" + prId +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Commit commit = (Commit) o;
        return Objects.equals(sha, commit.sha) &&
                Objects.equals(message, commit.message) &&
                Objects.equals(authorName, commit.authorName) &&
                Objects.equals(authorEmail, commit.authorEmail) &&
                Objects.equals(authorLogin, commit.authorLogin) &&
                Objects.equals(date, commit.date) &&
                Objects.equals(summary, commit.summary) &&
                Objects.equals(prDescription, commit.prDescription) &&
                Objects.equals(prId, commit.prId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sha, message, authorName, authorEmail, authorLogin, date, summary, prDescription, prId);
    }
}
