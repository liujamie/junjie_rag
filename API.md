# RAG AI 知识库系统 - API 接口文档

## 📋 目录

- [基本信息](#基本信息)
- [用户管理接口](#一用户管理接口)
- [AI 对话接口](#二 ai 对话接口)
- [知识库管理接口](#三知识库管理接口)
- [图像生成接口](#四图像生成接口)
- [敏感词管理接口](#五敏感词管理接口)
- [敏感词分类接口](#六敏感词分类接口)
- [分词统计接口](#七分词统计接口)
- [日志管理接口](#八日志管理接口)
- [认证说明](#认证说明)
- [通用响应格式](#通用响应格式)

---

## 基本信息

| 项目 | 值 |
|------|-----|
| **API 版本** | `/api/v1` |
| **应用名称** | xs-rag-ai |
| **基础框架** | Spring Boot + Spring AI |
| **认证方式** | JWT Token |

---

## 一、用户管理接口

**Controller**: `UserController`  
**基础路径**: `/api/v1/user`

### 1.1 用户注册

**接口**: `POST /api/v1/user/register`

**请求体**:
```json
{
  "userName": "string",
  "password": "string",
  "name": "string",
  "phone": "string",
  "sex": "string",
  "idNumber": "string"
}
```

**响应示例**:
```json
{
  "code": 200,
  "message": "注册成功",
  "data": null
}
```

---

### 1.2 用户登录

**接口**: `POST /api/v1/user/login`

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| userName | String | 否 | admin | 用户名 |
| password | String | 否 | 123456 | 密码 |

**响应示例**:
```json
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "id": 1,
    "userName": "admin",
    "name": "管理员",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
  }
}
```

---

### 1.3 修改密码

**接口**: `POST /api/v1/user/updatePassword`

**请求体**:
```json
{
  "id": 1,
  "oldPassword": "123456",
  "newPassword": "new123",
  "confirmPassword": "new123"
}
```

**说明**: 
- 需要验证旧密码
- 新密码与确认密码必须一致

---

### 1.4 退出登录

**接口**: `POST /api/v1/user/logout`

**请求头**:
```
Authorization: Bearer <token>
```

---

### 1.5 新增用户

**接口**: `POST /api/v1/user/addUser`

**请求体**:
```json
{
  "userName": "string",
  "password": "string",
  "name": "string",
  "phone": "string",
  "sex": "string",
  "idNumber": "string"
}
```

---

### 1.6 分页查询用户

**接口**: `GET /api/v1/user/page`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页大小 |
| name | String | 否 | 姓名模糊查询 |
| phone | String | 否 | 手机号精确查询 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "userName": "admin",
        "name": "管理员",
        "phone": "13800138000",
        "sex": "男",
        "idNumber": "110101199001011234",
        "status": 1
      }
    ],
    "total": 1
  }
}
```

---

### 1.7 启用/禁用账号

**接口**: `POST /api/v1/user/status/{status}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| status | Integer | 0-禁用，1-启用 |

**查询参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| id | Integer | 是 | 用户 ID |

---

### 1.8 根据 ID 查询用户

**接口**: `GET /api/v1/user/{id}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Long | 用户 ID |

---

### 1.9 编辑用户信息

**接口**: `PUT /api/v1/user/update`

**请求体**:
```json
{
  "id": 1,
  "userName": "string",
  "name": "string",
  "phone": "string",
  "sex": "string",
  "idNumber": "string"
}
```

---

## 二、AI 对话接口

### 2.1 RAG 知识库对话

**Controller**: `AiRagController`  
**接口**: `GET /api/v1/ai/rag`

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| message | String | 否 | "你好" | 用户问题 |
| prompt | String | 否 | "你是一名 AI 助手..." | 系统提示语 |

**请求头**:
```
Authorization: Bearer <token>
```

**Content-Type**: `text/event-stream`

**功能特性**:
- ✅ 基于向量知识库的智能问答
- ✅ 支持流式响应 (SSE)
- ✅ 敏感词自动过滤
- ✅ 对话历史记忆
- ✅ 相似度阈值：0.1
- ✅ TopK: 3

**响应示例** (SSE 流):
```
data: 你
data: 好
data: ！
data: 我
data: 是
data: 知
data: 识
data: 库
data: 助
data: 手
data: ...
```

---

### 2.2 普通对话

**Controller**: `ChatController`  
**接口**: `GET /api/v1/chat/stream`

**请求参数**:
| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| message | String | 否 | "你好" | 用户问题 |
| prompt | String | 否 | "你是一名 AI 助手..." | 系统提示语 |

**功能特性**:
- ✅ 不带知识库的纯聊天对话
- ✅ 支持对话历史记忆
- ✅ 敏感词过滤

---

## 三、知识库管理接口

**Controller**: `KnowledgeController`  
**基础路径**: `/api/v1/knowledge`

### 3.1 上传文件到知识库

**接口**: `POST /api/v1/knowledge/file/upload`

**Content-Type**: `multipart/form-data`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| file | List<MultipartFile> | 是 | 文件列表（支持多文件） |

**处理流程**:
1. 上传文件到阿里云 OSS
2. 使用 Tika 解析文档内容
3. TokenTextSplitter 分词处理
4. 向量化存储到 VectorStore
5. 记录文件信息到数据库

**支持格式**: PDF、Word、TXT、Markdown 等

**响应示例**:
```json
{
  "code": 200,
  "message": "文件上传成功",
  "data": null
}
```

---

### 3.2 查询文件列表

**接口**: `GET /api/v1/knowledge/contents`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 是 | 页码 |
| pageSize | Integer | 是 | 每页大小 |
| fileName | String | 否 | 文件名模糊查询 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "fileName": "产品手册.pdf",
        "url": "https://oss.example.com/files/uuid-xxx.pdf",
        "vectorId": "[\"doc-id-1\",\"doc-id-2\",...]",
        "createTime": "2024-01-01T10:00:00",
        "updateTime": "2024-01-01T10:00:00"
      }
    ],
    "total": 1
  }
}
```

---

### 3.3 删除文件

**接口**: `DELETE /api/v1/knowledge/delete`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ids | List<Long> | 是 | 文件 ID 列表 |

**示例**: `DELETE /api/v1/knowledge/delete?ids=1,2,3`

---

### 3.4 下载文件

**接口**: `GET /api/v1/knowledge/download`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| ids | List<Long> | 是 | 文件 ID 列表 |

---

## 四、图像生成接口

**Controller**: `DrawImageController`  
**基础路径**: `/api/v1/draw`

### 4.1 生成图片

**接口**: `GET /api/v1/draw/image`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| prompt | String | 是 | 图片描述文本 |

**示例**: `GET /api/v1/draw/image?prompt=一只可爱的猫咪在草地上玩耍`

**响应**: 
- Content-Type: `image/png`
- 返回二进制图片数据

---

## 五、敏感词管理接口

**Controller**: `SensitiveWordController`  
**基础路径**: `/api/v1/sensitive`

### 5.1 新增敏感词

**接口**: `POST /api/v1/sensitive/add`

**请求体**:
```json
{
  "word": "敏感词内容",
  "categoryId": 1
}
```

**自动设置**:
- status: "1" (启用)
- createdAt: 当前日期
- updatedAt: 当前日期

---

### 5.2 删除敏感词

**接口**: `DELETE /api/v1/sensitive/{id}`

**路径参数**:
| 参数 | 类型 | 说明 |
|------|------|------|
| id | Integer | 敏感词 ID |

---

### 5.3 批量删除敏感词

**接口**: `POST /api/v1/sensitive/batch`

**请求体**:
```json
[1, 2, 3, 4, 5]
```

---

### 5.4 更新敏感词

**接口**: `PUT /api/v1/sensitive`

**请求体**:
```json
{
  "id": 1,
  "word": "新的敏感词",
  "categoryId": 2,
  "status": "1"
}
```

---

### 5.5 分页查询敏感词

**接口**: `GET /api/v1/sensitive/page`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 是 | 页码 |
| size | Integer | 是 | 每页大小 |

**响应示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "word": "敏感词",
        "categoryId": 1,
        "status": "1",
        "createdAt": "2024-01-01",
        "updatedAt": "2024-01-01"
      }
    ],
    "total": 1
  }
}
```

---

### 5.6 查询所有敏感词

**接口**: `GET /api/v1/sensitive`

**响应**: 返回所有敏感词列表

---

## 六、敏感词分类接口

**Controller**: `SensitiveCategoryController`  
**基础路径**: `/api/v1/category`

### 6.1 新增分类

**接口**: `POST /api/v1/category/add`

**请求体**:
```json
{
  "name": "分类名称",
  "description": "分类描述"
}
```

---

### 6.2 批量删除分类

**接口**: `DELETE /api/v1/category/batch`

**请求体**:
```json
[1, 2, 3]
```

---

### 6.3 更新分类

**接口**: `PUT /api/v1/category/update`

**请求体**:
```json
{
  "id": 1,
  "name": "新分类名",
  "description": "新的描述"
}
```

---

### 6.4 分页查询分类

**接口**: `GET /api/v1/category/page`

**请求参数**:
| 参数 | 类型 | 默认值 | 说明 |
|------|------|--------|------|
| page | Integer | 1 | 页码 |
| size | Integer | 10 | 每页大小 |

---

### 6.5 获取全部分类列表

**接口**: `GET /api/v1/category/list`

**响应**: 返回所有敏感词分类列表

---

## 七、分词统计接口

**Controller**: `WordFrequencyController`  
**基础路径**: `/api/v1/frequency`

### 7.1 分页查询分词统计

**接口**: `POST /api/v1/frequency/page`

**请求体**:
```json
{
  "page": 1,
  "pageSize": 10,
  "word": "关键词",
  "startTime": "2024-01-01",
  "endTime": "2024-12-31"
}
```

---

### 7.2 清空分词数据

**接口**: `DELETE /api/v1/frequency/clean`

---

### 7.3 获取分词列表（带缓存）

**接口**: `GET /api/v1/frequency/getList`

**缓存策略**:
- Redis 缓存键：`wordFrequencyList`
- 过期时间：5 分钟
- 缓存命中时直接返回缓存数据

---

## 八、日志管理接口

**Controller**: `LogInfoController`  
**基础路径**: `/api/v1/log`

### 8.1 分页查询日志

**接口**: `GET /api/v1/log/page`

**请求参数**:
| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| page | Integer | 是 | 页码 |
| size | Integer | 是 | 每页大小 |
| methodName | String | 否 | 方法名模糊查询 |
| className | String | 否 | 类名模糊查询 |
| requestParams | String | 否 | 请求参数模糊查询 |

**日志字段**:
- id: 日志 ID
- userId: 用户 ID
- className: 类名
- methodName: 方法名
- requestParams: 请求参数
- responseResult: 响应结果
- executionTime: 执行时间 (ms)
- createTime: 创建时间

---

### 8.2 批量删除日志

**接口**: `POST /api/v1/log/batch`

**说明**: 清空所有日志记录

---

## 认证说明

### JWT Token 使用方式

除以下公开接口外，其他所有接口均需要在请求头中携带 JWT Token：

**公开接口** (无需认证):
- `POST /api/v1/user/login`
- `POST /api/v1/user/register`
- Swagger 文档相关接口

**Token 传递格式**:
```http
Authorization: Bearer <your-jwt-token>
```

### Token 获取流程

1. 调用登录接口获取 Token
2. 将 Token 保存到客户端（localStorage/cookie）
3. 后续请求在 Header 中携带 Token

### Token 配置信息

| 配置项 | 说明 |
|--------|------|
| 密钥 | jwtProperties.userSecretKey |
| 有效期 | jwtProperties.userTtl |
| Claims | userId |

---

## 通用响应格式

### 成功响应

```json
{
  "code": 200,
  "message": "success",
  "data": {
    // 具体业务数据
  }
}
```

### 错误响应

```json
{
  "code": 500,
  "message": "错误描述信息",
  "data": null
}
```

### 常见状态码

| 状态码 | 说明 |
|--------|------|
| 200 | 请求成功 |
| 400 | 参数错误 |
| 401 | 未授权/Token 失效 |
| 403 | 禁止访问 |
| 404 | 资源不存在 |
| 500 | 服务器内部错误 |

### BaseResponse 结构

```java
public class BaseResponse<T> {
    private Integer code;
    private String message;
    private T data;
}
```

---

## 技术架构

### 核心技术栈

| 技术 | 用途 |
|------|------|
| Spring Boot | 基础框架 |
| Spring AI | AI 模型集成 |
| MyBatis Plus | ORM 框架 |
| Redis | 缓存 |
| MySQL | 关系型数据库 |
| VectorStore | 向量数据库 |
| Aliyun OSS | 对象存储 |
| JWT | 身份认证 |

### AI 相关组件

| 组件 | 用途 |
|------|------|
| ChatClient | 大语言模型客户端 |
| VectorStore | 向量存储与检索 |
| TokenTextSplitter | 文本分词器 |
| TikaDocumentReader | 文档解析器 |
| ImageModel | 图像生成模型 |
| ChatMemory | 对话记忆管理 |

### 安全特性

- ✅ JWT Token 认证
- ✅ 敏感词过滤
- ✅ SQL 注入防护
- ✅ 操作日志审计
- ✅ 异常统一处理

---

## 附录

### A. 错误码定义

详见 `ErrorCode.java`:

```java
public enum ErrorCode {
    SUCCESS(200, "操作成功"),
    PARAMS_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "禁止访问"),
    NOT_FOUND(404, "资源不存在"),
    SYSTEM_ERROR(500, "系统内部错误");
}
```

### B. 常量定义

详见 `ApplicationConstant.java`:

```java
public class ApplicationConstant {
    public final static String API_VERSION = "/api/v1";
    public final static String APPLICATION_NAME = "xs-rag-ai";
    public final static String DEFAULT_BASE_URL = "https://api.openai.com";
}
```

### C. 实体类说明

| 实体类 | 说明 |
|--------|------|
| User | 用户信息 |
| SensitiveWord | 敏感词 |
| SensitiveCategory | 敏感词分类 |
| WordFrequency | 分词统计 |
| LogInfo | 操作日志 |
| AliOssFile | OSS 文件记录 |

---

## 联系方式

如有问题，请联系开发团队或查阅项目文档。

---

*文档生成时间：2026-03-29*  
*文档版本：v1.0*
