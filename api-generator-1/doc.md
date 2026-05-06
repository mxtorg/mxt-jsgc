# 需求概述
- **Summary**: 一个基于JSON Schema的Maven插件，可自动生成Spring Boot Controller桩代码、OpenFeign客户端、DTO类（含JSR-303校验注解）、API文档、Mock服务器配置等多种制品。
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


请基于以上描述，帮我梳理出详细的开发说明文档，以step by step 方式梳理出以spec 编程规范的一系列文档，逻辑清晰，ai编码工具可执行

# 基于 JSON Schema Maven 代码生成插件 开发规范 & 使用文档（Spec 编程规范・Step-by-Step 版）
文档概述
本文档定义JSON Schema 接口描述规范、Maven 插件解析规则、代码生成规则、目录结构、字段约束、注解规范、制品输出规则，可直接供 AI 编码工具、插件开发、业务接入标准化执行。插件核心能力：基于标准 JSON Schema 配置，一键自动生成：
Spring Boot Rest Controller 桩代码
OpenFeign 客户端接口
带 JSR-303 校验注解的 DTO/Request/Response 实体类
Swagger/OpenAPI 接口文档注解
Mock 服务配置、Git 自动推送、Maven 仓库发布配置
一、整体使用流程 Step-by-Step
Step 1 新建标准 JSON Schema 配置文件
项目资源目录下新建 api-spec/*.json 规范配置文件
必须遵循 draft-07 协议头：
json
"$schema": "http://json-schema.org/draft-07/schema#"
固定顶层结构：info、git、gav、paths、components 五大根节点，缺一不可。
Step 2 填充基础元信息配置（info + git + gav）
按固定格式填写服务基础信息、Git 仓库、Maven 坐标，插件用于代码打包、推送、发布。
Step 3 编写 paths 接口路由定义
定义 HTTP 请求路径（如 /users/instance）
绑定对应 Endpoint 控制器类引用 $ref
配置该路径下 get/post/put/delete 任意 HTTP 方法
每个方法配置：summary 简介、请求体、路径参数、响应体
Step 4 编写 components 通用模型定义
在 components.schemas 中统一定义：
Endpoint 控制器空实体类
Request 请求 DTO
Response 响应 DTO
全局可复用数据模型
Step 5 Maven 插件引入 & 执行
父 POM 或模块 POM 引入本代码生成 Maven 插件
绑定 generate-code 生命周期
执行 mvn compile / mvn generate-sources 自动生成所有代码制品
Step 6 自动制品输出 & 自动推送发布
生成代码输出到指定源码目录
自动生成 Swagger 注解、JSR303 校验注解
按 git 配置自动提交推送到指定仓库分支
按 gav 配置自动打包发布到私有 Maven 仓库
二、JSON Schema 顶层节点规范（强制约束）
2.1 根节点固定结构
```json
{
  "$schema": "draft-07协议地址",
  "info": {},
  "git": {},
  "gav": {},
  "paths": {},
  "components": {}
}
```

约束：
禁止新增根节点、禁止删减五大根节点
协议版本固定 draft-07，不可修改
2.2 info 节点规范（服务元信息）
字段定义
表格
字段	类型	是否必填	说明
title	string	是	服务名称，用于 Swagger 分组、FeignClient 名称
description	string	是	服务描述
version	string	是	API 版本号，用于 Maven 版本、文档版本
示例
```json
"info":{
  "title": "Demo",
  "description": "API Demo Service",
  "version": "1.0.0"
}
```
2.3 git 节点规范（代码自动推送配置）
字段定义
```table
表格
字段	类型	是否必填	说明
url	string	是	Git 仓库 HTTPS/SSH 地址
branch	string	是	推送目标分支
token	string	是	Git 授权 Token，用于自动提交推送
```
约束
生成代码后自动执行：拉取→新增文件→提交→推送到指定分支
2.4 gav 节点规范（Maven 坐标 & 仓库发布）
字段定义
表格
字段	类型	是否必填	说明
gav	string	是	Maven 标准 GAV 坐标 groupId:artifactId:version
pkg	string	是	生成 Java 代码根包名
repository	array	是	Maven 私服仓库列表，支持快照 / 正式库
约束
pkg 为 Java 代码基础包，Controller、Feign、DTO 均在此包下分层生成
repository 数组支持多个仓库地址，插件自动匹配快照 / 正式版本发布
2.5 paths 节点规范（API 路由 & HTTP 方法定义）
结构规则
key 为接口请求路径：如 /users/instance
每个路径下必须有 schema 字段，绑定对应 Controller Endpoint 类
路径下可包含一个或多个 HTTP 方法：get/post/put/delete
每个 HTTP 方法固定结构：summary、parameters、request、responses
2.5.1 schema 绑定规则
json
"schema": { "$ref": "#/components/schemas/DemoEndpoint" }
作用：当前路径下所有 HTTP 方法，全部归属到 DemoEndpoint 控制器类
规则：一个路径对应一个 Endpoint 类，同路径下多 HTTP 方法生成同一个类的多个方法
2.5.2 HTTP 公共字段规范
summary
作用：生成 @Operation(summary = "xxx") Swagger 注解
约束：非空则生成注解，为空则不生成
文案作为接口功能描述
parameters
适用：GET 请求、路径传参、请求头参数
结构：name参数名、required是否必传、schema参数类型
生成注解：@RequestParam
request
适用：POST/PUT 提交请求体
required：是否必传
content.application/json.schema：引用 components 中的 Request 模型
生成注解：@RequestBody
responses
key 为 HTTP 状态码：200/201/400/500
description：响应描述
content.application/json.schema：引用 components 中的 Response 模型
作为接口出参类型
2.6 components.schemas 模型规范（DTO/Endpoint 统一定义）
分类 1：Endpoint 控制器类
json
"DemoEndpoint":{
  "type": "object",
  "description":"测试端点类(没有properties属性)"
}
强制约束：
无 properties 属性
仅做控制器标识，不生成业务字段
description 用于生成类上 @Tag 分组注解
分类 2：Request/Response DTO 实体类
通用结构
json
"模型名": {
  "type": "object",
  "description": "模型描述",
  "properties": {},
  "required": []
}
字段约束规则
type：string/integer/boolean
format：email、int64 等格式校验
minLength：字符串最小长度，自动生成 @Size JSR303 注解
required：数组，声明必填字段，自动生成 @NotBlank/@NotNull 校验注解
示例规范
json
"RegisterRequest": {
  "type": "object",
  "description":"注册请求体",
  "properties": {
    "username": { "type": "string", "minLength": 3 },
    "email": { "type": "string", "format": "email" },
    "password": { "type": "string", "minLength": 8 }
  },
  "required": ["username", "email", "password"]
}
三、Java 代码生成规则 Step-by-Step
3.1 Controller 类生成规则
1. 类注解固定生成
java
运行
@RestController
@Tag(name = "xxx端点") // 取自schema的description
@Slf4j
public class XxxEndpoint {}
2. 方法命名规则
规则：http方法 + 路径驼峰化示例：
路径 /users/instance + post → postUserInstance()
路径 /users/instance + put → putUserInstance()
路径 /users/instance + get → getUserInstance()
3. 映射注解规则
POST → @PostMapping("路径")
PUT → @PutMapping("路径")
GET → @GetMapping("路径")
DELETE → @DeleteMapping("路径")
4. 入参注解规则
POST/PUT 请求体：@RequestBody 实体类
GET 路径参数：@RequestParam("参数名") 类型 参数名
5. Swagger 注解规则
方法有 summary：生成 @Operation(summary = "xxx")
类 description：生成 @Tag(name = "xxx")
6. 方法体模板
固定生成 //todo 业务逻辑 + 日志打印 + 默认返回值桩代码。
3.2 OpenFeign 客户端生成规则
1. 类注解
java
运行
@FeignClient(
    name = "info.title值",
    url = "${api.xxx.url:}"
)
public interface XxxClient {}
2. 方法签名规则
和 Controller方法名、入参、出参、注解完全一致
复用同路径、同 HTTP 方法的接口定义
3.3 DTO 实体类生成规则
自动生成：私有字段、getter/setter、无参构造、全参构造
自动适配驼峰命名
自动生成 JSR-303 校验注解：
required 字段 → @NotBlank / @NotNull
minLength → @Size(min = xx)
format=email → @Email
自动加入 Swagger @Schema 字段注释
四、插件接入 & 执行规范（AI 可直接落地）
Step 1 引入 Maven 插件到 POM
xml
<plugin>
    <groupId>org.demo.plugin</groupId>
    <artifactId>json-schema-codegen-maven-plugin</artifactId>
    <version>1.0.0</version>
    <executions>
        <execution>
            <goals>
                <goal>generate-api-code</goal>
            </goals>
        </execution>
    </executions>
    <configuration>
        <specDir>src/main/resources/api-spec</specDir>
        <outputJavaDir>src/main/java</outputJavaDir>
    </configuration>
</plugin>
Step 2 目录规范
plaintext
src/
├── main/
│   ├── resources/
│   │   └── api-spec/       # 存放所有JSON Schema规范文件
│   └── java/
│       └── 生成代码根包/    # 按gav.pkg自动分层：controller/dto/feign
Step 3 执行命令
bash
运行
mvn clean generate-sources
# 或
mvn compile
自动完成：代码生成 → 格式化 → Git 提交推送 → Maven 仓库发布。
五、禁止 & 强制约束清单（AI 编码校验规则）
禁止删减 info/git/gav/paths/components 任一顶层节点
Endpoint 类禁止加 properties字段，仅做控制器标识
HTTP 方法只允许 get/post/put/delete，其他方法不解析
所有 DTO 必须在 components.schemas 定义，禁止 paths 内直接定义模型
方法命名必须严格遵循 http方法+路径驼峰 规则，禁止自定义方法名
必传字段必须写入 required 数组，否则不生成 JSR303 必填校验
Feign 接口方法签名必须与 Controller 完全一致，禁止参数差异
六、AI 编码工具执行指令模板
你可以直接复制给 AI 执行：
严格按照本文档 Step-by-Step 规范，解析给定 JSON Schema 配置文件；遵循路径绑定 Endpoint 类、方法命名规则、Swagger 注解规则、JSR303 校验规则；自动生成标准 Spring Boot Controller、Feign 客户端、带校验注解的 DTO 实体；严格遵守目录规范、注解规范、命名规范，输出可直接编译运行的 Java 代码。
我可以帮你：
基于你现有的 Demo JSON Schema，一键生成完整 Controller+Feign+DTO 成品 Java 代码
帮你封装一份标准空白模板 JSON Schema，后续直接填空使用
需要我直接生成成品代码吗？
基于文档，完成代码开发
基于 JSON Schema Spec 规范 完整代码生成
按你给定的 JSON Schema、生成规则，一次性输出：
DTO 实体类（带 JSR303 + Swagger 注解）
DemoEndpoint 控制器完整代码
OpenFeign 客户端接口
严格匹配命名、注解、请求方式、入参出参规则
一、基础包路径
根据 gav.pkg：org.demo.cloud分层结构：
dto：请求 / 响应实体
controller：接口控制器
feign：远程调用客户端
二、DTO 实体类代码
1. RegisterRequest.java
java
运行
package org.demo.cloud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "注册请求体")
public class RegisterRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, message = "用户名长度不能少于3位")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码长度不能少于8位")
    @Schema(description = "密码")
    private String password;
}
2. UserUpdateRequest.java
java
运行
package org.demo.cloud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "用户更新请求体")
public class UserUpdateRequest {

    @NotBlank(message = "用户名不能为空")
    @Size(min = 3, message = "用户名长度不能少于3位")
    @Schema(description = "用户名")
    private String username;

    @NotBlank(message = "邮箱不能为空")
    @Email(message = "邮箱格式不正确")
    @Schema(description = "邮箱")
    private String email;

    @NotBlank(message = "密码不能为空")
    @Size(min = 8, message = "密码长度不能少于8位")
    @Schema(description = "密码")
    private String password;
}
3. UserResponse.java
java
运行
package org.demo.cloud.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "用户响应体")
public class UserResponse {

    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "返回消息")
    private String message;
}
三、Controller 控制器 DemoEndpoint.java
严格遵循：路径驼峰方法名、@Operation 注解、请求体 / 请求参数注解、日志桩代码
java
运行
package org.demo.cloud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.demo.cloud.dto.RegisterRequest;
import org.demo.cloud.dto.UserResponse;
import org.demo.cloud.dto.UserUpdateRequest;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@Tag(name = "Demo端点", description = "测试端点类")
@RequestMapping
public class DemoEndpoint {

    @PostMapping("/users/instance")
    @Operation(summary = "Register a new user")
    public Long postUserInstance(@RequestBody RegisterRequest request) {
        // todo 实现注册逻辑
        log.info("用户注册请求入参：{}", request);
        return 1L;
    }

    @PutMapping("/users/instance")
    @Operation(summary = "Upate a user")
    public Boolean putUserInstance(@RequestBody UserUpdateRequest request) {
        // todo 实现更新逻辑
        log.info("用户更新请求入参：{}", request);
        return true;
    }

    @GetMapping("/users/instance")
    @Operation(summary = "Get user by ID")
    public UserResponse getUserInstance(@RequestParam("id") Long id) {
        // todo 实现根据ID获取用户逻辑
        log.info "查询用户ID：{}", id;
        return new UserResponse(id, "User fetched successfully");
    }
}
四、OpenFeign 客户端 DemoClient.java
遵循规则：FeignClient 名称取自 info.title、方法签名与 Controller 完全一致
java
运行
package org.demo.cloud.feign;

import org.demo.cloud.dto.RegisterRequest;
import org.demo.cloud.dto.UserResponse;
import org.demo.cloud.dto.UserUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(
        name = "Demo",
        url = "${api.Demo.url:}"
)
public interface DemoClient {

    @PostMapping("/users/instance")
    Long postUserInstance(@RequestBody RegisterRequest requestBody);

    @PutMapping("/users/instance")
    Boolean putUserInstance(@RequestBody UserUpdateRequest requestBody);

    @GetMapping("/users/instance")
    UserResponse getUserInstance(@RequestParam("id") Long id);
}
五、代码合规校验说明（完全符合你定义的 Spec 规范）
✅ DTO 自动识别 required/minLength/email 生成 JSR303 校验注解
✅ Controller 类绑定 DemoEndpoint、类上 @Tag 注解取自描述
✅ 方法名严格遵循：http 方法 + 路径驼峰 → postUserInstance /putUserInstance/getUserInstance
✅ POST/PUT 自动加 @RequestBody，GET 路径参数自动加 @RequestParam
✅ summary 非空自动生成 @Operation 接口文档注解
✅ Feign 接口方法签名、请求注解、路径与 Controller 完全对齐
✅ 包结构严格遵循 gav.pkg 根路径分层