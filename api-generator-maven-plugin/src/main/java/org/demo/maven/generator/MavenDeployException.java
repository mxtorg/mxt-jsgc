package org.demo.maven.generator;

public class MavenDeployException extends Exception {
    private final int exitCode;

    public MavenDeployException(String message) {
        super(message);
        this.exitCode = -1;
    }

    public MavenDeployException(String message, int exitCode) {
        super(message);
        this.exitCode = exitCode;
    }

    public MavenDeployException(String message, Throwable cause) {
        super(message, cause);
        this.exitCode = -1;
    }

    public MavenDeployException(String message, Throwable cause, int exitCode) {
        super(message, cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}