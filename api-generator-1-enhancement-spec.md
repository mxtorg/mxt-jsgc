# API Generator Maven Plugin - 增强功能规范

## Why

当前API Generator Maven Plugin存在以下问题需要优化：

1. 每次执行都会重新生成代码，即使文件未变化，造成不必要的开销
2. 无法防止重复生成，需要增量更新机制
3. Maven部署流程不完整，缺少自动化推送
4. GitHub仓库创建和推送需要手动操作

## What Changes

### 新增功能

1. **Lock文件机制**
   - 生成代码后，在项目目录下生成`.api-generator.lock`文件
   - Lock文件记录生成时间、配置文件的MD5哈希值
   - 再次执行时，对比Lock文件和当前配置，如果一致则跳过生成
2. **Maven自动部署**
   - 集成Maven Invoker实现自动化打包部署
   - 执行`mvn package`时自动根据api-schema.json生成代码，当执行deploy时发布到配置的Maven仓库并同步创建github仓库和完成推送
   - 支持私有Nexus/Artifactory仓库认证
3. **GitHub仓库自动创建和推送**
   - 使用GitHub API (github-api库)自动创建远程仓库
   - 支持设置仓库名称、描述、可见性
   - 自动初始化Git仓库并推送代码
   - 支持配置分支保护规则
4. **Maven生命周期集成**
   - `mvn generate-sources`: 仅生成代码（检查Lock文件）
   - `mvn package`: 生成代码并打包（不deploy）
   - `mvn deploy`: 生成代码、打包并部署到Maven仓库
   - 支持跳过各阶段配置

### 技术变更

- 新增依赖：`org.kohsuke:github-api:1.315`
- 新增Lock文件管理类：`LockFileManager.java`
- 新增GitHub集成类：`GitHubRepoManager.java`
- 修改`ApiGeneratorMojo.java`支持生命周期绑定

## Impact

### 受影响的规范

- `01-PRD-项目需求分析文档.md`: 新增FR-11（Lock文件机制）、FR-12（GitHub仓库管理）、FR-13（Maven自动部署）
- `04-开发规范-代码生成规则文档.md`: 新增Lock文件规范说明

### 受影响的代码

- `ApiGeneratorMojo.java`: 修改为支持多阶段执行
- 新增 `LockFileManager.java`: Lock文件管理
- 新增 `GitHubRepoManager.java`: GitHub API集成
- 修改 `ApiPackager.java`: 支持deploy到仓库

## ADDED Requirements

### Requirement: Lock文件机制

系统应该在代码生成完成后，在项目根目录生成`.api-generator.lock`文件，记录生成元数据。

#### Scenario: 首次生成

- **WHEN** 用户执行 `mvn generate-sources` 且项目目录下不存在Lock文件
- **THEN** 执行代码生成，生成`.api-generator.lock`文件

#### Scenario: 配置未变化，跳过生成

- **WHEN** 用户执行 `mvn generate-sources` 且Lock文件存在且配置的MD5哈希值一致
- **THEN** 跳过代码生成，直接返回"代码已是最新，无需生成"

#### Scenario: 配置变化，重新生成

- **WHEN** 用户执行 `mvn generate-sources` 且Lock文件存在但配置MD5哈希值变化
- **THEN** 重新生成代码，更新`.api-generator.lock`文件

### Requirement: Maven自动部署

系统应该在接收到`deploy`命令时，自动将生成的API包部署到配置的Maven仓库。

#### Scenario: 正常部署流程

- **WHEN** 用户执行 `mvn deploy` 且配置了有效的repository信息
- **THEN** 系统自动执行：生成代码 → 打包 → 上传到Maven仓库

#### Scenario: 仓库认证失败

- **WHEN** Maven部署时认证失败
- **THEN** 输出详细错误信息，抛出`MavenDeployException`异常

### Requirement: GitHub仓库自动创建

系统应该能够通过GitHub API自动创建远程仓库并推送代码。

#### Scenario: 创建新仓库

- **WHEN** 用户执行插件且配置了git信息（url、branch、token）
- **THEN** 系统通过GitHub API创建远程仓库，初始化本地Git仓库，推送代码

#### Scenario: 仓库已存在

- **WHEN** GitHub API返回仓库已存在（409状态码）
- **THEN** 跳过创建，直接推送代码到现有仓库

#### Scenario: GitHub API认证失败

- **WHEN** GitHub token无效或权限不足
- **THEN** 抛出`GitHubApiException`异常，提示检查token配置

### Requirement: Lock文件格式规范

Lock文件采用JSON格式，包含以下字段：

```json
{
  "generatedAt": "2026-05-07T10:30:00+08:00",
  "configMd5": "a1b2c3d4e5f6...",
  "generatedFiles": [
    "org/demo/cloud/controller/DemoEndpoint.java",
    "org/demo/cloud/dto/RegisterRequest.java"
  ],
  "generatorVersion": "1.0.0"
}
```

#### Scenario: Lock文件格式验证

- **WHEN** 读取Lock文件时
- **THEN** 验证JSON格式正确，包含所有必需字段
- **AND** 如果格式错误，记录警告并重新生成

### Requirement: 配置MD5计算规则

系统应该使用配置文件内容的MD5哈希值作为变更检测依据。

#### Scenario: MD5计算范围

- **WHEN** 计算配置MD5时
- **THEN** 对整个JSON配置文件内容计算MD5（不包含空白字符）
- **AND** 排除Lock文件本身的影响

## MODIFIED Requirements

### Requirement: 插件执行阶段配置

原`@Mojo`注解的`defaultPhase`从`GENERATE_SOURCES`改为支持多个阶段：

| Maven命令          | 对应阶段             | 执行内容           |
| ---------------- | ---------------- | -------------- |
| generate-sources | generate-sources | 仅生成代码，检查Lock   |
| package          | package          | 生成代码 + 打包      |
| deploy           | deploy           | 生成代码 + 打包 + 部署 |

## REMOVED Requirements

无

## Technical Specifications

### Lock文件存储位置

```
${project.basedir}/.api-generator.lock
```

### Lock文件忽略规则

`.gitignore`应包含：

```
.api-generator.lock
```

### GitHub API集成

使用`github-api`库，主要方法：

```java
// 创建仓库
GHRepository repo = gh.createRepository(repoName)
    .description(description)
    .private_(false)
    .initializeWithInitialBranch("main")
    .create();

// 推送代码
git.addAll().addPath(".").commit().push();
```

### Maven部署配置

生成的pom.xml需包含：

```xml
<distributionManagement>
  <repository>
    <id>nexus-releases</id>
    <url>${repository.url}</url>
  </repository>
  <snapshotRepository>
    <id>nexus-snapshots</id>
    <url>${repository.url}</url>
  </snapshotRepository>
</distributionManagement>
```

### 错误处理策略

| 错误类型           | 处理方式          | 影响范围   |
| -------------- | ------------- | ------ |
| Lock文件读取失败     | 记录警告，重新生成     | 继续执行   |
| MD5计算失败        | 记录错误，跳过Lock检查 | 强制重新生成 |
| GitHub API错误   | 记录错误，抛出异常     | 停止执行   |
| Maven Deploy失败 | 记录错误，抛出异常     | 停止执行   |

## Dependencies

### 新增Maven依赖

```xml
<dependency>
  <groupId>org.kohsuke</groupId>
  <artifactId>github-api</artifactId>
  <version>1.315</version>
</dependency>
```

