# 上下文
## json schema 结构
{
  "$schema": "http://json-schema.org/draft-07/schema#", 
  "paths": {
    "/users/register": {
      "schema": { "$ref": "#/components/schemas/DemoEndpoint" },
      "post": {
        "operationId": "registerUser",
        "request": {
          "required": true,
          "content": {
            "application/json": {
              "schema": { "$ref": "#/components/schemas/RegisterRequest" }
            }
          }
        },
        "responses": {
          "201": {
            "description": "Created",
            "content": {
              "application/json": {
                "schema": { "$ref": "#/components/schemas/UserResponse" }
              }
            }
          }
        }
      }
    }
  },
  "components": {
    "pkg":"org.demo.api",
    "gav":"org.demo.cloud:demo-api:1.0.0-SNAPSHOT",
    "repository":"https://github.com/demo/demo-api",
    "schemas": {
      "DemoEndpoint":{
        "type": "object",
        "description":"阿斯兰的饭卡手动阀"
      },
      "RegisterRequest": {
        "type": "object",
        "description":"阿斯兰的饭卡手动阀",
        "properties": {
          "username": { "type": "string", "minLength": 3 },
          "email": { "type": "string", "format": "email" },
          "password": { "type": "string", "minLength": 8 }
        },
        "required": ["username", "email", "password"]
      },
      "UserResponse": {
        "type": "object",
        "properties": {
          "id": { "type": "integer" },
          "message": { "type": "string" }
        }
      }
    }
  }
}
## 功能说明
基于** json schema结构 ** ，实现以下功能：

1. 自动生成 Spring Boot Controller 桩代码
输入：paths + compone·nts/schemas
输出：@RestController 类，包含方法参数、@RequestBody、@PathVariable（如果 path 含变量）、@RequestParam 等。
效能提升：后端开发只需填充业务逻辑，无需手写参数接收、对象转换、响应封装。

2. 生成 OpenFeign 客户端接口
输入：paths + method、operationId、request/response schemas
输出：@FeignClient 接口，方法签名与 Controller 完全一致。
效能提升：调用方一行依赖引入，直接 @Autowired 使用，无需手写 HTTP 调用代码，避免 URL 硬编码和手动序列化。

3. 生成请求/响应 DTO 及校验注解
输入：components/schemas 中的 JSON Schema
输出：Java POJO，字段携带 JSR-303 校验注解（如 @NotNull、@Size(min=3)、@Email）。
效能提升：自动完成参数校验，无需手写 if 判断；Schema 变更时 DTO 自动同步。

4. 自动生成 API 文档（Markdown / Swagger UI）
输入：直接使用 OpenAPI 兼容部分
输出：swagger-ui.html 可识别配置，或静态接口文档。
效能提升：契约即文档，避免手工维护 .md 文件。

5. 生成 Mock 测试服务器（基于 WireMock 或 Mockito）
输入：responses 中各状态码的 schema 示例
输出：按路径和请求内容匹配的 Mock 规则，返回符合 Schema 的示例 JSON。
效能提升：前端/上游服务可在 API 后端未完成时并发开发，无等待。

6. 生成集成测试用例骨架
输入：request.schema 的 required、minLength、format 约束
输出：JUnit 测试用例，包含正常请求、边界值、非法参数等场景。
效能提升：自动覆盖常见校验分支，减少人工编写测试用例的工作量。

7. 生成 API 发布包（供其他服务直接依赖）
输入：GAV (org.demo.cloud:demo-api:1.0.0-SNAPSHOT) + 代码生成结果
输出：Maven 模块（包含客户端、DTO、契约 JSON），部署到 repository 指定的仓库。
效能提升：依赖方只需引入该 artifact，无需复制 DTO，实现跨项目共享。

8. 低代码编排工具集成（如 Camunda、Drools）
输入：请求/响应的 JSON Schema
输出：工作流变量定义、规则引擎的事实模型。
效能提升：打通 API 定义与业务流程建模，减少重复定义。


# 需求说明
一、核心创新点
契约即制品（Contract as Artifact）
将 API 定义（JSON Schema）与 Maven GAV 坐标绑定，发布到私有仓库。其他服务可通过依赖该制品获得接口的类型安全客户端/服务端骨架，彻底消除手工编写 DTO 和 Feign 接口的重复劳动。

双向代码生成 + 契约校验
基于同一份 JSON，同时生成：
服务端 Controller 接口（Spring MVC 注解）
客户端 @FeignClient 接口
请求/响应的 Java POJO 类
并且可以在 CI 中校验实现代码是否偏离契约（例如字段缺失、类型变化），实现“契约驱动开发”。
跨服务复用与版本管理
通过 GAV 版本号管理 API 演进。服务 A 升级 API 版本 → 所有依赖方自动感知，编译期捕获不兼容变更。配合 repository 字段，可集成私有 Nexus/Artifactory。
低代码 Mock 服务器
根据 JSON Schema 中的数据类型自动生成合理的 Mock 数据（例如利用 minLength、format 生成示例），无需人工编写 Mock 逻辑。

二、基于该 JSON 可提升效能的具体事项
使用 JavaPoet 生成源码，OpenAPI Parser 解析你提供的扩展 JSON，Maven Invoker 完成打包部署

1. 项目结构
text
api-generator-maven-plugin/
├── pom.xml (插件自己的 pom)
└── src/main/java/org/demo/maven/
    ├── ApiGeneratorMojo.java
    ├── generator/
    │   ├── DtoGenerator.java
    │   ├── ControllerGenerator.java
    │   ├── FeignClientGenerator.java
    │   ├── MarkdownDocGenerator.java
    │   ├── SwaggerUIConfigGenerator.java
    │   ├── WireMockStubGenerator.java
    │   ├── CamundaDmnGenerator.java
    │   ├── DroolsRuleGenerator.java
    │   └── ApiPackager.java
    └── util/
        ├── OpenApiUtil.java
        ├── TypeMapper.java
        └── NamingUtil.java
2. 插件 Mojo（入口）
java
package org.demo.maven;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.demo.maven.generator.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
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

    @Override
    public void execute() throws MojoExecutionException {
        getLog().info("Generating API artifacts from " + apiSpecFile.getAbsolutePath());
        try {
            // 1. 解析 OpenAPI
            OpenAPI openAPI = parseOpenAPI(apiSpecFile);
            // 2. 提取自定义扩展字段
            Map<String, Object> extensions = openAPI.getExtensions();
            String gav = (String) extensions.get("x-gav");
            String repository = (String) extensions.get("x-repository");
            String pkg = (String) extensions.get("x-pkg");
            if (pkg != null) basePackage = pkg;

            // 3. 生成 DTO (带校验注解)
            new DtoGenerator(openAPI, outputDir, basePackage).generate();
            // 4. 生成 Controller 桩代码
            new ControllerGenerator(openAPI, outputDir, basePackage).generate();
            // 5. 生成 Feign Client
            new FeignClientGenerator(openAPI, outputDir, basePackage).generate();
            // 6. 生成 API 文档 (Markdown)
            new MarkdownDocGenerator(openAPI, outputDir).generate();
            // 7. 生成 Swagger UI 配置 (applicaton.yml 片段)
            new SwaggerUIConfigGenerator(openAPI, outputDir).generate();
            // 8. 生成 WireMock Mock 服务
            new WireMockStubGenerator(openAPI, outputDir, basePackage).generate();
            // 9. 生成 Camunda DMN 集成
            new CamundaDmnGenerator(openAPI, outputDir, basePackage).generate();
            // 10. 生成 Drools Rule 集成 (可选)
            new DroolsRuleGenerator(openAPI, outputDir, basePackage).generate();

            // 11. 打包并发布 API jar (包含所有生成类)
            if (!skipDeploy && gav != null && repository != null) {
                new ApiPackager(gav, repository, outputDir, basePackage, apiSpecFile).packageAndDeploy();
            } else {
                getLog().info("Skipping deploy (no GAV/repository provided or skipDeploy=true)");
            }
            getLog().info("API generation completed.");
        } catch (Exception e) {
            throw new MojoExecutionException("Error generating API", e);
        }
    }

    private OpenAPI parseOpenAPI(File specFile) throws IOException {
        ParseOptions options = new ParseOptions();
        options.setResolve(true);
        return new OpenAPIV3Parser().read(specFile.getAbsolutePath(), null, options);
    }
}
3. DTO 生成器（含校验注解）
java
package org.demo.maven.generator;

import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.MapSchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import org.demo.maven.util.TypeMapper;

import javax.lang.model.element.Modifier;
import javax.validation.constraints.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

public class DtoGenerator {

    private final OpenAPI openAPI;
    private final Path outputDir;
    private final String basePackage;

    public DtoGenerator(OpenAPI openAPI, File outputDir, String basePackage) {
        this.openAPI = openAPI;
        this.outputDir = outputDir.toPath();
        this.basePackage = basePackage;
    }

    public void generate() throws IOException {
        Components components = openAPI.getComponents();
        if (components == null || components.getSchemas() == null) return;
        Map<String, Schema> schemas = components.getSchemas();
        for (Map.Entry<String, Schema> entry : schemas.entrySet()) {
            String name = entry.getKey();
            Schema schema = entry.getValue();
            TypeSpec.Builder classBuilder = TypeSpec.classBuilder(name)
                    .addModifiers(Modifier.PUBLIC)
                    .addAnnotation(AnnotationSpec.builder(ClassName.get("com.fasterxml.jackson.annotation", "JsonIgnoreProperties"))
                            .addMember("ignoreUnknown", "true")
                            .build());

            // 字段
            Map<String, Schema> properties = schema.getProperties();
            List<String> required = schema.getRequired() != null ? schema.getRequired() : Collections.emptyList();
            for (Map.Entry<String, Schema> prop : properties.entrySet()) {
                String fieldName = prop.getKey();
                Schema propSchema = prop.getValue();
                TypeName fieldType = TypeMapper.toJavaType(propSchema);
                FieldSpec.Builder fieldBuilder = FieldSpec.builder(fieldType, fieldName, Modifier.PRIVATE);
                // 添加校验注解
                addValidationAnnotations(fieldBuilder, propSchema, required.contains(fieldName));
                classBuilder.addField(fieldBuilder.build());
                // getter
                classBuilder.addMethod(createGetter(fieldName, fieldType));
                // setter
                classBuilder.addMethod(createSetter(fieldName, fieldType));
            }
            // 生成 Java 文件
            JavaFile javaFile = JavaFile.builder(basePackage + ".dto", classBuilder.build())
                    .build();
            javaFile.writeTo(outputDir);
        }
    }

    private void addValidationAnnotations(FieldSpec.Builder field, Schema schema, boolean isRequired) {
        if (isRequired) {
            field.addAnnotation(NotNull.class);
        }
        Integer minLength = schema.getMinLength();
        Integer maxLength = schema.getMaxLength();
        if (minLength != null || maxLength != null) {
            AnnotationSpec.Builder sizeBuilder = AnnotationSpec.builder(Size.class);
            if (minLength != null) sizeBuilder.addMember("min", "$L", minLength);
            if (maxLength != null) sizeBuilder.addMember("max", "$L", maxLength);
            field.addAnnotation(sizeBuilder.build());
        }
        if ("email".equals(schema.getFormat())) {
            field.addAnnotation(Email.class);
        }
        if (schema.getMinimum() != null) {
            field.addAnnotation(AnnotationSpec.builder(Min.class)
                    .addMember("value", "$L", schema.getMinimum().longValue())
                    .build());
        }
        if (schema.getMaximum() != null) {
            field.addAnnotation(AnnotationSpec.builder(Max.class)
                    .addMember("value", "$L", schema.getMaximum().longValue())
                    .build());
        }
        if (schema.getPattern() != null) {
            field.addAnnotation(AnnotationSpec.builder(Pattern.class)
                    .addMember("regexp", "$S", schema.getPattern())
                    .build());
        }
    }

    private MethodSpec createGetter(String fieldName, TypeName type) {
        String getterName = "get" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return MethodSpec.methodBuilder(getterName)
                .addModifiers(Modifier.PUBLIC)
                .returns(type)
                .addStatement("return this.$N", fieldName)
                .build();
    }

    private MethodSpec createSetter(String fieldName, TypeName type) {
        String setterName = "set" + Character.toUpperCase(fieldName.charAt(0)) + fieldName.substring(1);
        return MethodSpec.methodBuilder(setterName)
                .addModifiers(Modifier.PUBLIC)
                .addParameter(type, fieldName)
                .addStatement("this.$N = $N", fieldName, fieldName)
                .build();
    }
}
4. Controller 生成器（Spring MVC 桩）
java
package org.demo.maven.generator;

import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.parameters.RequestBody;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.demo.maven.util.TypeMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class ControllerGenerator {

    private final OpenAPI openAPI;
    private final Path outputDir;
    private final String basePackage;

    public ControllerGenerator(OpenAPI openAPI, File outputDir, String basePackage) {
        this.openAPI = openAPI;
        this.outputDir = outputDir.toPath();
        this.basePackage = basePackage;
    }

    public void generate() throws IOException {
        String controllerName = NamingUtil.capitalize(openAPI.getInfo().getTitle()) + "Controller";
        TypeSpec.Builder controllerBuilder = TypeSpec.classBuilder(controllerName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(RestController.class)
                .addAnnotation(AnnotationSpec.builder(RequestMapping.class)
                        .addMember("value", "$S", "/api")
                        .build());

        Paths paths = openAPI.getPaths();
        if (paths != null) {
            for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
                String path = entry.getKey();
                PathItem pathItem = entry.getValue();
                // 处理各 HTTP 方法
                processOperation(controllerBuilder, path, PathItem.HttpMethod.GET, pathItem.getGet());
                processOperation(controllerBuilder, path, PathItem.HttpMethod.POST, pathItem.getPost());
                processOperation(controllerBuilder, path, PathItem.HttpMethod.PUT, pathItem.getPut());
                processOperation(controllerBuilder, path, PathItem.HttpMethod.DELETE, pathItem.getDelete());
                // 可继续添加 PATCH 等
            }
        }

        JavaFile javaFile = JavaFile.builder(basePackage + ".controller", controllerBuilder.build()).build();
        javaFile.writeTo(outputDir);
    }

    private void processOperation(TypeSpec.Builder builder, String path, PathItem.HttpMethod method, Operation op) {
        if (op == null) return;
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(op.getOperationId())
                .addModifiers(Modifier.PUBLIC)
                .returns(ParameterizedTypeName.get(ClassName.get(ResponseEntity.class), TypeName.OBJECT));

        // 添加映射注解
        AnnotationSpec mappingAnno;
        switch (method) {
            case GET:
                mappingAnno = AnnotationSpec.builder(GetMapping.class).addMember("value", "$S", path).build();
                break;
            case POST:
                mappingAnno = AnnotationSpec.builder(PostMapping.class).addMember("value", "$S", path).build();
                break;
            case PUT:
                mappingAnno = AnnotationSpec.builder(PutMapping.class).addMember("value", "$S", path).build();
                break;
            case DELETE:
                mappingAnno = AnnotationSpec.builder(DeleteMapping.class).addMember("value", "$S", path).build();
                break;
            default:
                mappingAnno = AnnotationSpec.builder(RequestMapping.class)
                        .addMember("value", "$S", path)
                        .addMember("method", "RequestMethod." + method.name())
                        .build();
        }
        methodBuilder.addAnnotation(mappingAnno);

        // 路径参数
        for (Parameter param : op.getParameters()) {
            if (param.getIn() == Parameter.In.PATH) {
                TypeName paramType = TypeMapper.toJavaType(param.getSchema());
                methodBuilder.addParameter(paramType, param.getName())
                        .addAnnotation(AnnotationSpec.builder(PathVariable.class)
                                .addMember("value", "$S", param.getName())
                                .build());
            }
        }

        // 请求体
        RequestBody requestBody = op.getRequestBody();
        if (requestBody != null && requestBody.getContent() != null) {
            var content = requestBody.getContent().get("application/json");
            if (content != null) {
                var schema = content.getSchema();
                String dtoClassName = resolveRefName(schema.get$ref());
                TypeName bodyType = ClassName.get(basePackage + ".dto", dtoClassName);
                methodBuilder.addParameter(bodyType, "requestBody")
                        .addAnnotation(RequestBody.class);
            }
        }

        // 查询参数
        for (Parameter param : op.getParameters()) {
            if (param.getIn() == Parameter.In.QUERY) {
                TypeName paramType = TypeMapper.toJavaType(param.getSchema());
                AnnotationSpec.Builder reqParamAnno = AnnotationSpec.builder(RequestParam.class)
                        .addMember("value", "$S", param.getName())
                        .addMember("required", "$L", param.getRequired());
                if (param.getSchema().getDefault() != null) {
                    reqParamAnno.addMember("defaultValue", "$S", param.getSchema().getDefault().toString());
                }
                methodBuilder.addParameter(paramType, param.getName())
                        .addAnnotation(reqParamAnno.build());
            }
        }

        // 响应：默认返回 ResponseEntity.ok().build()，实际可根据 responses 生成更精细的 mock
        ApiResponses responses = op.getResponses();
        if (responses != null && responses.containsKey("200")) {
            methodBuilder.addStatement("return ResponseEntity.ok().build()");
        } else if (responses != null && responses.containsKey("201")) {
            methodBuilder.addStatement("return ResponseEntity.status(201).build()");
        } else {
            methodBuilder.addStatement("return ResponseEntity.ok().build()");
        }

        builder.addMethod(methodBuilder.build());
    }

    private String resolveRefName(String ref) {
        if (ref != null && ref.startsWith("#/components/schemas/")) {
            return ref.substring("#/components/schemas/".length());
        }
        return "Object";
    }
}
5. Feign Client 生成器
java
package org.demo.maven.generator;

import com.squareup.javapoet.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class FeignClientGenerator {

    private final OpenAPI openAPI;
    private final Path outputDir;
    private final String basePackage;

    public FeignClientGenerator(OpenAPI openAPI, File outputDir, String basePackage) {
        this.openAPI = openAPI;
        this.outputDir = outputDir.toPath();
        this.basePackage = basePackage;
    }

    public void generate() throws IOException {
        String clientName = NamingUtil.capitalize(openAPI.getInfo().getTitle()) + "Client";
        TypeSpec.Builder clientBuilder = TypeSpec.interfaceBuilder(clientName)
                .addModifiers(Modifier.PUBLIC)
                .addAnnotation(AnnotationSpec.builder(FeignClient.class)
                        .addMember("name", "$S", openAPI.getInfo().getTitle())
                        .addMember("url", "$S", "${api." + openAPI.getInfo().getTitle() + ".url:}")
                        .build());

        Paths paths = openAPI.getPaths();
        for (Map.Entry<String, PathItem> entry : paths.entrySet()) {
            String path = entry.getKey();
            PathItem pathItem = entry.getValue();
            addMethod(clientBuilder, path, PathItem.HttpMethod.GET, pathItem.getGet());
            addMethod(clientBuilder, path, PathItem.HttpMethod.POST, pathItem.getPost());
            addMethod(clientBuilder, path, PathItem.HttpMethod.PUT, pathItem.getPut());
            addMethod(clientBuilder, path, PathItem.HttpMethod.DELETE, pathItem.getDelete());
        }

        JavaFile javaFile = JavaFile.builder(basePackage + ".client", clientBuilder.build()).build();
        javaFile.writeTo(outputDir);
    }

    private void addMethod(TypeSpec.Builder builder, String path, PathItem.HttpMethod method, Operation op) {
        if (op == null) return;
        MethodSpec.Builder methodBuilder = MethodSpec.methodBuilder(op.getOperationId())
                .addModifiers(Modifier.PUBLIC, Modifier.ABSTRACT)
                .returns(ClassName.get("org.springframework.http", "ResponseEntity", TypeName.OBJECT));

        // 添加映射注解
        AnnotationSpec mappingAnno;
        switch (method) {
            case GET:
                mappingAnno = AnnotationSpec.builder(GetMapping.class).addMember("value", "$S", path).build();
                break;
            case POST:
                mappingAnno = AnnotationSpec.builder(PostMapping.class).addMember("value", "$S", path).build();
                break;
            // ... 其他类似
            default:
                mappingAnno = AnnotationSpec.builder(RequestMapping.class)
                        .addMember("value", "$S", path)
                        .addMember("method", "RequestMethod." + method.name())
                        .build();
        }
        methodBuilder.addAnnotation(mappingAnno);

        // 参数
        for (Parameter param : op.getParameters()) {
            TypeName paramType = TypeMapper.toJavaType(param.getSchema());
            if (param.getIn() == Parameter.In.PATH) {
                methodBuilder.addParameter(paramType, param.getName())
                        .addAnnotation(AnnotationSpec.builder(PathVariable.class)
                                .addMember("value", "$S", param.getName()).build());
            } else if (param.getIn() == Parameter.In.QUERY) {
                methodBuilder.addParameter(paramType, param.getName())
                        .addAnnotation(AnnotationSpec.builder(RequestParam.class)
                                .addMember("value", "$S", param.getName())
                                .addMember("required", "$L", param.getRequired()).build());
            }
        }
        // 请求体类似 Controller 生成
        builder.addMethod(methodBuilder.build());
    }
}
6. 其他生成器（简洁版）
6.1 MarkdownDocGenerator
java
public class MarkdownDocGenerator {
    public void generate(OpenAPI openAPI, File outputDir) throws IOException {
        // 生成 README.md，基于 paths 和 schemas
        StringBuilder md = new StringBuilder();
        md.append("# ").append(openAPI.getInfo().getTitle()).append("\n\n");
        md.append("## Endpoints\n");
        // 遍历 paths 写表格
        Path target = outputDir.toPath().resolve("api-docs.md");
        Files.write(target, md.toString().getBytes(StandardCharsets.UTF_8));
    }
}
6.2 SwaggerUIConfigGenerator
java
public class SwaggerUIConfigGenerator {
    public void generate(OpenAPI openAPI, File outputDir) throws IOException {
        // 生成 application.yml 片段，配置 springdoc
        String yaml = """
                springdoc:
                  api-docs:
                    path: /v3/api-docs
                  swagger-ui:
                    path: /swagger-ui.html
                """;
        Path target = outputDir.toPath().resolve("application-swagger.yml");
        Files.write(target, yaml.getBytes(StandardCharsets.UTF_8));
    }
}
6.3 WireMockStubGenerator
java
public class WireMockStubGenerator {
    public void generate(OpenAPI openAPI, File outputDir, String basePackage) throws IOException {
        // 生成一个 Java 类，使用 WireMock 规则
        TypeSpec.Builder stubClass = TypeSpec.classBuilder("WireMockStubs")
                .addModifiers(Modifier.PUBLIC);
        MethodSpec setup = MethodSpec.methodBuilder("setupStubs")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addStatement("// TODO: implement stubs per path")
                .build();
        stubClass.addMethod(setup);
        JavaFile.builder(basePackage + ".mock", stubClass.build()).writeTo(outputDir.toPath());
    }
}
6.4 CamundaDmnGenerator
java
public class CamundaDmnGenerator {
    public void generate(OpenAPI openAPI, File outputDir, String basePackage) throws IOException {
        // 生成简单的 .dmn 文件内容
        StringBuilder dmn = new StringBuilder();
        dmn.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n")
           .append("<definitions xmlns=\"https://www.omg.org/spec/DMN/20191111/MODEL/\" ...>\n")
           .append("  <itemDefinition id=\"RegisterRequest\">\n")
           .append("    <structureRef>").append(basePackage).append(".dto.RegisterRequest</structureRef>\n")
           .append("  </itemDefinition>\n")
           .append("</definitions>");
        Path target = outputDir.toPath().resolve("camunda/api-model.dmn");
        Files.write(target, dmn.toString().getBytes(StandardCharsets.UTF_8));
    }
}
6.5 DroolsRuleGenerator
java
public class DroolsRuleGenerator {
    public void generate(OpenAPI openAPI, File outputDir, String basePackage) throws IOException {
        // 生成 DRL 文件，声明事实类型
        StringBuilder drl = new StringBuilder();
        drl.append("package org.demo.rules\n")
           .append("import ").append(basePackage).append(".dto.*;\n\n")
           .append("rule \"Example\"\n")
           .append("  when\n")
           .append("    $r : RegisterRequest(username != null)\n")
           .append("  then\n")
           .append("    System.out.println(\"Valid request\");\n")
           .append("end\n");
        Path target = outputDir.toPath().resolve("drools/api-rules.drl");
        Files.write(target, drl.toString().getBytes(StandardCharsets.UTF_8));
    }
}
7. API 打包与部署（ApiPackager）
java
package org.demo.maven.generator;

import org.apache.maven.shared.invoker.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

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
        // 1. 在 outputDir 下生成 pom.xml
        String[] gavParts = gav.split(":");
        String groupId = gavParts[0];
        String artifactId = gavParts[1];
        String version = gavParts[2];

        String pomContent = generatePom(groupId, artifactId, version);
        Path pomPath = outputDir.toPath().resolve("pom.xml");
        Files.write(pomPath, pomContent.getBytes());

        // 2. 复制原始 API 规范文件到生成的模块中
        Path specTarget = outputDir.toPath().resolve("src/main/resources/api-spec.json");
        Files.createDirectories(specTarget.getParent());
        Files.copy(originalSpecFile.toPath(), specTarget);

        // 3. 使用 Maven Invoker 执行 clean deploy
        InvocationRequest request = new DefaultInvocationRequest();
        request.setPomFile(pomPath.toFile());
        request.setGoals(Arrays.asList("clean", "deploy"));
        request.setProperties(Map.of("maven.deploy.skip", "false"));
        Invoker invoker = new DefaultInvoker();
        invoker.setMavenHome(new File(System.getenv("MAVEN_HOME")));
        InvocationResult result = invoker.execute(request);
        if (result.getExitCode() != 0) {
            throw new RuntimeException("Maven deploy failed");
        }
    }

    private String generatePom(String groupId, String artifactId, String version) {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <project xmlns="http://maven.apache.org/POM/4.0.0"
                         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
                         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
                    <modelVersion>4.0.0</modelVersion>
                    <groupId>%s</groupId>
                    <artifactId>%s</artifactId>
                    <version>%s</version>
                    <packaging>jar</packaging>
                    <properties>
                        <java.version>11</java.version>
                        <maven.compiler.source>11</maven.compiler.source>
                        <maven.compiler.target>11</maven.compiler.target>
                    </properties>
                    <dependencies>
                        <dependency>
                            <groupId>org.springframework.boot</groupId>
                            <artifactId>spring-boot-starter-web</artifactId>
                            <version>2.7.0</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>org.springframework.cloud</groupId>
                            <artifactId>spring-cloud-starter-openfeign</artifactId>
                            <version>3.1.3</version>
                            <scope>provided</scope>
                        </dependency>
                        <dependency>
                            <groupId>javax.validation</groupId>
                            <artifactId>validation-api</artifactId>
                            <version>2.0.1.Final</version>
                            <scope>provided</scope>
                        </dependency>
                    </dependencies>
                    <build>
                        <plugins>
                            <plugin>
                                <groupId>org.apache.maven.plugins</groupId>
                                <artifactId>maven-source-plugin</artifactId>
                                <version>3.2.1</version>
                                <executions>
                                    <execution>
                                        <id>attach-sources</id>
                                        <goals><goal>jar</goal></goals>
                                    </execution>
                                </executions>
                            </plugin>
                        </plugins>
                    </build>
                    <distributionManagement>
                        <repository>
                            <id>api-repo</id>
                            <url>%s</url>
                        </repository>
                    </distributionManagement>
                </project>
                """.formatted(groupId, artifactId, version, repository);
    }
}
8. 辅助工具类
java
// TypeMapper.java (部分映射)
public class TypeMapper {
    public static TypeName toJavaType(Schema schema) {
        String type = schema.getType();
        String format = schema.getFormat();
        if ("string".equals(type)) {
            if ("email".equals(format)) return ClassName.get(String.class);
            return ClassName.get(String.class);
        } else if ("integer".equals(type)) {
            if ("int32".equals(format)) return TypeName.INT;
            else return TypeName.LONG;
        } else if ("boolean".equals(type)) return TypeName.BOOLEAN;
        else if ("array".equals(type)) {
            Schema items = ((ArraySchema) schema).getItems();
            return ParameterizedTypeName.get(ClassName.get(List.class), toJavaType(items));
        } else if ("object".equals(type)) return ClassName.get(Object.class);
        else return ClassName.get(String.class);
    }
}

// NamingUtil.java
public class NamingUtil {
    public static String capitalize(String str) {
        if (str == null || str.isEmpty()) return "Api";
        return Character.toUpperCase(str.charAt(0)) + str.substring(1);
    }
}
9. 插件自身 pom.xml（关键部分）
xml
<project>
    <groupId>org.demo</groupId>
    <artifactId>api-generator-maven-plugin</artifactId>
    <version>1.0.0</version>
    <packaging>maven-plugin</packaging>
    <properties>
        <maven.compiler.source>11</maven.compiler.source>
        <maven.compiler.target>11</maven.compiler.target>
    </properties>
    <dependencies>
        <dependency>
            <groupId>org.apache.maven</groupId>
            <artifactId>maven-plugin-api</artifactId>
            <version>3.8.1</version>
        </dependency>
        <dependency>
            <groupId>org.apache.maven.plugin-tools</groupId>
            <artifactId>maven-plugin-annotations</artifactId>
            <version>3.6.0</version>
            <scope>provided</scope>
        </dependency>
        <dependency>
            <groupId>io.swagger.parser.v3</groupId>
            <artifactId>swagger-parser</artifactId>
            <version>2.1.12</version>
        </dependency>
        <dependency>
            <groupId>com.squareup</groupId>
            <artifactId>javapoet</artifactId>
            <version>1.13.0</version>
        </dependency>
        <dependency>
            <groupId>org.apache.maven.shared</groupId>
            <artifactId>maven-invoker</artifactId>
            <version>3.1.0</version>
        </dependency>
        <!-- 添加 spring 相关注解依赖用于编译 -->
        <dependency>
            <groupId>org.springframework</groupId>
            <artifactId>spring-web</artifactId>
            <version>5.3.20</version>
            <scope>provided</scope>
        </dependency>
        <!-- 其他依赖 -->
    </dependencies>
</project>
10. 使用示例
在项目根目录执行：

bash
mvn org.demo:api-generator-maven-plugin:1.0.0:generate \
  -DapiSpecFile=/path/to/api-spec.json \
  -DdeployRepository=https://nexus/repo
或在 pom.xml 中配置插件，绑定到 generate-sources 生命周期。

生成的产物将作为独立 Maven 模块发布，其他服务可通过添加 groupId:artifactId:version 依赖直接使用 DTO 和 Feign Client。