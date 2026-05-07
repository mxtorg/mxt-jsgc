package org.demo.maven.generator;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.demo.maven.exception.GeneratorException;
import org.demo.maven.model.ApiConfig;
import org.demo.maven.util.OpenApiUtil;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * API代码生成Maven插件主入口
 */
@Mojo(name = "generate-api-code", defaultPhase = LifecyclePhase.INITIALIZE/*.GENERATE_SOURCES*/)
public class ApiGeneratorMojo extends AbstractMojo {

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
            // 检查配置目录
            if (!specDir.exists()) {
                getLog().warn("[API-Generator] 配置目录不存在: " + specDir.getAbsolutePath());
                return;
            }

            // 加载配置文件
            List<File> specFiles = loadSpecFiles();
            if (specFiles.isEmpty()) {
                getLog().warn("[API-Generator] 没有找到JSON配置文件");
                return;
            }

            getLog().info("[API-Generator] 找到 " + specFiles.size() + " 个配置文件");

            // 处理每个配置文件
            for (File specFile : specFiles) {
                processSpecFile(specFile);
            }

            getLog().info("[API-Generator] 代码生成完成");

        } catch (Exception e) {
            getLog().error("[API-Generator] 代码生成失败", e);
            throw new MojoExecutionException("代码生成失败", e);
        }
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

    private void processSpecFile(File specFile) throws GeneratorException, IOException {
        getLog().info("[API-Generator] 处理配置文件: " + specFile.getName());

        // 解析配置文件
        ApiConfig config = OpenApiUtil.parse(specFile);

        // 验证配置
        List<String> errors = OpenApiUtil.validate(config);
        if (!errors.isEmpty()) {
            for (String error : errors) {
                getLog().error("[API-Generator] 配置错误: " + error);
            }
            throw new GeneratorException("配置文件验证失败");
        }

        // 确保输出目录存在
        if (!outputJavaDir.exists()) {
            outputJavaDir.mkdirs();
        }

        // 执行各生成器
        for (CodeGenerator generator : generators) {
            getLog().info("[API-Generator] 执行生成器: " + generator.getName());
            generator.generate(config, outputJavaDir);
        }

        // TODO: Git操作（如果需要）
        if (!skipGit) {
            getLog().info("[API-Generator] Git操作（待实现）");
        }

        // TODO: Maven部署（如果需要）
        if (!skipDeploy) {
            getLog().info("[API-Generator] Maven部署（待实现）");
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
}
