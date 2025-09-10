package org.microsoft.github.data;

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

    @Override
    public String toString() {
        return "Commit{" +
                "sha='" + sha + '\'' +
                ", message='" + message + '\'' +
                ", authorName='" + authorName + '\'' +
                ", authorEmail='" + authorEmail + '\'' +
                ", authorLogin='" + authorLogin + '\'' +
                ", date='" + date + '\'' +
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
                Objects.equals(date, commit.date);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sha, message, authorName, authorEmail, authorLogin, date);
    }
}
