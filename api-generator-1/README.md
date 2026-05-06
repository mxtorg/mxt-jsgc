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

## 配置说明

### 插件配置参数

| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| specDir | File | src/main/resources/api-spec | JSON配置文件目录 |
| outputJavaDir | File | src/main/java | Java代码输出目录 |
| skipGit | boolean | false | 是否跳过Git操作 |
| skipDeploy | boolean | false | 是否跳过Maven部署 |
| skipIfExists | boolean | false | 文件存在时是否跳过 |

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
