# API Generator Maven Plugin 增强功能 - 任务清单

## 任务总览
- **总任务数**: 8
- **优先级**: P0（核心功能）, P1（重要功能）
- **预计工作量**: 中等

## 任务列表

### [ ] Task 1: 创建Lock文件管理器
**优先级**: P0
**依赖**: 无
**描述**: 实现LockFileManager类，管理Lock文件的创建、读取、验证和更新

**子任务**:
- [ ] 1.1 创建LockFileManager.java类
- [ ] 1.2 实现`createLockFile()`方法 - 生成Lock文件
- [ ] 1.3 实现`readLockFile()`方法 - 读取Lock文件
- [ ] 1.4 实现`validateLockFile()`方法 - 验证Lock文件格式
- [ ] 1.5 实现`shouldRegenerate()`方法 - 判断是否需要重新生成
- [ ] 1.6 实现MD5计算逻辑

**验收标准**:
- Lock文件格式为JSON，包含generatedAt、configMd5、generatedFiles、generatorVersion字段
- MD5计算基于配置文件内容，忽略空白字符差异

---

### [ ] Task 2: 创建GitHub仓库管理器
**优先级**: P0
**依赖**: Task 1（建议先完成，便于测试）
**描述**: 实现GitHubRepoManager类，通过GitHub API创建仓库并推送代码

**子任务**:
- [ ] 2.1 创建GitHubRepoManager.java类
- [ ] 2.2 实现`connect()`方法 - 连接GitHub API
- [ ] 2.3 实现`createOrGetRepository()`方法 - 创建或获取仓库
- [ ] 2.4 实现`pushCode()`方法 - 推送代码到远程仓库
- [ ] 2.5 处理409错误（仓库已存在）
- [ ] 2.6 处理认证失败异常

**验收标准**:
- 支持使用Token认证
- 能创建公开/私有仓库
- 仓库已存在时跳过创建，直接推送

---

### [ ] Task 3: 修改ApiGeneratorMojo支持多阶段执行
**优先级**: P0
**依赖**: Task 1, Task 2
**描述**: 修改插件主类，支持generate-sources、package、deploy三个阶段的绑定

**子任务**:
- [ ] 3.1 添加skipLockCheck参数
- [ ] 3.2 添加skipGitPush参数
- [ ] 3.3 添加skipMavenDeploy参数
- [ ] 3.4 实现phase绑定逻辑
- [ ] 3.5 集成LockFileManager检查
- [ ] 3.6 集成GitHubRepoManager

**验收标准**:
- `mvn generate-sources`: 仅生成代码
- `mvn package`: 生成代码 + 打包
- `mvn deploy`: 生成代码 + 打包 + 部署
- 支持通过参数跳过各阶段

---

### [ ] Task 4: 修改ApiPackager支持Maven Deploy
**优先级**: P0
**依赖**: Task 3
**描述**: 增强ApiPackager类，支持调用Maven Invoker执行deploy命令

**子任务**:
- [ ] 4.1 修改pom.xml生成逻辑，添加distributionManagement配置
- [ ] 4.2 实现`deployToRepository()`方法
- [ ] 4.3 添加仓库认证配置（settings.xml集成）
- [ ] 4.4 处理部署失败异常

**验收标准**:
- 能生成包含repository配置的pom.xml
- 能执行`mvn clean deploy`命令
- 正确处理部署成功/失败

---

### [ ] Task 5: 更新JSON Schema配置规范
**优先级**: P1
**依赖**: 无
**描述**: 更新配置规范，添加Lock文件相关的配置说明

**子任务**:
- [ ] 5.1 更新03-配置规范-JSON-Schema配置规范文档.md
- [ ] 5.2 添加Lock文件生成规则说明
- [ ] 5.3 添加Maven仓库认证配置说明

**验收标准**:
- 文档包含完整的Lock文件机制说明
- 文档包含Maven仓库配置示例

---

### [ ] Task 6: 添加单元测试
**优先级**: P1
**依赖**: Task 1, Task 2
**描述**: 为LockFileManager和GitHubRepoManager添加单元测试

**子任务**:
- [ ] 6.1 创建LockFileManagerTest测试类
- [ ] 6.2 测试Lock文件创建和读取
- [ ] 6.3 测试MD5计算准确性
- [ ] 6.4 测试Lock文件验证逻辑
- [ ] 6.5 创建GitHubRepoManagerTest测试类（Mock测试）

**验收标准**:
- 测试覆盖率达到80%以上
- 所有测试用例通过

---

### [ ] Task 7: 更新README文档
**优先级**: P1
**依赖**: Task 3, Task 4
**描述**: 更新README.md，添加新功能使用说明

**子任务**:
- [ ] 7.1 添加Lock文件机制说明
- [ ] 7.2 添加Maven多阶段使用说明
- [ ] 7.3 添加GitHub集成配置示例
- [ ] 7.4 添加常见问题解答

**验收标准**:
- README包含完整的功能说明
- 包含代码示例和使用场景

---

### [ ] Task 8: 集成测试和验证
**优先级**: P0
**依赖**: Task 3, Task 4
**描述**: 在demo项目上完整测试所有新功能

**子任务**:
- [ ] 8.1 测试Lock文件机制（首次生成、再次生成）
- [ ] 8.2 测试GitHub仓库创建（需要配置真实token）
- [ ] 8.3 测试Maven打包部署（需要配置真实仓库）
- [ ] 8.4 验证生成的代码完整性

**验收标准**:
- Lock文件正确生成和验证
- GitHub仓库成功创建并推送
- Maven包成功部署到仓库

---

## 任务依赖关系图

```
Task 1 (Lock文件管理器)
    ↑
Task 2 (GitHub管理器)     Task 5 (更新文档)
    ↑                           ↑
    └─────────┬─────────────────┘
              ↓
Task 3 (修改Mojo) ← Task 4 (修改Packager)
              ↓
Task 6 (单元测试)
              ↓
Task 7 (更新README)
              ↓
Task 8 (集成测试)
```

## 并行执行建议

以下任务可以并行执行：
- Task 1 和 Task 2（相互独立）
- Task 5 和 Task 6（在Task 1、2完成后）
- Task 7 可以在任何阶段进行文档更新

## 优先级排序建议

1. **P0核心任务**（必须完成）: Task 1, Task 2, Task 3, Task 4, Task 8
2. **P1重要任务**（增强功能）: Task 5, Task 6, Task 7
