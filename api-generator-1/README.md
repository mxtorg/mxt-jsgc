# API Generator Maven Plugin

基于JSON Schema的Maven插件，自动生成Spring Boot Controller、OpenFeign客户端、DTO类等。

## 功能特性

- 自动生成Spring Boot Controller类
- 自动生成OpenFeign客户端接口
- 自动生成带JSR-303校验注解的DTO类
- 自动生成Swagger/OpenAPI注解
- 支持Git自动提交推送（待实现）
- 支持Maven自动部署（待实现）

## 快速开始

### 1. 安装插件

```bash
cd api-generator-1
mvn clean install
```

### 2. 在项目中配置插件

在你的项目`pom.xml`中添加：

```xml
<plugin>
    <groupId>org.demo</groupId>
    <artifactId>api-generator-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-api-code</goal>
            </goals>
            <phase>generate-sources</phase>
        </execution>
    </executions>
    <configuration>
        <specDir>src/main/resources/api-spec</specDir>
        <outputJavaDir>src/main/java</outputJavaDir>
    </configuration>
</plugin>
```

### 3. 创建JSON Schema配置文件

在`src/main/resources/api-spec/`目录下创建JSON配置文件，参考示例：`demo-api.json`

### 4. 执行生成代码

```bash
mvn clean compile
```

或者直接运行插件：

```bash
mvn org.demo:api-generator-maven-plugin:1.0.0:generate-api-code
```

## 新增功能

### Lock文件机制

#### 什么是Lock文件

Lock文件（`.api-generator.lock`）是用于记录最近一次代码生成状态的标记文件，默认存放在项目根目录。当插件执行生成任务时，会自动创建或更新该文件，记录生成时间、处理的Schema文件列表等信息。

#### Lock文件的作用

Lock文件的核心作用是实现**增量生成**：

- 插件会比较Lock文件中记录的上次生成状态与当前Schema文件状态
- 只有在Schema文件发生变更（修改时间或内容哈希变化）时，才会重新生成对应的代码
- 避免不必要的重复生成，提升大型项目的构建效率

#### Lock文件结构示例

```json
{
  "lastGenerationTime": "2026-05-07T10:30:00",
  "schemaFiles": [
    {
      "path": "src/main/resources/api-spec/user-api.json",
      "lastModified": 1715069400000,
      "contentHash": "a1b2c3d4e5f6"
    }
  ],
  "generatedOutputs": [
    {
      "type": "Controller",
      "file": "UserController.java",
      "schemaFile": "user-api.json"
    }
  ]
}
```

#### 如何强制重新生成

使用以下方式可以强制重新生成所有代码，跳过Lock检查：

```bash
mvn clean compile -DskipLockCheck=true
```

或者在pom.xml中配置：

```xml
<configuration>
    <skipLockCheck>true</skipLockCheck>
</configuration>
```

#### Lock文件管理命令

| 命令 | 说明 |
|------|------|
| `-DskipLockCheck=true` | 跳过Lock检查，强制重新生成所有文件 |
| 删除.lock文件 | 清除生成记录，下次执行将全部重新生成 |

### Maven多阶段执行

插件支持Maven的多个生命周期阶段，可以根据需要选择合适的执行方式：

#### mvn generate-sources：仅生成代码

此阶段仅执行代码生成，不进行打包或部署：

```bash
mvn generate-sources
```

**适用场景**：开发调试阶段，只需生成代码并查看效果。

#### mvn package：生成代码 + 打包

此阶段在生成代码后执行Maven打包：

```bash
mvn package
```

**适用场景**：需要将生成的代码打包成JAR文件进行测试。

#### mvn deploy：生成代码 + 打包 + 部署

此阶段执行完整的构建和部署流程：

```bash
mvn deploy
```

**适用场景**：完成开发后，需要将生成的 artifacts 部署到远程Maven仓库。

#### 各阶段参数配置

在pom.xml中配置插件时，可以针对不同阶段设置参数：

```xml
<plugin>
    <groupId>org.demo</groupId>
    <artifactId>api-generator-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <!-- generate-sources阶段 -->
        <execution>
            <id>generate-api</id>
            <goals>
                <goal>generate-api-code</goal>
            </goals>
            <phase>generate-sources</phase>
            <configuration>
                <specDir>src/main/resources/api-spec</specDir>
                <outputJavaDir>src/main/java</outputJavaDir>
            </configuration>
        </execution>
        
        <!-- package阶段 - 跳过Git推送 -->
        <execution>
            <id>package-api</id>
            <goals>
                <goal>generate-api-code</goal>
            </goals>
            <phase>package</phase>
            <configuration>
                <specDir>src/main/resources/api-spec</specDir>
                <outputJavaDir>src/main/java</outputJavaDir>
                <skipGitPush>true</skipGitPush>
            </configuration>
        </execution>
        
        <!-- deploy阶段 - 完整执行 -->
        <execution>
            <id>deploy-api</id>
            <goals>
                <goal>generate-api-code</goal>
            </goals>
            <phase>deploy</phase>
            <configuration>
                <specDir>src/main/resources/api-spec</specDir>
                <outputJavaDir>src/main/java</outputJavaDir>
            </configuration>
        </execution>
    </executions>
</plugin>
```

### GitHub自动集成

插件支持在代码生成后自动提交并推送到Git仓库，实现自动化工作流。

#### 功能说明

- **自动提交**：将生成的代码文件自动添加到Git并提交
- **自动推送**：可选是否推送到远程仓库
- **提交信息**：自动生成包含生成时间的提交信息

#### JSON配置示例

在JSON Schema文件中配置GitHub集成选项：

```json
{
  "apiName": "UserAPI",
  "git": {
    "enabled": true,
    "autoPush": true,
    "commitMessage": "feat: Auto generate API code",
    "branch": "main"
  },
  "endpoints": [
    {
      "path": "/users",
      "method": "GET",
      "responseType": "UserListResponse"
    }
  ]
}
```

#### 参数配置说明

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| skipGit | boolean | false | 是否跳过Git操作 |
| skipGitPush | boolean | false | 是否跳过Git推送（仅提交不推送） |
| gitCommitMessage | String | Auto generate API code | 提交信息 |

#### 使用示例

**完整Git工作流（提交并推送）**：

```bash
mvn clean compile -DskipGit=false
```

**仅生成代码，跳过Git操作**：

```bash
mvn clean compile -DskipGit=true
```

**仅提交，不推送到远程**：

```bash
mvn clean compile -DskipGitPush=true
```

#### 常见问题解答

**Q: Git推送失败怎么处理？**

A: 请检查以下配置：
1. 确保远程仓库地址配置正确
2. 确保本地Git凭证已配置（使用Git Credential Helper）
3. 检查网络连接是否正常

**Q: 如何避免生成的文件被Git追踪？**

A: 在`.gitignore`中添加：

```
# API Generator outputs
src/main/java/**/generated/
```

**Q: 能否自定义提交分支？**

A: 目前版本仅支持推送到默认分支，未来版本将支持指定分支。

### 命令行参数汇总

| 参数 | 说明 |
|------|------|
| `-DskipLockCheck=true` | 跳过Lock检查，强制重新生成所有文件 |
| `-DskipGitPush=true` | 跳过Git推送操作（仅提交本地） |
| `-DskipMavenDeploy=true` | 跳过Maven部署操作 |
| `-DskipGit=true` | 完全跳过Git操作 |

## 配置说明

### 插件配置参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| specDir | File | src/main/resources/api-spec | JSON配置文件目录 |
| outputJavaDir | File | src/main/java | Java代码输出目录 |
| skipGit | boolean | false | 是否跳过Git操作 |
| skipDeploy | boolean | false | 是否跳过Maven部署 |
| skipIfExists | boolean | false | 文件存在时是否跳过 |
| skipLockCheck | boolean | false | 是否跳过Lock检查 |
| skipGitPush | boolean | false | 是否跳过Git推送 |
| skipMavenDeploy | boolean | false | 是否跳过Maven部署 |

### JSON Schema配置规范

请参考文档：[JSON Schema配置规范文档](./03-配置规范-JSON-Schema配置规范文档.md)

## 文档索引

- [项目需求分析文档](./01-PRD-项目需求分析文档.md)
- [系统架构设计文档](./02-架构设计-系统架构设计文档.md)
- [JSON Schema配置规范文档](./03-配置规范-JSON-Schema配置规范文档.md)
- [代码生成规则文档](./04-开发规范-代码生成规则文档.md)
- [API接口规范文档](./05-接口规范-API接口规范文档.md)
- [测试规范文档](./06-测试规范-测试规范文档.md)

## 项目结构

```
api-generator-1/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── org/
│   │   │       └── demo/
│   │   │           └── maven/
│   │   │               ├── exception/
│   │   │               │   └── GeneratorException.java
│   │   │               ├── generator/
│   │   │               │   ├── ApiGeneratorMojo.java
│   │   │               │   ├── CodeGenerator.java
│   │   │               │   ├── ControllerGenerator.java
│   │   │               │   ├── DtoGenerator.java
│   │   │               │   └── FeignClientGenerator.java
│   │   │               ├── model/
│   │   │               │   └── ApiConfig.java
│   │   │               └── util/
│   │   │                   ├── NamingUtil.java
│   │   │                   ├── OpenApiUtil.java
│   │   │                   └── TypeMapper.java
│   │   └── resources/
│   │       └── api-spec/
│   │           └── demo-api.json
│   └── test/
│       └── java/
├── pom.xml
└── README.md
```

## 生成的代码示例

### DTO类示例

```java
@Data
@Schema(description = "注册请求体")
public class RegisterRequest {
    @NotBlank(message = "username不能为空")
    @Size(min = 3, message = "username长度不能少于3位")
    @Schema(description = "username")
    private String username;

    @NotBlank(message = "email不能为空")
    @Email(message = "email格式不正确")
    @Schema(description = "email")
    private String email;

    @NotBlank(message = "password不能为空")
    @Size(min = 8, message = "password长度不能少于8位")
    @Schema(description = "password")
    private String password;
}
```

### Controller类示例

```java
@Slf4j
@RestController
@Tag(name = "Demo端点")
public class DemoEndpoint {
    @PostMapping("/users/instance")
    @Operation(summary = "Register a new user")
    public Long postUserInstance(@RequestBody RegisterRequest request) {
        // todo 实现业务逻辑
        log.info("Register a new user，入参: {}", request);
        return 1L;
    }
}
```

### Feign Client示例

```java
@FeignClient(name = "Demo", url = "${api.Demo.url:}")
public interface DemoClient {
    @PostMapping("/users/instance")
    Long postUserInstance(@RequestBody RegisterRequest requestBody);
}
```

## 技术栈

- Java 11+
- Maven Plugin API
- JavaPoet 1.13.0
- Swagger Parser 2.1.12
- Jackson 2.13.0
- JGit 6.1.0

## 开发计划

- [x] 核心代码生成功能
- [ ] Git自动提交推送
- [ ] Maven自动部署
- [ ] API文档生成（Markdown）
- [ ] WireMock Mock配置生成
- [ ] 更多生成器扩展

## License

MIT License
