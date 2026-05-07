# JSON Schema 配置规范文档

## 1. 文档信息
| 项目名称 | 版本 | 创建日期 | 作者 |
|---------|------|---------|------|
| API Generator Maven Plugin | 1.0.0 | 2026-05-07 | AI Team |

## 2. 配置文件概述

### 2.1 基本要求
- **协议版本**: JSON Schema Draft-07
- **文件扩展名**: .json
- **编码格式**: UTF-8
- **存放位置**: src/main/resources/api-spec/

### 2.2 顶层结构
```json
{
  "$schema": "http://json-schema.org/draft-07/schema#",
  "info": {},
  "git": {},
  "gav": {},
  "paths": {},
  "components": {}
}
```

**强制约束**: 五个根节点缺一不可，禁止新增或删减。

## 3. info 节点规范

### 3.1 字段定义
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| title | string | 是 | 服务名称，用于Swagger分组、FeignClient名称 |
| description | string | 是 | 服务描述 |
| version | string | 是 | API版本号，用于Maven版本、文档版本 |

### 3.2 示例
```json
"info": {
  "title": "Demo",
  "description": "API Demo Service",
  "version": "1.0.0"
}
```

## 4. git 节点规范

### 4.1 字段定义
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| url | string | 是 | Git仓库HTTPS/SSH地址 |
| branch | string | 是 | 推送目标分支 |
| token | string | 是 | Git授权Token |

### 4.2 示例
```json
"git": {
  "url": "https://github.com/demo/demo-api",
  "branch": "main",
  "token": "ghp_1234567890abcdef1234567890abcdef12345678"
}
```

### 4.3 行为说明
- 生成代码后自动执行：拉取 → 添加文件 → 提交 → 推送到指定分支
- 如目录不是Git仓库，自动初始化

## 5. gav 节点规范

### 5.1 字段定义
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| gav | string | 是 | Maven标准GAV坐标 groupId:artifactId:version |
| pkg | string | 是 | 生成Java代码根包名 |
| repository | array | 是 | Maven私服仓库列表 |

### 5.2 repository 元素定义
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| url | string | 是 | Maven仓库URL |

### 5.3 示例
```json
"gav": {
  "gav": "org.demo.cloud:demo-api:1.0.0-SNAPSHOT",
  "pkg": "org.demo.cloud",
  "repository": [
    {
      "url": "https://nexus.example.com/repository/maven-releases/"
    },
    {
      "url": "https://nexus.example.com/repository/maven-snapshots/"
    }
  ]
}
```

### 5.4 行为说明
- pkg为Java代码基础包，Controller、Feign、DTO均在此包下分层生成
- repository数组支持多个仓库地址，插件自动匹配快照/正式版本发布

## 6. paths 节点规范

### 6.1 结构概述
paths是一个Map，key为接口请求路径，value为路径配置对象。

### 6.2 路径配置对象
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| schema | object | 是 | 绑定的Controller类引用 |
| get/put/post/delete | object | 否 | HTTP方法配置 |

### 6.3 schema 字段
```json
"schema": {
  "$ref": "#/components/schemas/DemoEndpoint"
}
```
**作用**: 当前路径下所有HTTP方法，全部归属到指定Controller类。

### 6.4 HTTP方法配置对象
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| summary | string | 否 | 接口功能描述，用于@Operation注解 |
| parameters | array | 否 | 请求参数配置（GET/DELETE用） |
| request | object | 否 | 请求体配置（POST/PUT用） |
| responses | object | 是 | 响应配置 |

### 6.5 parameters 数组元素
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| name | string | 是 | 参数名 |
| required | boolean | 是 | 是否必填 |
| schema | object | 是 | 参数类型定义 |

### 6.6 request 对象
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| required | boolean | 是 | 请求体是否必填 |
| content | object | 是 | 请求内容 |

content对象结构：
```json
"content": {
  "application/json": {
    "schema": { "$ref": "#/components/schemas/RegisterRequest" }
  }
}
```

### 6.7 responses 对象
responses是一个Map，key为HTTP状态码，value为响应配置。

响应配置对象：
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| description | string | 是 | 响应描述 |
| content | object | 否 | 响应内容 |

### 6.8 完整示例
```json
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
}
```

## 7. components 节点规范

### 7.1 结构概述
```json
"components": {
  "schemas": {}
}
```

### 7.2 schemas 分类
schemas包含两类定义：
1. **Endpoint控制器类**: 无properties，仅作控制器标识
2. **DTO实体类**: 有properties，定义请求/响应模型

### 7.3 Endpoint控制器类定义
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 固定为"object" |
| description | string | 是 | 控制器描述，用于@Tag注解 |

**强制约束**: 禁止添加properties属性。

示例：
```json
"DemoEndpoint": {
  "type": "object",
  "description": "测试端点类"
}
```

### 7.4 DTO实体类定义
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 固定为"object" |
| description | string | 是 | 模型描述，用于@Schema注解 |
| properties | object | 是 | 属性定义 |
| required | array | 否 | 必填字段列表 |

### 7.5 properties 属性定义
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| type | string | 是 | 数据类型 |
| format | string | 否 | 格式约束 |
| minLength | integer | 否 | 字符串最小长度 |
| maxLength | integer | 否 | 字符串最大长度 |
| minimum | integer | 否 | 数值最小值 |
| maximum | integer | 否 | 数值最大值 |

### 7.6 类型映射规则
| JSON Schema类型 | Java类型 |
|----------------|---------|
| string | String |
| integer | Integer / Long (format=int64时) |
| number | Double / BigDecimal |
| boolean | Boolean |
| array | List&lt;T&gt; |
| object | 引用的DTO类 |

### 7.7 校验注解映射规则
| JSON Schema约束 | JSR-303注解 |
|----------------|------------|
| required数组包含 | @NotBlank (String) / @NotNull (Object) |
| minLength | @Size(min=xx) |
| maxLength | @Size(max=xx) |
| format=email | @Email |

### 7.8 DTO示例
```json
"RegisterRequest": {
  "type": "object",
  "description": "注册请求体",
  "properties": {
    "username": {
      "type": "string",
      "minLength": 3
    },
    "email": {
      "type": "string",
      "format": "email"
    },
    "password": {
      "type": "string",
      "minLength": 8
    }
  },
  "required": ["username", "email", "password"]
}
```

## 8. 完整配置示例

完整示例见 `doc.md` 中的示例配置。

## 9. 校验清单

使用本规范前，请确认：
- [ ] 协议版本为draft-07
- [ ] 包含所有5个根节点
- [ ] info节点包含title、description、version
- [ ] git节点包含url、branch、token
- [ ] gav节点包含gav、pkg、repository
- [ ] 所有路径都绑定了schema
- [ ] Endpoint类没有properties
- [ ] 所有DTO引用都在components.schemas中定义
- [ ] HTTP方法仅限get/put/post/delete

## 10. Lock文件机制

### 10.1 Lock文件概述
Lock文件用于记录代码生成状态，支持增量生成和变更检测。插件通过对比Lock文件与当前配置判断是否需要重新生成代码。

### 10.2 Lock文件位置
```
${project.basedir}/.api-generator.lock
```
Lock文件存放于项目根目录下，与pom.xml同级。

### 10.3 Lock文件格式
```json
{
  "generatedAt": "2026-05-07T10:30:00Z",
  "configMd5": "a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6",
  "generatedFiles": [
    "src/main/java/org/demo/cloud/controller/DemoController.java",
    "src/main/java/org/demo/cloud/feign/DemoFeignClient.java"
  ],
  "generatorVersion": "1.0.0"
}
```

### 10.4 Lock文件字段说明
| 字段 | 类型 | 说明 |
|------|------|------|
| generatedAt | string | 代码生成时间，ISO-8601格式 |
| configMd5 | string | 配置文件的MD5摘要值 |
| generatedFiles | array | 本次生成的所有文件路径列表 |
| generatorVersion | string | 生成器版本号 |

### 10.5 MD5计算规则
MD5计算基于配置文件内容，规则如下：
- 计算前移除所有空白字符（空格、制表符、换行符）
- 使用UTF-8编码计算MD5
- 计算结果转换为32位十六进制字符串

```java
String content = configJson.replaceAll("\\s+", "");
MessageDigest md = MessageDigest.getInstance("MD5");
byte[] digest = md.digest(content.getBytes(StandardCharsets.UTF_8));
String md5 = bytesToHex(digest);
```

### 10.6 增量生成策略
1. **首次运行**: 项目根目录不存在Lock文件时，执行全量生成
2. **配置变更检测**: 对比当前配置MD5与Lock文件中configMd5，不一致时触发重新生成
3. **文件对比**: 即使MD5一致，也会检查generatedFiles中的文件是否存在
4. **清理策略**: 重新生成时，删除generatedFiles中记录但本次未生成的文件

### 10.7 手动干预
如需强制全量生成，可删除项目根目录下的`.api-generator.lock`文件。

## 11. Maven仓库配置

### 11.1 settings.xml认证配置
当Maven私服需要认证时，需在`~/.m2/settings.xml`中配置服务器认证信息。

```xml
<settings>
  <servers>
    <server>
      <id>nexus-releases</id>
      <username>deployer</username>
      <password> encrypted_password_here </password>
    </server>
    <server>
      <id>nexus-snapshots</id>
      <username>deployer</username>
      <password> encrypted_password_here </password>
    </server>
  </servers>
</settings>
```

**说明**: settings.xml中的server id需与pom.xml或JSON配置中的repository id保持一致。

### 11.2 JSON配置中的认证字段
JSON配置中的repository节点支持认证配置：

```json
"repository": [
  {
    "id": "nexus-releases",
    "url": "https://nexus.example.com/repository/maven-releases/",
    "username": "deployer",
    "password": "password123"
  },
  {
    "id": "nexus-snapshots",
    "url": "https://nexus.example.com/repository/maven-snapshots/",
    "username": "deployer",
    "password": "password123"
  }
]
```

### 11.3 repository节点扩展字段
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | string | 是 | 仓库唯一标识，需与settings.xml中server id对应 |
| url | string | 是 | 仓库地址 |
| username | string | 否 | 用户名（优先使用settings.xml配置） |
| password | string | 否 | 密码（优先使用settings.xml配置） |
| releasesEnabled | boolean | 否 | 是否启用正式版本，默认为true |
| snapshotsEnabled | boolean | 否 | 是否启用快照版本，默认为true |

### 11.4 多仓库支持
插件支持配置多个Maven仓库地址，发布时会根据版本号自动选择对应仓库：

```json
"repository": [
  {
    "id": "internal-nexus",
    "url": "https://nexus.internal.com/repository/maven-releases/",
    "releasesEnabled": true,
    "snapshotsEnabled": false
  },
  {
    "id": "internal-nexus-snapshots",
    "url": "https://nexus.internal.com/repository/maven-snapshots/",
    "releasesEnabled": false,
    "snapshotsEnabled": true
  },
  {
    "id": "public-maven-central",
    "url": "https://repo.maven.apache.org/maven2/",
    "releasesEnabled": true,
    "snapshotsEnabled": false
  }
]
```

### 11.5 仓库优先级
当配置多个仓库时，插件按以下顺序查找可用仓库：
1. 匹配版本类型的仓库（releases或snapshots）
2. 按配置顺序依次尝试
3. 确保至少有一个仓库可用

## 12. GitHub集成配置

### 12.1 token权限要求
GitHub Personal Access Token (PAT) 需要具备以下权限：

| 权限 | 最低要求 | 说明 |
|------|---------|------|
| repo | Full control | 需要读写仓库内容 |
| workflow | Not required | 不需要工作流权限 |

**推荐权限配置**:
- 仅需repo权限，不需workflow
- 建议创建专用Token用于API生成，避免使用个人主Token
- Token格式: `ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 12.2 仓库可见性设置
插件支持三种仓库可见性：

```json
"git": {
  "url": "https://github.com/demo/demo-api",
  "branch": "main",
  "token": "ghp_xxxx",
  "visibility": "private"
}
```

| visibility值 | 说明 | 适用场景 |
|--------------|------|---------|
| public | 公开仓库 | 开源项目 |
| private | 私有仓库 | 企业内部项目 |
| internal | 内部可见 | GitHub Enterprise组织内部 |

### 12.3 分支配置
```json
"git": {
  "url": "https://github.com/demo/demo-api",
  "branch": "feature/api-generation",
  "token": "ghp_xxxx",
  "createIfNotExists": true
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| branch | string | 是 | 目标分支名称 |
| createIfNotExists | boolean | 否 | 分支不存在时是否创建，默认为false |

### 12.4 分支管理说明
- **现有分支**: 直接推送代码到指定分支
- **新建分支**: 设置`createIfNotExists: true`时，如分支不存在则从默认分支创建
- **分支保护**: 推送前需确保有分支推送权限

### 12.5 GitHub配置完整示例
```json
"git": {
  "url": "https://github.com/demo/demo-api",
  "branch": "main",
  "token": "ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx",
  "visibility": "private",
  "createIfNotExists": false,
  "commitMessage": "chore: auto-generate API code",
  "author": {
    "name": "API Generator",
    "email": "generator@example.com"
  }
}
```

### 12.6 commitMessage与author字段
| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| commitMessage | string | 否 | 提交信息模板，默认为"chore: auto-generate API code" |
| author | object | 否 | 提交作者信息 |
| author.name | string | 否 | 作者名称 |
| author.email | string | 否 | 作者邮箱 |

### 12.7 GitHub Enterprise支持
插件同时支持GitHub Enterprise自建实例：

```json
"git": {
  "url": "https://github.mycompany.com/demo/demo-api",
  "branch": "main",
  "token": "ghp_xxxx",
  "apiUrl": "https://github.mycompany.com/api/v3"
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| apiUrl | string | 否 | GitHub Enterprise API地址，默认使用官方api.github.com |
