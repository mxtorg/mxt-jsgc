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
