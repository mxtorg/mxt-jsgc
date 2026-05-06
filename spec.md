# API Generator Maven Plugin - Product Requirement Document

## Overview

- **Summary**: 一个基于JSON Schema的Maven插件，使用javapoet，生成Spring Boot的 Controller桩代码、OpenFeign客户端、DTO类（含JSR-303校验注解）、API文档、Mock服务器配置等多种制品。
  ```json
  {
    "$schema": "http://json-schema.org/draft-07/schema#",
    "info":{
      "title": "Demo",
      "description": "API Demo Service",
      "version": "1.0.0"
    },
    "git":{
      "url":"https://github.com/demo/demo-api",
      "branch":"main",
      "token":"ghp_1234567890abcdef1234567890abcdef12345678"
    },  
    "gav":{
      "gav":"org.demo.cloud:demo-api:1.0.0-SNAPSHOT",
      "pkg":"org.demo.cloud",
      "repository":[{
        "url":"https://nexus.example.com/repository/maven-releases/"
      },{
        "url":"https://nexus.example.com/repository/maven-snapshots/"
      }]
    },  
    "paths": {
      "/users/instance": {
        "schema": { "$ref": "#/components/schemas/DemoEndpoint" },
        "post": {
          "summary": "Register a new user",
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
                  "parameters": [
                    {
                      "name": "id",
                      "schema": {
                        "type": "integer",
                        "format": "int64"
                      }
                    }
                  ]
                }
              }
            }
          }
        },
        "put": {
          "summary": "Upate a user",
          "request": {
            "required": true,
            "content": {
              "application/json": {
                "schema": { "$ref": "#/components/schemas/UserUpdateRequest" }
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
        },
        "get": {
          "summary": "Get user by ID",
          "parameters": [
            {
              "name": "id",
              "required": true,
              "schema": {
                "type": "integer",
                "format": "int64"
              }
            }
          ],
          "responses": {
            "200": {
              "description": "User found",
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
      "schemas": {
        "DemoEndpoint":{
          "type": "object",
          "description":"测试端点类(没有properties属性)"
        },
        "RegisterRequest": {
          "type": "object",
          "description":"注册请求体",
          "properties": {
            "username": { "type": "string", "minLength": 3 },
            "email": { "type": "string", "format": "email" },
            "password": { "type": "string", "minLength": 8 }
          },
          "required": ["username", "email", "password"]
        },
        "UserUpdateRequest": {
          "type": "object",
          "description":"注册请求体",
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
  ```
  其中：

  ***git***：用于指定API项目定义的存储位置（如Git仓库）。生成的代码将自动推送到该仓库
  
  ***gav***：用于指定该API发布包的Maven坐标（GAV）以及发布到的mvn仓库

  ***path***：用户指定API路径，用于生成对应的Controller方法和Feign客户端接口的方法签名及swagger接口文档注解，生成规则为:
    - ***类+方法签名规则***：路径中的schema对应一个Controller类，如：/users/instance路径下的"schema": { "$ref": "#/components/schemas/DemoEndpoint" }, 表示该路径对应的方法签名在 DemoEndpoint 类中（DemoEndpoint 在components/schemas/DemoEndpoint.json中被定义）, 每个路径下存在一个或多个rest 规范的http方法对象（post|put|delete|get）且这些路径对应的方法签名都映射到该schema 指向的Controller或Endpoint类，且方法名为：httpMthod+路径，如：/users/instance 中的post方法对应Endpoint类中的postUserInstance方法，put方法对应putUserInstance方法等。
    - ***注解规则***：路径中每个http方法对象（post|put|delete|get）下的summary用于生成swagger文档注解，如果summary不为空，则生成注解，如果为空，则不增加注解，如：/users/instance 中的post方法对应postUserInstance方法，并在该方法上增加@Operation(summary = "Register a new user")注解，其他同理。
    - ***出入参规则***：路径中的resquest 和response 分别代表该路径对应方法的入参和出参，入参需遵守spring+rest规范，如post|put 需增加@RequestBody 注解，且方法的出入参为components/schemas/下定义的schema对象（如参数为对象类型时）。
    - ***生成Feign客户端接口规则***：每个Controller方法对应一个Feign客户端接口方法，方法签名与Controller方法签名相同。
      ***示例如下***：
      ```java
      // Controller 类
      @RestController
      @Tag(name = "Deme端点")
      @Slf4j
      public class DemoEndpoint{
        @Operation(summary = "Register a new user")
        @PostMapping("/users/instance")
        public Long postUserInstance(@RequestBody RegisterRequest request) {
          //todo  实现注册逻辑
          log.info("User registered successfully");
          return 1L;
        }
        
        @Operation(summary = "Upate a user")  
        @PutMapping("/users/instance")
        public Boolean putUserInstance(@RequestBody UserUpdateRequest request) {
          // todo 实现更新逻辑
          return true;
        }

        @Operation(summary = "Get a user")  
        @GetMapping("/users/instance")
        public UserResponse getUserInstance(@RequestParam("id") String id) {
          // 实现获取逻辑
          return new UserResponse(1, "User fetched successfully");
        }
      }


      // Feign或RPC客户端接口
      @FeignClient(
          name = "Demo",
          url = "${api.Demo.url:}"
      )
      public interface DemoClient {
        @PostMapping("/users/register")
        Long postUserInstance(@RequestBody RegisterRequest requestBody);

        @GetMapping("/users/instance")
        UserResponse getUserById(@RequestParam("id") String id);
      }
      
      ```
  ***components***：参考openapi，定义了API的组件，如请求体、响应体、Controller或Endpoint类等。用于在API定义中引用(可复用)。



- **Purpose**: 将API契约定义转化为可直接使用的代码和配置，消除手工编写DTO和Feign接口的重复劳动，实现契约驱动开发。
- **Target Users**: Java后端开发人员、架构师、DevOps工程师

## Goals

- 实现基于JSON Schema的双向代码生成（服务端Controller + 客户端Feign）
- 自动生成带JSR-303校验注解的DTO类
- 生成API文档（Markdown/Swagger UI配置）
- 生成WireMock Mock服务器配置
- 支持API发布包的Maven打包与部署
- 实现契约校验能力

## Non-Goals (Out of Scope)

- 不提供运行时API网关功能
- 不实现API版本迁移工具
- 不包含数据库访问层代码生成
- 不提供前端代码生成

## Background & Context

- 基于OpenAPI Parser解析扩展JSON Schema
- 使用JavaPoet生成Java源码
- 通过Maven Invoker完成打包部署
- 支持私有Nexus/Artifactory仓库集成

## Functional Requirements

- **FR-1**: 解析扩展JSON Schema（含GAV、repository、pkg字段）
- **FR-2**: 生成Spring Boot @RestController桩代码（支持@RequestBody、@PathVariable、@RequestParam）
- **FR-3**: 生成@FeignClient客户端接口（方法签名与Controller一致）
- **FR-4**: 生成带JSR-303校验注解的DTO类（@NotNull、@Size、@Email等）
- **FR-5**: 生成Markdown格式API文档
- **FR-6**: 生成Swagger UI配置（application.yml片段）
- **FR-7**: 生成WireMock Mock规则配置
- **FR-8**: 生成Camunda DMN集成配置
- **FR-9**: 生成Drools规则文件
- **FR-10**: 打包并部署API发布包到指定Maven仓库

## Non-Functional Requirements

- **NFR-1**: 支持Java 11及以上版本
- **NFR-2**: 生成的代码符合Spring Boot 2.7.x规范
- **NFR-3**: 支持Maven 3.8.x及以上版本
- **NFR-4**: 生成代码需兼容Spring Cloud OpenFeign 3.1.x

## Constraints

- **Technical**: 使用OpenAPI Parser 2.1.12、JavaPoet 1.13.0、Maven Invoker 3.1.0
- **Business**: 需与现有Maven构建流程集成
- **Dependencies**: 依赖spring-web、validation-api等库（provided scope）

## Assumptions

- 用户已安装Maven并配置MAVEN\_HOME环境变量
- 目标仓库已正确配置认证信息
- 输入的JSON Schema符合OpenAPI 3.0规范

## Acceptance Criteria

### AC-1: DTO生成

- **Given**: 输入包含RegisterRequest schema的JSON配置
- **When**: 执行插件generate目标
- **Then**: 生成带@NotNull、@Size、@Email注解的RegisterRequest.java类
- **Verification**: `programmatic`

### AC-2: Controller生成

- **Given**: 输入包含/users/register路径的JSON配置
- **When**: 执行插件generate目标
- **Then**: 生成带@RestController和@PostMapping注解的Controller类
- **Verification**: `programmatic`

### AC-3: FeignClient生成

- **Given**: 输入包含operationId的JSON配置
- **When**: 执行插件generate目标
- **Then**: 生成带@FeignClient注解的客户端接口，方法签名与Controller一致
- **Verification**: `programmatic`

### AC-4: API文档生成

- **Given**: 输入完整的JSON配置
- **When**: 执行插件generate目标
- **Then**: 生成包含Endpoints表格的Markdown文档
- **Verification**: `human-judgment`

### AC-5: Mock服务器配置生成

- **Given**: 输入包含responses的JSON配置
- **When**: 执行插件generate目标
- **Then**: 生成WireMockStub配置类
- **Verification**: `programmatic`

### AC-6: API包部署

- **Given**: 提供GAV和repository配置
- **When**: 执行插件generate目标（skipDeploy=false）
- **Then**: 生成pom.xml并部署到指定仓库
- **Verification**: `programmatic`

### AC-7: Camunda集成生成

- **Given**: 输入包含schemas的JSON配置
- **When**: 执行插件generate目标
- **Then**: 生成DMN文件，包含itemDefinition定义
- **Verification**: `programmatic`

### AC-8: Drools规则生成

- **Given**: 输入包含schemas的JSON配置
- **When**: 执行插件generate目标
- **Then**: 生成DRL文件，包含事实类型声明
- **Verification**: `programmatic`

## Open Questions

- [ ] 是否需要支持OpenAPI 3.1规范？
- [ ] 是否需要支持其他Mock框架（如MockServer）？
- [ ] 是否需要提供命令行独立运行模式？

