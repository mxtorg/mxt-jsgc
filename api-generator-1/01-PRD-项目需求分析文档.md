# 项目需求分析文档 (PRD)

## 1. 文档信息
| 项目名称 | 版本 | 创建日期 | 作者 |
|---------|------|---------|------|
| API Generator Maven Plugin | 1.0.0 | 2026-05-07 | AI Team |

## 2. 项目概述

### 2.1 项目背景
在现代微服务架构中，API契约定义与实现之间存在大量重复工作。开发人员需要手动编写DTO、Controller、Feign客户端等重复代码，效率低下且容易出错。本项目旨在通过JSON Schema驱动的代码生成，实现契约优先开发，提高开发效率。

### 2.2 项目目标
- 实现基于JSON Schema的自动代码生成
- 支持Spring Boot Controller、OpenFeign客户端、DTO类生成
- 自动生成JSR-303校验注解和Swagger文档注解
- 支持Git自动推送和Maven仓库自动发布
- 提供完整的API文档和Mock服务器配置

### 2.3 目标用户
- Java后端开发人员
- 系统架构师
- DevOps工程师

## 3. 功能需求 (FR)

### FR-1: JSON Schema解析
- **描述**: 解析扩展的JSON Schema配置文件
- **输入**: 符合draft-07规范的JSON文件
- **输出**: 结构化的配置对象
- **约束**: 必须包含info、git、gav、paths、components五大根节点

### FR-2: Controller代码生成
- **描述**: 自动生成Spring Boot @RestController类
- **输入**: paths配置
- **输出**: 包含完整注解和方法桩的Controller类
- **详细规则**:
  - 类注解：@RestController、@Tag、@Slf4j
  - 方法命名：httpMethod + 路径驼峰化
  - 映射注解：@PostMapping/@PutMapping/@GetMapping/@DeleteMapping
  - 入参注解：@RequestBody、@RequestParam
  - Swagger注解：@Operation

### FR-3: Feign客户端生成
- **描述**: 自动生成OpenFeign客户端接口
- **输入**: paths配置
- **输出**: 与Controller方法签名一致的Feign接口
- **详细规则**:
  - 类注解：@FeignClient(name="info.title", url="${api.xxx.url:}")
  - 方法签名：与Controller完全一致

### FR-4: DTO类生成
- **描述**: 自动生成带JSR-303校验注解的DTO类
- **输入**: components.schemas配置
- **输出**: 完整的DTO实体类
- **详细规则**:
  - 注解：@Data、@Schema
  - 校验注解：@NotBlank/@NotNull、@Size、@Email
  - 构造函数：无参构造、全参构造

### FR-5: API文档生成
- **描述**: 生成Markdown格式的API文档
- **输入**: 完整的JSON Schema配置
- **输出**: API接口文档

### FR-6: Mock服务器配置生成
- **描述**: 生成WireMock Mock服务器配置
- **输入**: responses配置
- **输出**: WireMock stub配置

### FR-7: Git自动推送
- **描述**: 将生成的代码自动提交并推送到Git仓库
- **输入**: git配置
- **输出**: 代码提交到指定分支

### FR-8: Maven自动发布
- **描述**: 将生成的API包自动发布到Maven仓库
- **输入**: gav配置
- **输出**: 发布到指定Maven仓库

## 4. 非功能需求 (NFR)

### NFR-1: 技术栈
- Java 11+
- Spring Boot 2.7.x
- Maven 3.8.x+
- Spring Cloud OpenFeign 3.1.x

### NFR-2: 性能要求
- 代码生成时间 < 5秒（单个配置文件）
- 支持多配置文件并发处理

### NFR-3: 兼容性
- 生成的代码可直接编译运行
- 兼容主流IDE（IntelliJ IDEA、Eclipse）

### NFR-4: 可维护性
- 代码结构清晰，模块化设计
- 完善的日志输出
- 详细的错误提示

## 5. 约束条件

### 5.1 技术约束
- 使用OpenAPI Parser 2.1.12解析JSON Schema
- 使用JavaPoet 1.13.0生成Java代码
- 使用Maven Invoker 3.1.0处理打包部署
- 使用JGit 6.1.0处理Git操作

### 5.2 业务约束
- 必须与现有Maven构建流程集成
- 支持私有Maven仓库（Nexus/Artifactory）

### 5.3 依赖约束
- spring-web: 5.3.20 (provided)
- validation-api: 2.0.1.Final (provided)
- spring-cloud-starter-openfeign: 3.1.3 (provided)

## 6. 验收标准 (AC)

### AC-1: DTO生成验收
- 输入：包含RegisterRequest schema的JSON配置
- 执行：mvn generate-sources
- 预期：生成RegisterRequest.java，包含@NotBlank、@Size、@Email注解

### AC-2: Controller生成验收
- 输入：包含/users/instance路径的JSON配置
- 执行：mvn generate-sources
- 预期：生成DemoEndpoint.java，包含@RestController、@PostMapping等注解

### AC-3: FeignClient生成验收
- 输入：完整的JSON配置
- 执行：mvn generate-sources
- 预期：生成DemoClient.java，方法签名与Controller一致

### AC-4: 完整流程验收
- 输入：完整的JSON配置文件
- 执行：mvn clean compile
- 预期：所有代码制品生成成功，可直接编译

## 7. 范围边界

### 7.1 包含范围
- JSON Schema配置解析
- Controller、Feign、DTO代码生成
- API文档生成
- Mock服务器配置生成
- Git自动推送
- Maven自动发布

### 7.2 不包含范围
- 运行时API网关功能
- API版本迁移工具
- 数据库访问层代码生成
- 前端代码生成
