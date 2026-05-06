package org.demo.maven;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.demo.maven.generator.*;
import org.demo.maven.util.GitUtil;

import java.io.File;
import java.io.IOException;
import java.util.Map;

@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ApiGeneratorMojo extends AbstractMojo {

    @Parameter(property = "apiSpecFile", required = true)
    private File apiSpecFile;

    @Parameter(defaultValue = "${project.build.directory}/generated-sources/api")
    private File outputDir;

    @Parameter(defaultValue = "org.demo.api")
    private String basePackage;

    @Parameter(property = "deployRepository")
    private String deployRepository;

    @Parameter(property = "skipDeploy", defaultValue = "false")
    private boolean skipDeploy;

    @Parameter(property = "skipGitPush", defaultValue = "true")
    private boolean skipGitPush;

    @Parameter(property = "gitBranch", defaultValue = "main")
    private String gitBranch;

    @Parameter(property = "gitToken")
    private String gitToken;

    @Parameter(property = "gitTargetPath", defaultValue = "api-module")
    private String gitTargetPath;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating API artifacts from " + apiSpecFile.getAbsolutePath());
        try {
            OpenAPI openAPI = parseOpenAPI(apiSpecFile);
            
            Map<String, Object> extensions = openAPI.getExtensions();
            String gav = extensions != null ? (String) extensions.get("x-gav") : null;
            String repository = extensions != null ? (String) extensions.get("x-repository") : null;
            String pkg = extensions != null ? (String) extensions.get("x-pkg") : null;
            if (pkg != null) {
                basePackage = pkg;
            }

            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }

            getLog().info("Generating DTOs...");
            new DtoGenerator(openAPI, outputDir, basePackage).generate();

            getLog().info("Generating Controller...");
            new ControllerGenerator(openAPI, outputDir, basePackage).generate();

            getLog().info("Generating Feign Client...");
            new FeignClientGenerator(openAPI, outputDir, basePackage).generate();

            getLog().info("Generating Markdown Documentation...");
            new MarkdownDocGenerator().generate(openAPI, outputDir);

            getLog().info("Generating Swagger UI Config...");
            new SwaggerUIConfigGenerator().generate(openAPI, outputDir);

            getLog().info("Generating WireMock Stubs...");
            new WireMockStubGenerator().generate(openAPI, outputDir, basePackage);

            getLog().info("Generating Camunda DMN...");
            new CamundaDmnGenerator().generate(openAPI, outputDir, basePackage);

            getLog().info("Generating Drools Rules...");
            new DroolsRuleGenerator().generate(openAPI, outputDir, basePackage);

            if (!skipDeploy && gav != null && repository != null) {
                getLog().info("Packaging and deploying API artifact...");
                new ApiPackager(gav, repository, outputDir, basePackage, apiSpecFile).packageAndDeploy();
            } else {
                getLog().info("Skipping deploy (no GAV/repository provided or skipDeploy=true)");
            }

            String gitUrl = extensions != null ? (String) extensions.get("x-git-url") : null;
            if (!skipGitPush && gitUrl != null && gitToken != null) {
                getLog().info("Pushing generated code to Git repository...");
                GitUtil gitUtil = new GitUtil(gitUrl, gitBranch, gitToken);
                gitUtil.pushToGit(outputDir, gitTargetPath);
                getLog().info("Successfully pushed to Git repository.");
            } else {
                getLog().info("Skipping Git push (no git URL/token provided or skipGitPush=true)");
            }

            getLog().info("API generation completed successfully.");
        } catch (Exception e) {
            getLog().error("Error generating API", e);
            throw new MojoExecutionException("Error generating API", e);
        }
    }

    private OpenAPI parseOpenAPI(File specFile) throws IOException {
        if (!specFile.exists()) {
            throw new IOException("API spec file not found: " + specFile.getAbsolutePath());
        }
        
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        options.setResolveFully(true);
        
        return new OpenAPIV3Parser().read(specFile.getAbsolutePath(), null, options);
    }
}