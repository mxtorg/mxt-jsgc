package org.demo.maven.util;

import org.demo.maven.exception.GeneratorException;
import org.demo.maven.model.ApiConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class GitHubRepoManagerTest {

    private GitHubRepoManager gitHubRepoManager;

    @BeforeEach
    void setUp() {
        gitHubRepoManager = new GitHubRepoManager();
    }

    @Test
    void testConnect_WithValidToken() throws GeneratorException {
        gitHubRepoManager.connect("valid-token");
        assertNotNull(gitHubRepoManager);
    }

    @Test
    void testConnect_WithInvalidToken() {
        assertThrows(GeneratorException.class, () -> {
            gitHubRepoManager.connect(null);
        });
    }

    @Test
    void testConnectWithConfig_WithValidConfig() throws GeneratorException {
        ApiConfig.GitConfig gitConfig = new ApiConfig.GitConfig();
        gitConfig.setToken("test-token");
        gitConfig.setBranch("main");
        gitConfig.setUrl("https://github.com/test/repo");

        gitHubRepoManager.connectWithConfig(gitConfig);

        assertEquals("https://github.com/test/repo", gitHubRepoManager.getRepositoryUrl());
    }

    @Test
    void testConnectWithConfig_WithNullConfig() {
        assertThrows(GeneratorException.class, () -> {
            gitHubRepoManager.connectWithConfig(null);
        });
    }

    @Test
    void testPushCode() throws GeneratorException {
        gitHubRepoManager.connect("test-token");

        boolean result = gitHubRepoManager.pushCode("/tmp/test", "main");

        assertFalse(result);
    }

    @Test
    void testGetRepositoryUrl() throws GeneratorException {
        ApiConfig.GitConfig gitConfig = new ApiConfig.GitConfig();
        gitConfig.setToken("test-token");
        gitConfig.setUrl("https://github.com/test/repo");

        gitHubRepoManager.connectWithConfig(gitConfig);

        assertEquals("https://github.com/test/repo", gitHubRepoManager.getRepositoryUrl());
    }

    @Test
    void testDisconnect() throws GeneratorException {
        ApiConfig.GitConfig gitConfig = new ApiConfig.GitConfig();
        gitConfig.setToken("test-token");

        gitHubRepoManager.connectWithConfig(gitConfig);
        gitHubRepoManager.disconnect();

        assertNull(gitHubRepoManager.getRepositoryUrl());
    }
}