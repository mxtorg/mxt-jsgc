# API 接口规范文档

## 1. 文档信息
| 项目名称 | 版本 | 创建日期 | 作者 |
|---------|------|---------|------|
| API Generator Maven Plugin | 1.0.0 | 2026-05-07 | AI Team |

## 2. 插件配置接口

### 2.1 Maven插件配置

#### 配置示例
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
        <skipGit>false</skipGit>
        <skipDeploy>false</skipDeploy>
        <skipIfExists>false</skipIfExists>
    </configuration>
</plugin>
```

#### 配置参数说明
| 参数名 | 类型 | 默认值 | 必填 | 说明 |
|--------|------|--------|------|------|
| specDir | File | src/main/resources/api-spec | 否 | JSON Schema配置文件目录 |
| outputJavaDir | File | src/main/java | 否 | 生成Java代码输出目录 |
| outputDocDir | File | src/main/docs | 否 | 生成文档输出目录 |
| skipGit | boolean | false | 否 | 是否跳过Git操作 |
| skipDeploy | boolean | false | 否 | 是否跳过Maven发布 |
| skipIfExists | boolean | false | 否 | 文件已存在时是否跳过 |

### 2.2 命令行接口

#### 执行命令
```bash
# 生成代码
mvn api-generator:generate-api-code

# 完整构建（包含生成代码）
mvn clean compile

# 跳过测试
mvn clean compile -DskipTests
```

#### 系统属性参数
| 参数名 | 说明 |
|--------|------|
| api-generator.specDir | 指定配置文件目录 |
| api-generator.outputJavaDir | 指定输出目录 |
| api-generator.skipGit | 跳过Git操作 |
| api-generator.skipDeploy | 跳过Maven发布 |

示例：
```bash
mvn api-generator:generate-api-code -Dapi-generator.skipGit=true
```

## 3. 核心类接口规范

### 3.1 ApiGeneratorMojo 接口

#### 类签名
```java
@Mojo(name = "generate-api-code", defaultPhase = LifecyclePhase.GENERATE_SOURCES)
public class ApiGeneratorMojo extends AbstractMojo
```

#### 主要方法
| 方法名 | 返回类型 | 说明 |
|--------|---------|------|
| execute() | void | 插件执行入口 |
| loadConfigFiles() | List&lt;File&gt; | 加载配置文件 |
| executeGenerators() | void | 执行代码生成器 |

### 3.2 CodeGenerator 接口

#### 接口定义
```java
public interface CodeGenerator {
    String getName();
    int getOrder();
    void generate(ApiConfig config, File outputDir) throws GeneratorException;
}
```

#### 内置实现
| 实现类 | 名称 | 顺序 |
|--------|------|------|
| DtoGenerator | dto-generator | 100 |
| ControllerGenerator | controller-generator | 200 |
| FeignClientGenerator | feign-generator | 300 |
| MarkdownDocGenerator | doc-generator | 400 |
| WireMockStubGenerator | mock-generator | 500 |

### 3.3 数据模型接口

#### ApiConfig
```java
public class ApiConfig {
    public Info getInfo();
    public GitConfig getGit();
    public GavConfig getGav();
    public Map<String, PathConfig> getPaths();
    public Components getComponents();
}
```

#### PathConfig
```java
public class PathConfig {
    public String getPath();
    public String getSchemaRef();
    public Map<HttpMethod, OperationConfig> getOperations();
}
```

#### OperationConfig
```java
public class OperationConfig {
    public String getSummary();
    public List<ParameterConfig> getParameters();
    public RequestBodyConfig getRequestBody();
    public Map<String, ResponseConfig> getResponses();
}
```

## 4. 工具类接口

### 4.1 NamingUtil

#### 方法列表
| 方法名 | 返回类型 | 说明 |
|--------|---------|------|
| pathToMethodName(String, String) | String | 路径转方法名 |
| refToClassName(String) | String | 引用转类名 |
| underscoreToCamelCase(String) | String | 下划线转驼峰 |
| capitalize(String) | String | 首字母大写 |
| uncapitalize(String) | String | 首字母小写 |

#### 使用示例
```java
// 路径转方法名
String methodName = NamingUtil.pathToMethodName("post", "/users/instance");
// 返回: "postUserInstance"

// 引用转类名
String className = NamingUtil.refToClassName("#/components/schemas/DemoEndpoint");
// 返回: "DemoEndpoint"
```

### 4.2 TypeMapper

#### 方法列表
| 方法名 | 返回类型 | 说明 |
|--------|---------|------|
| toJavaType(Schema) | TypeName | Schema转Java类型 |
| getDefaultValue(TypeName) | String | 获取类型默认值 |

#### 使用示例
```java
Schema schema = ...;
TypeName javaType = TypeMapper.toJavaType(schema);
// 返回: ClassName.get(String.class)
```

### 4.3 OpenApiUtil

#### 方法列表
| 方法名 | 返回类型 | 说明 |
|--------|---------|------|
| parse(File) | ApiConfig | 解析JSON Schema文件 |
| validate(ApiConfig) | List&lt;String&gt; | 验证配置 |

#### 使用示例
```java
File specFile = new File("api-spec.json");
ApiConfig config = OpenApiUtil.parse(specFile);
List<String> errors = OpenApiUtil.validate(config);
```

### 4.4 GitUtil

#### 方法列表
| 方法名 | 返回类型 | 说明 |
|--------|---------|------|
| cloneOrPull(GitConfig, File) | Git | 克隆或拉取仓库 |
| addAll(Git, File) | void | 添加所有文件 |
| commit(Git, String) | void | 提交更改 |
| push(Git, GitConfig) | void | 推送到远程 |

## 5. 异常接口

### 5.1 异常类层次
```
GeneratorException (基类)
├── ConfigParseException (配置解析异常)
├── CodeGenerationException (代码生成异常)
├── GitOperationException (Git操作异常)
└── MavenDeployException (Maven发布异常)
```

### 5.2 GeneratorException
```java
public class GeneratorException extends Exception {
    public GeneratorException(String message);
    public GeneratorException(String message, Throwable cause);
}
```

## 6. SPI扩展接口

### 6.1 扩展点
插件支持通过Java SPI机制扩展自定义Generator。

### 6.2 实现自定义Generator
1. 实现CodeGenerator接口
2. 在META-INF/services/org.demo.maven.generator.CodeGenerator中注册

### 6.3 示例
```java
public class CustomGenerator implements CodeGenerator {
    @Override
    public String getName() {
        return "custom-generator";
    }
    
    @Override
    public int getOrder() {
        return 1000;
    }
    
    @Override
    public void generate(ApiConfig config, File outputDir) {
        // 自定义生成逻辑
    }
}
```

## 7. 日志接口

### 7.1 日志级别
| 级别 | 使用场景 |
|------|---------|
| DEBUG | 详细执行步骤、参数值 |
| INFO | 主要执行阶段、生成文件统计 |
| WARN | 非致命错误、跳过操作 |
| ERROR | 致命错误、执行中断 |

### 7.2 日志格式
```
[API-Generator] ${阶段}: ${描述}
```

示例：
```
[API-Generator] 开始解析配置文件: api-spec.json
[API-Generator] 生成DTO类: RegisterRequest.java
[API-Generator] 代码生成完成，共生成5个文件
```

## 8. 返回码规范

| 返回码 | 说明 |
|--------|------|
| 0 | 执行成功 |
| 1 | 配置文件错误 |
| 2 | 代码生成错误 |
| 3 | Git操作错误 |
| 4 | Maven发布错误 |
| 5 | 其他错误 |
