package org.demo.maven.generator;

import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Properties;

public class ApiPackager {

    private final String gav;
    private final String repository;
    private final File outputDir;
    private final String basePackage;
    private final File originalSpecFile;

    public ApiPackager(String gav, String repository, File outputDir, String basePackage, File originalSpecFile) {
        this.gav = gav;
        this.repository = repository;
        this.outputDir = outputDir;
        this.basePackage = basePackage;
        this.originalSpecFile = originalSpecFile;
    }

    public void packageAndDeploy() throws Exception {
        String[] gavParts = gav.split(":");
        String groupId = gavParts[0];
        String artifactId = gavParts[1];
        String version = gavParts[2];

        String pomContent = generatePom(groupId, artifactId, version);
        Path pomPath = outputDir.toPath().resolve("pom.xml");
        Files.write(pomPath, pomContent.getBytes(StandardCharsets.UTF_8));

        Path specTarget = outputDir.toPath().resolve("src/main/resources/api-spec.json");
        Files.createDirectories(specTarget.getParent());
        Files.copy(originalSpecFile.toPath(), specTarget);

        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomPath.toFile());
        request.setGoals(Arrays.asList("clean", "deploy"));
        Properties props = new Properties();
        props.put("maven.deploy.skip", "false");
        request.setProperties(props);
        
        Invoker invoker = new DefaultInvoker();
        String mavenHome = System.getenv("MAVEN_HOME");
        if (mavenHome != null && !mavenHome.isEmpty()) {
            invoker.setMavenHome(new File(mavenHome));
        }
        
        InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new RuntimeException("Maven deploy failed with exit code: " + result.getExitCode());
        }
    }

    private String generatePom(String groupId, String artifactId, String version) {
        String pomTemplate = 
            "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<project xmlns=\"http://maven.apache.org/POM/4.0.0\"\n" +
            "         xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n" +
            "         xsi:schemaLocation=\"http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd\">\n" +
            "    <modelVersion>4.0.0</modelVersion>\n" +
            "    <groupId>%s</groupId>\n" +
            "    <artifactId>%s</artifactId>\n" +
            "    <version>%s</version>\n" +
            "    <packaging>jar</packaging>\n" +
            "    <properties>\n" +
            "        <java.version>11</java.version>\n" +
            "        <maven.compiler.source>11</maven.compiler.source>\n" +
            "        <maven.compiler.target>11</maven.compiler.target>\n" +
            "    </properties>\n" +
            "    <dependencies>\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.boot</groupId>\n" +
            "            <artifactId>spring-boot-starter-web</artifactId>\n" +
            "            <version>2.7.0</version>\n" +
            "            <scope>provided</scope>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>org.springframework.cloud</groupId>\n" +
            "            <artifactId>spring-cloud-starter-openfeign</artifactId>\n" +
            "            <version>3.1.3</version>\n" +
            "            <scope>provided</scope>\n" +
            "        </dependency>\n" +
            "        <dependency>\n" +
            "            <groupId>javax.validation</groupId>\n" +
            "            <artifactId>validation-api</artifactId>\n" +
            "            <version>2.0.1.Final</version>\n" +
            "            <scope>provided</scope>\n" +
            "        </dependency>\n" +
            "    </dependencies>\n" +
            "    <build>\n" +
            "        <plugins>\n" +
            "            <plugin>\n" +
            "                <groupId>org.apache.maven.plugins</groupId>\n" +
            "                <artifactId>maven-source-plugin</artifactId>\n" +
            "                <version>3.2.1</version>\n" +
            "                <executions>\n" +
            "                    <execution>\n" +
            "                        <id>attach-sources</id>\n" +
            "                        <goals><goal>jar</goal></goals>\n" +
            "                    </execution>\n" +
            "                </executions>\n" +
            "            </plugin>\n" +
            "        </plugins>\n" +
            "    </build>\n" +
            "    <distributionManagement>\n" +
            "        <repository>\n" +
            "            <id>api-repo</id>\n" +
            "            <url>%s</url>\n" +
            "        </repository>\n" +
            "    </distributionManagement>\n" +
            "</project>";
        return String.format(pomTemplate, groupId, artifactId, version, repository);
    }
}