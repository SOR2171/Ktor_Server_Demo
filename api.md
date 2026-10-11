# Ktor Server Demo 接口文档 (RESTful & kotlinx-rpc)

本文档详细描述了 **Ktor Server Demo** 后端服务的 **kotlinx-rpc 远程过程调用接口** 与 **RESTful / WebSocket 接口**
规范，旨在帮助前端开发者、Kotlin 多平台（KMP）客户端开发者以及联调人员快速集成与测试。

---

## 1. kotlinx-rpc 远程过程调用规范 (推荐)

服务端基于 JetBrains 官方 **kotlinx-rpc** (kRPC 协议) 提供强类型、跨平台、端到端的 RPC 通信支持，基于 WebSocket 传输并支持
JSON 序列化。

### 1.1 RPC 服务端点

- **RPC 挂载端点**: `ws://localhost:8080/rpc`
- **传输协议**: `kRPC` (基于 Ktor WebSockets)
- **序列化格式**: JSON (`kotlinx.rpc.krpc.serialization.json`)

### 1.2 客户端接入方式 (Kotlin / KMP)

在客户端的 `build.gradle.kts` 中引入依赖：

```kotlin
plugins {
    id("org.jetbrains.kotlinx.rpc.plugin") version "0.10.4"
    id("org.jetbrains.kotlin.plugin.serialization") version "2.4.0"
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-ktor-client:0.10.4")
    implementation("org.jetbrains.kotlinx:kotlinx-rpc-krpc-serialization-json:0.10.4")
    implementation("io.ktor:ktor-client-cio:3.6.0")
    implementation("io.ktor:ktor-client-websockets:3.6.0")
}
```

客户端建立 RPC 连接与调用示例：

```kotlin
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.http.*
import kotlinx.rpc.krpc.ktor.client.installKrpc
import kotlinx.rpc.krpc.ktor.client.rpc
import kotlinx.rpc.krpc.serialization.json.json
import kotlinx.rpc.withService

val httpClient = HttpClient(CIO) {
    installKrpc {
        serialization {
            json()
        }
    }
}

// 建立连接到 RPC 端点
val rpcClient = httpClient.rpc {
    url {
        protocol = URLProtocol.WS
        host = "localhost"
        port = 8080
        path("/rpc")
    }
}

// 1. 获取认证服务代理
val authService = rpcClient.withService<AuthRpcService>()
val loginResult = authService.login(LoginVO(username = "test", password = "123456"))

// 2. 获取测试服务代理
val testService = rpcClient.withService<TestRpcService>()
println(testService.hello())

// 3. 获取实时流式服务代理 (支持 Flow 双向流)
val streamService = rpcClient.withService<StreamRpcService>()
val echoResult = streamService.echo("hello")
```

---

### 1.3 RPC 契约定义清单

#### 1.3.1 认证服务 (`AuthRpcService`)

定义在包 `io.github.sor2171.backend.rpc.AuthRpcService`：

```kotlin
@Rpc
interface AuthRpcService {
    // 用户名/邮箱与密码登录
    suspend fun login(loginVO: LoginVO): ApiResponse<AuthorizeVO?>

    // 发送邮箱验证码 (支持 REGISTER 或 RESET)
    suspend fun askCode(email: String, type: EmailType, clientIp: String = "127.0.0.1"): ApiResponse<String?>

    // 邮箱与验证码注册新账号
    suspend fun register(registerVO: EmailRegisterVO): ApiResponse<String?>

    // 验证码重置密码
    suspend fun resetPassword(resetVO: PasswordResetVO): ApiResponse<String?>

    // 注销登录并拉黑 Token
    suspend fun logout(token: String): ApiResponse<String?>

    // 续签刷新 Token
    suspend fun relogin(token: String): ApiResponse<ReloginVO?>
}
```

#### 1.3.2 辅助与测试服务 (`TestRpcService`)

定义在包 `io.github.sor2171.backend.rpc.TestRpcService`：

```kotlin
@Rpc
interface TestRpcService {
    // 连通性测试，返回 "Hello Kotlin Ktor RPC!"
    suspend fun hello(): String

    // 签发一次性 WebSocket 握手 Token
    suspend fun getWsToken(handlerName: String = "ExampleHandler"): ApiResponse<String>
}
```

#### 1.3.3 实时与双向流通信服务 (`StreamRpcService`)

定义在包 `io.github.sor2171.backend.rpc.StreamRpcService`，利用 Kotlin 协程 `Flow` 进行全双工流式传输：

```kotlin
@Rpc
interface StreamRpcService {
    // 单次回显调用
    suspend fun echo(message: String): String

    // 响应式双向流传输 (Client Flow -> Server Flow)
    fun echoStream(messages: Flow<String>): Flow<String>
}
```

---

## 2. 全局设计规范 (RESTful API)

### 2.1 服务基础信息

- **服务基础地址 (Base URL)**: `http://localhost:8080`
- **WebSocket 基础地址 (WS URL)**: `ws://localhost:8080`
- **字符编码**: `UTF-8`
- **数据格式**: 请求体与响应体默认采用 `application/json`；部分接口（如登录）额外兼容 `application/x-www-form-urlencoded`。

### 2.2 跨域策略 (CORS)

服务已配置全局跨域支持：

- 支持任意 Host 源请求 (`anyHost()`)
- 允许携带凭证 (`Access-Control-Allow-Credentials: true`)
- 允许的请求方法：`GET`, `POST`, `PUT`, `DELETE`, `PATCH`, `OPTIONS`
- 允许的请求头：`Content-Type`, `Authorization`

### 2.3 身份认证机制 (Authentication)

- 基于 **JWT (JSON Web Token)** 进行无状态认证（HMAC256 签名）。
- 对于受保护的接口，客户端须在请求头中携带：
  ```http
  Authorization: Bearer <your_jwt_token>
  ```
- **Token 结构说明**：包含用户 ID (`id`)、用户名 (`username`)、角色权限 (`authorities`)、唯一标识 (`jti`) 及签发/过期时间戳。默认有效期为
  168 小时（7 天）。
- **Token 黑名单机制**：用户调用注销登录（Logout）后，Token 的唯一标识（`jti`）将被置入 Redis 黑名单，直至该 Token 自然过期。黑名单中的
  Token 即使签名有效也无法通过鉴权。

### 2.4 全局限流与防护 (Flow Limit)

- 服务端基于客户端 IP 实现全局访问频率保护：
    - 默认限频规则： **3 秒内请求次数超过 1000 次** 将触发防护。
    - 触发防护后封禁时长： **60 秒**。
    - 被限流时的响应：HTTP 状态码 `403 Forbidden`，返回信息为 `"Too many requests"`。
- 发送邮件验证码接口具有独立的频控逻辑： **同一 IP 60 秒内仅允许请求一次**。

### 2.5 统一响应结构 (ApiResponse)

所有 RESTful 接口均遵循统一的包装结构（除连通性测试等纯文本接口外）：

```json
{
  "code": 200,
  "data": null,
  "message": "Success"
}
```

#### 字段说明

| 字段名    | 类型     | 说明                                                                      |
|:----------|:---------|:--------------------------------------------------------------------------|
| `code`    | `Int`    | 业务状态码，通常与 HTTP 状态码保持一致（如 200、400、401、403、404、500） |
| `data`    | `Any?`   | 业务数据对象，成功时返回对应实体，无数据或失败时通常为 `null`             |
| `message` | `String` | 提示消息（如 `"Success"` 或具体错误原因）                                 |

---

## 3. RESTful 接口列表概览

> **说明**：用户认证相关路由同时支持 `/api/v1/auth/...` 与兼容前缀 `/api/auth/...`。

| 分类        | 方法   | 路径                    | 认证方式       | 说明                                   |
|:------------|:-------|:------------------------|:---------------|:---------------------------------------|
| **认证**    | `POST` | `/api/v1/auth/login`    | 公开           | 用户名/邮箱与密码登录，返回 JWT        |
| **认证**    | `GET`  | `/api/v1/auth/ask-code` | 公开           | 发送邮箱验证码（注册/重置密码）        |
| **认证**    | `POST` | `/api/v1/auth/register` | 公开           | 邮箱验证码注册新账号                   |
| **认证**    | `POST` | `/api/v1/auth/reset`    | 公开           | 邮箱验证码重置登录密码                 |
| **认证**    | `GET`  | `/api/v1/auth/logout`   | `Bearer Token` | 注销登录并拉黑当前 Token               |
| **认证**    | `GET`  | `/api/v1/auth/relogin`  | `Bearer Token` | 续签刷新 Token                         |
| **测试**    | `GET`  | `/api/test/hello`       | 公开           | 服务连通性测试                         |
| **测试**    | `GET`  | `/api/test/wstoken`     | 公开           | 签发一次性 WebSocket 握手 Token        |
| **WS 通信** | `WS`   | `/ws/test`              | 公开           | WebSocket 基础连通测试通道             |
| **WS 通信** | `WS`   | `/ws/v1/example`        | `?token=...`   | 基于一次性 Token 鉴权的 WebSocket 管道 |

---

## 4. RESTful 详细接口规范

### 4.1 用户登录 (`POST /api/v1/auth/login`)

- **请求体 (JSON)**:
  ```json
  {
    "username": "test",
    "password": "123456",
    "remember": false
  }
  ```
- **成功响应 (HTTP 200)**:
  ```json
  {
    "code": 200,
    "data": {
      "username": "test",
      "role": "user",
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expire": "2026-10-18T10:00:00"
    },
    "message": "Success"
  }
  ```

### 4.2 发送邮箱验证码 (`GET /api/v1/auth/ask-code`)

- **Query 参数**: `email`（邮箱地址），`type`（可选，`register` 或 `reset`）。
- **成功响应 (HTTP 200)**:
  ```json
  {
    "code": 200,
    "data": null,
    "message": "Success"
  }
  ```

### 4.3 邮箱注册 (`POST /api/v1/auth/register`)

- **请求体 (JSON)**:
  ```json
  {
    "email": "user@example.com",
    "code": "849201",
    "username": "newuser",
    "password": "Password123"
  }
  ```
- **成功响应 (HTTP 200)**:
  ```json
  {
    "code": 200,
    "data": null,
    "message": "Success"
  }
  ```

### 4.4 密码重置 (`POST /api/v1/auth/reset`)

- **请求体 (JSON)**:
  ```json
  {
    "email": "user@example.com",
    "code": "849201",
    "password": "NewPassword123"
  }
  ```

### 4.5 退出登录 (`GET /api/v1/auth/logout`)

- **请求头**: `Authorization: Bearer <token>`
- **成功响应 (HTTP 200)**:
  ```json
  {
    "code": 200,
    "data": null,
    "message": "Success"
  }
  ```

### 4.6 刷新续期 Token (`GET /api/v1/auth/relogin`)

- **请求头**: `Authorization: Bearer <token>`
- **成功响应 (HTTP 200)**:
  ```json
  {
    "code": 200,
    "data": {
      "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
      "expire": "2026-10-18T10:00:00"
    },
    "message": "Success"
  }
  ```

---

## 5. 附录：测试账号

在导入初始数据后，默认预置以下测试账号：

- **用户名**: `test`
- **登录密码**: `123456`
- **绑定邮箱**: `1234567890@gmail.com`
- **默认角色**: `user`
