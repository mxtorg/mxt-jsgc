package org.demo.maven.generator;

public class GavConfig {
    private String groupId;
    private String artifactId;
    private String version;
    private Repository[] repositories;

    public GavConfig() {
    }

    public GavConfig(String gav, Repository[] repositories) {
        String[] gavParts = gav.split(":");
        this.groupId = gavParts[0];
        this.artifactId = gavParts[1];
        this.version = gavParts[2];
        this.repositories = repositories;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.groupId = groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public void setArtifactId(String artifactId) {
        this.artifactId = artifactId;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Repository[] getRepositories() {
        return repositories;
    }

    public void setRepositories(Repository[] repositories) {
        this.repositories = repositories;
    }

    public String getGav() {
        return groupId + ":" + artifactId + ":" + version;
    }
}