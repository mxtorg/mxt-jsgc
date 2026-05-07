package org.demo.maven.generator;

public class Repository {
    private String id;
    private String name;
    private String url;
    private String username;
    private String password;

    public Repository() {
    }

    public Repository(String id, String name, String url) {
        this.id = id;
        this.name = name;
        this.url = url;
    }

    public Repository(String id, String name, String url, String username, String password) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.username = username;
        this.password = password;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean hasCredentials() {
        return username != null && !username.isEmpty() && password != null && !password.isEmpty();
    }
}