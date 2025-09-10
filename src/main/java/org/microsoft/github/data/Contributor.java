package org.microsoft.github.data;

import java.util.Objects;

/**
 * Represents a GitHub contributor.
 */
public class Contributor {
    /** Contributor login */
    private String login;
    /** Contributor ID */
    private long id;
    /** Number of contributions */
    private int contributions;
    /** Contributor avatar URL */
    private String avatarUrl;
    /** Contributor API URL */
    private String url;

    public Contributor() {}

    // Getters and setters
    public String getLogin() { return login; }
    public void setLogin(String login) { this.login = login; }
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }
    public int getContributions() { return contributions; }
    public void setContributions(int contributions) { this.contributions = contributions; }
    public String getAvatarUrl() { return avatarUrl; }
    public void setAvatarUrl(String avatarUrl) { this.avatarUrl = avatarUrl; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    @Override
    public String toString() {
        return "Contributor{" +
                "login='" + login + '\'' +
                ", id=" + id +
                ", contributions=" + contributions +
                ", avatarUrl='" + avatarUrl + '\'' +
                ", url='" + url + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Contributor that = (Contributor) o;
        return id == that.id &&
                contributions == that.contributions &&
                Objects.equals(login, that.login) &&
                Objects.equals(avatarUrl, that.avatarUrl) &&
                Objects.equals(url, that.url);
    }

    @Override
    public int hashCode() {
        return Objects.hash(login, id, contributions, avatarUrl, url);
    }
}
