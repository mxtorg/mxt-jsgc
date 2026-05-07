package org.demo.maven.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.demo.maven.exception.GeneratorException;
import org.demo.maven.model.ApiConfig;
import org.demo.maven.util.GitHubRepoManager;
import org.demo.maven.util.LockFileManager;
import org.demo.maven.util.OpenApiUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * API代码生成Maven插件主入口
 */
@Mojo(name = "generate-api-code", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ApiGeneratorMojo extends AbstractMojo {

    private static final String GENERATOR_VERSION = "1.0.0";

    @Parameter(defaultValue = "${project.basedir}/src/main/resources/api-spec", property = "specDir")
    private File specDir;

    @Parameter(defaultValue = "${project.basedir}/src/main/java", property = "outputJavaDir")
    private File outputJavaDir;

    @Parameter(defaultValue = "false", property = "skipGit")
    private boolean skipGit;

    @Parameter(defaultValue = "false", property = "skipDeploy")
    private boolean skipDeploy;

    @Parameter(defaultValue = "false", property = "skipIfExists")
    private boolean skipIfExists;

    @Parameter(defaultValue = "false", property = "skipLockCheck")
    private boolean skipLockCheck;

    @Parameter(defaultValue = "false", property = "skipGitPush")
    private boolean skipGitPush;

    @Parameter(defaultValue = "false", property = "skipMavenDeploy")
    private boolean skipMavenDeploy;

    @Parameter(defaultValue = "${project}", readonly = true)
    private org.apache.maven.project.MavenProject project;

    private final List<CodeGenerator> generators = new ArrayList<>();

    public ApiGeneratorMojo() {
        // 注册内置生成器
        generators.add(new DtoGenerator());
        generators.add(new ControllerGenerator());
        generators.add(new FeignClientGenerator());
        
        // 按顺序排序
        generators.sort(Comparator.comparingInt(CodeGenerator::getOrder));
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        getLog().info("[API-Generator] 开始执行代码生成...");

        try {
            if (!specDir.exists()) {
                getLog().warn("[API-Generator] 配置目录不存在: " + specDir.getAbsolutePath());
                return;
            }

            List<File> specFiles = loadSpecFiles();
            if (specFiles.isEmpty()) {
                getLog().warn("[API-Generator] 没有找到JSON配置文件");
                return;
            }

            getLog().info("[API-Generator] 找到 " + specFiles.size() + " 个配置文件");

            Path projectDir = project.getBasedir().toPath();
            for (File specFile : specFiles) {
                if (!skipLockCheck && !needRegenerate(projectDir, specFile)) {
                    getLog().info("[API-Generator] 代码已是最新，无需生成: " + specFile.getName());
                    continue;
                }
                List<String> generatedFiles = processSpecFile(specFile);
                if (!generatedFiles.isEmpty()) {
                    updateLockFile(projectDir, specFile, generatedFiles);
                }
            }

            getLog().info("[API-Generator] 代码生成完成");

        } catch (Exception e) {
            getLog().error("[API-Generator] 代码生成失败", e);
            throw new MojoExecutionException("代码生成失败", e);
        }
    }

    private boolean needRegenerate(Path projectDir, File specFile) {
        String configMd5 = LockFileManager.calculateFileMd5(specFile);
        return LockFileManager.shouldRegenerate(projectDir, configMd5, GENERATOR_VERSION);
    }

    private void updateLockFile(Path projectDir, File specFile, List<String> generatedFiles) {
        String configContent;
        try {
            configContent = java.nio.file.Files.readString(specFile.toPath());
        } catch (IOException e) {
            getLog().warn("[API-Generator] 读取配置文件失败: " + e.getMessage());
            configContent = specFile.getName();
        }
        LockFileManager.updateLockFile(projectDir, configContent, generatedFiles, GENERATOR_VERSION);
    }

    private List<File> loadSpecFiles() {
        List<File> files = new ArrayList<>();
        if (specDir.isDirectory()) {
            File[] children = specDir.listFiles((dir, name) -> name.endsWith(".json"));
            if (children != null) {
                for (File child : children) {
                    files.add(child);
                }
            }
        } else if (specDir.isFile() && specDir.getName().endsWith(".json")) {
            files.add(specDir);
        }
        return files;
    }

    private List<String> processSpecFile(File specFile) throws GeneratorException, IOException {
        getLog().info("[API-Generator] 处理配置文件: " + specFile.getName());

        ApiConfig config = OpenApiUtil.parse(specFile);

        List<String> errors = OpenApiUtil.validate(config);
        if (!errors.isEmpty()) {
            for (String error : errors) {
                getLog().error("[API-Generator] 配置错误: " + error);
            }
            throw new GeneratorException("配置文件验证失败");
        }

        if (!outputJavaDir.exists()) {
            outputJavaDir.mkdirs();
        }

        List<String> generatedFiles = new ArrayList<>();
        for (CodeGenerator generator : generators) {
            getLog().info("[API-Generator] 执行生成器: " + generator.getName());
            generator.generate(config, outputJavaDir);
            generatedFiles.add(generator.getName() + " output");
        }

        if (!skipGitPush && config.getGit() != null) {
            pushToGitHub(config);
        }

        if (!skipMavenDeploy && !skipDeploy) {
            getLog().info("[API-Generator] Maven部署（待实现）");
        }

        return generatedFiles;
    }

    private void pushToGitHub(ApiConfig config) {
        try {
            GitHubRepoManager gitHubManager = new GitHubRepoManager();
            gitHubManager.connectWithConfig(config.getGit());
            gitHubManager.pushCode(project.getBasedir().getAbsolutePath(), config.getGit().getBranch());
            getLog().info("[API-Generator] GitHub推送完成");
        } catch (GeneratorException e) {
            getLog().warn("[API-Generator] GitHub推送失败: " + e.getMessage());
        }
    }

    // Setters for testing
    public void setSpecDir(File specDir) {
        this.specDir = specDir;
    }

    public void setOutputJavaDir(File outputJavaDir) {
        this.outputJavaDir = outputJavaDir;
    }

    public void setSkipGit(boolean skipGit) {
        this.skipGit = skipGit;
    }

    public void setSkipDeploy(boolean skipDeploy) {
        this.skipDeploy = skipDeploy;
    }

    public void setSkipIfExists(boolean skipIfExists) {
        this.skipIfExists = skipIfExists;
    }

    public void setSkipLockCheck(boolean skipLockCheck) {
        this.skipLockCheck = skipLockCheck;
    }

    public void setSkipGitPush(boolean skipGitPush) {
        this.skipGitPush = skipGitPush;
    }

    public void setSkipMavenDeploy(boolean skipMavenDeploy) {
        this.skipMavenDeploy = skipMavenDeploy;
    }
}
