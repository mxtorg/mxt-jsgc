# API Generator Maven Plugin - Implementation Plan

## [x] Task 1: 创建Maven插件项目结构
- **Priority**: P0
- **Depends On**: None
- **Description**: 
  - 创建插件项目目录结构
  - 创建插件主pom.xml配置
  - 配置Maven插件开发所需依赖
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8]
- **Test Requirements**:
  - `programmatic` TR-1.1: 项目能成功编译
  - `programmatic` TR-1.2: pom.xml包含所有必要依赖（swagger-parser, javapoet, maven-invoker）

## [x] Task 2: 实现辅助工具类
- **Priority**: P0
- **Depends On**: Task 1
- **Description**: 
  - 实现TypeMapper工具类（JSON Schema类型到Java类型的映射）
  - 实现NamingUtil工具类（命名规范处理）
  - 实现OpenApiUtil工具类（OpenAPI解析辅助方法）
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3]
- **Test Requirements**:
  - `programmatic` TR-2.1: TypeMapper能正确映射string/email到String类型
  - `programmatic` TR-2.2: TypeMapper能正确映射integer到int/long类型
  - `programmatic` TR-2.3: TypeMapper能正确映射array到List类型

## [x] Task 3: 实现DTO生成器（含校验注解）
- **Priority**: P0
- **Depends On**: Task 2
- **Description**: 
  - 实现DtoGenerator类
  - 支持从OpenAPI schemas生成Java POJO
  - 添加JSR-303校验注解（@NotNull, @Size, @Email等）
  - 生成getter/setter方法
- **Acceptance Criteria Addressed**: [AC-1]
- **Test Requirements**:
  - `programmatic` TR-3.1: 生成的DTO类包含@JsonIgnoreProperties注解
  - `programmatic` TR-3.2: required字段包含@NotNull注解
  - `programmatic` TR-3.3: string类型带minLength约束包含@Size注解
  - `programmatic` TR-3.4: email格式字段包含@Email注解

## [x] Task 4: 实现Controller生成器
- **Priority**: P0
- **Depends On**: Task 2
- **Description**: 
  - 实现ControllerGenerator类
  - 支持生成@RestController类
  - 支持@GetMapping/@PostMapping/@PutMapping/@DeleteMapping注解
  - 支持@PathVariable、@RequestParam、@RequestBody参数
- **Acceptance Criteria Addressed**: [AC-2]
- **Test Requirements**:
  - `programmatic` TR-4.1: 生成的类包含@RestController注解
  - `programmatic` TR-4.2: POST路径生成@PostMapping注解
  - `programmatic` TR-4.3: 路径参数生成@PathVariable注解
  - `programmatic` TR-4.4: 请求体参数生成@RequestBody注解

## [x] Task 5: 实现FeignClient生成器
- **Priority**: P0
- **Depends On**: Task 2
- **Description**: 
  - 实现FeignClientGenerator类
  - 支持生成@FeignClient接口
  - 方法签名与Controller完全一致
  - 支持URL配置占位符
- **Acceptance Criteria Addressed**: [AC-3]
- **Test Requirements**:
  - `programmatic` TR-5.1: 生成的接口包含@FeignClient注解
  - `programmatic` TR-5.2: 接口方法包含正确的HTTP映射注解
  - `programmatic` TR-5.3: 返回类型为ResponseEntity<Object>

## [x] Task 6: 实现文档和配置生成器
- **Priority**: P1
- **Depends On**: Task 1
- **Description**: 
  - 实现MarkdownDocGenerator（生成API文档）
  - 实现SwaggerUIConfigGenerator（生成application-swagger.yml）
- **Acceptance Criteria Addressed**: [AC-4]
- **Test Requirements**:
  - `programmatic` TR-6.1: 生成api-docs.md文件
  - `programmatic` TR-6.2: 生成application-swagger.yml文件
  - `human-judgment` TR-6.3: 文档包含Endpoints表格

## [x] Task 7: 实现Mock和规则引擎生成器
- **Priority**: P1
- **Depends On**: Task 1
- **Description**: 
  - 实现WireMockStubGenerator（生成WireMock配置类）
  - 实现CamundaDmnGenerator（生成DMN文件）
  - 实现DroolsRuleGenerator（生成DRL文件）
- **Acceptance Criteria Addressed**: [AC-5, AC-7, AC-8]
- **Test Requirements**:
  - `programmatic` TR-7.1: 生成WireMockStubs.java类
  - `programmatic` TR-7.2: 生成api-model.dmn文件
  - `programmatic` TR-7.3: 生成api-rules.drl文件

## [x] Task 8: 实现API打包与部署
- **Priority**: P1
- **Depends On**: Task 3, Task 4, Task 5
- **Description**: 
  - 实现ApiPackager类
  - 生成Maven模块pom.xml
  - 使用Maven Invoker执行clean deploy
- **Acceptance Criteria Addressed**: [AC-6]
- **Test Requirements**:
  - `programmatic` TR-8.1: 生成pom.xml文件
  - `programmatic` TR-8.2: 复制原始API规范到resources目录
  - `programmatic` TR-8.3: 能调用Maven命令执行打包

## [x] Task 9: 实现Mojo入口类
- **Priority**: P0
- **Depends On**: Task 3, Task 4, Task 5, Task 6, Task 7, Task 8
- **Description**: 
  - 实现ApiGeneratorMojo类
  - 配置插件参数（apiSpecFile, outputDir, basePackage等）
  - 编排所有生成器的执行顺序
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8]
- **Test Requirements**:
  - `programmatic` TR-9.1: Mojo类包含@Mojo注解
  - `programmatic` TR-9.2: 能正确解析JSON配置文件
  - `programmatic` TR-9.3: 按正确顺序调用所有生成器

## [x] Task 10: 集成测试与验证
- **Priority**: P2
- **Depends On**: Task 9
- **Description**: 
  - 创建测试用JSON配置文件
  - 运行插件验证所有生成器功能
  - 检查生成的代码正确性
- **Acceptance Criteria Addressed**: [AC-1, AC-2, AC-3, AC-4, AC-5, AC-6, AC-7, AC-8]
- **Test Requirements**:
  - `programmatic` TR-10.1: 插件能成功执行generate目标
  - `programmatic` TR-10.2: 所有预期文件正确生成
  - `human-judgment` TR-10.3: 生成代码格式符合Java规范