package org.demo.maven.util;

import org.demo.maven.exception.GeneratorException;
import org.demo.maven.model.ApiConfig;

import java.util.logging.Level;
import java.util.logging.Logger;

public class GitHubRepoManager {

    private static final Logger logger = Logger.getLogger(GitHubRepoManager.class.getName());

    private String githubToken;
    private String repositoryUrl;
    private String branch;

    public void connect(String token) throws GeneratorException {
        this.githubToken = token;
        logger.info("GitHub Token已设置");
    }

    public void connectWithConfig(ApiConfig.GitConfig gitConfig) throws GeneratorException {
        if (gitConfig == null) {
            throw new GeneratorException("GitConfig不能为空");
        }
        connect(gitConfig.getToken());
        this.repositoryUrl = gitConfig.getUrl();
        this.branch = gitConfig.getBranch();
    }

    public boolean pushCode(String localPath, String branch) throws GeneratorException {
        logger.warning("GitHub推送功能已禁用，请手动推送代码");
        logger.info("本地路径: " + localPath);
        logger.info("分支: " + (branch != null ? branch : this.branch));
        if (repositoryUrl != null) {
            logger.info("仓库地址: " + repositoryUrl);
        }
        return false;
    }

    public String getRepositoryUrl() {
        return repositoryUrl;
    }

    public void disconnect() {
        githubToken = null;
        repositoryUrl = null;
        branch = null;
        logger.info("已断开GitHub连接");
    }
}