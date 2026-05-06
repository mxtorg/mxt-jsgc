package org.demo.maven.util;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

public class GitUtil {

    private final String repoUrl;
    private final String branch;
    private final String token;

    public GitUtil(String repoUrl, String branch, String token) {
        this.repoUrl = repoUrl;
        this.branch = branch;
        this.token = token;
    }

    public void pushToGit(File sourceDir, String targetPath) throws IOException, GitAPIException {
        File tempDir = Files.createTempDirectory("api-generator-git").toFile();
        
        try {
            cloneRepo(tempDir);
            copyGeneratedFiles(sourceDir, tempDir, targetPath);
            commitAndPush(tempDir);
        } finally {
            deleteDirectory(tempDir);
        }
    }

    private void cloneRepo(File targetDir) throws GitAPIException {
        UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(token, "");
        
        Git.cloneRepository()
                .setURI(repoUrl)
                .setDirectory(targetDir)
                .setBranch(branch)
                .setCredentialsProvider(credentials)
                .call();
    }

    private void copyGeneratedFiles(File sourceDir, File repoDir, String targetPath) throws IOException {
        Path targetDir = repoDir.toPath().resolve(targetPath);
        if (Files.exists(targetDir)) {
            deleteDirectory(targetDir.toFile());
        }
        Files.createDirectories(targetDir);
        
        try (Stream<Path> paths = Files.walk(sourceDir.toPath())) {
            paths.filter(Files::isRegularFile).forEach(sourcePath -> {
                try {
                    Path relativePath = sourceDir.toPath().relativize(sourcePath);
                    Path destPath = targetDir.resolve(relativePath);
                    Files.createDirectories(destPath.getParent());
                    Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new RuntimeException("Failed to copy file: " + sourcePath, e);
                }
            });
        }
    }

    private void commitAndPush(File repoDir) throws GitAPIException {
        try (Git git = Git.open(repoDir)) {
            UsernamePasswordCredentialsProvider credentials = new UsernamePasswordCredentialsProvider(token, "");
            
            git.add()
                    .addFilepattern(".")
                    .call();
            
            git.commit()
                    .setMessage("chore: regenerate API stubs from schema")
                    .call();
            
            git.push()
                    .setCredentialsProvider(credentials)
                    .call();
        } catch (IOException e) {
            throw new RuntimeException("Failed to open Git repository", e);
        }
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    deleteDirectory(file);
                }
            }
        }
        dir.delete();
    }
}