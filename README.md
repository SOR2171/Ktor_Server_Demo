# Ktor Server Demo

[English](./README_en.md) / 简体中文

---

Ktor 3.x 后端项目，采用纯正、原生的 Ktor 方式（Idiomatic Ktor）完全重构实现，与 `Springboot4-Vue3` 项目功能与数据结构对齐，支持 Vue 3 前端无缝对接。

---

## 技术栈

- **语言与构建**：Kotlin (`jvmToolchain(21)` / `25`) + Gradle (Kotlin DSL)
- **Web 框架**：Ktor 3.6.0 (Engine: CIO)
- **数据库与 ORM**：
  - MySQL 8+ (`mysql-connector-j:9.2.0`)
  - HikariCP 连接池 (`5.1.0`)
  - JetBrains Exposed ORM (`0.55.0`，包含 Core、JDBC、DAO 与 `exposed-kotlin-datetime`)
- **缓存与频控**：Redis 6+ + Lettuce Client (`6.3.2.RELEASE`)
- **消息队列**：RabbitMQ 3.8+ (`amqp-client:5.34.0`)
- **邮件服务**：Jakarta Mail (`org.eclipse.angus:jakarta.mail:2.0.4`)
- **认证与加密**：
  - Auth0 Java-JWT (`4.4.0`) + Ktor Auth JWT (`ktor-server-auth-jwt`)
  - BCrypt 密码加密 (`at.favre.lib:bcrypt:0.10.2`，与 Spring Security BCrypt 100% 兼容)
- **序列化与时间**：`kotlinx-serialization-json` + `kotlinx-datetime`
- **实时通信**：Ktor WebSockets (`ktor-server-websockets`)
- **跨域与异常拦截**：Ktor CORS (`ktor-server-cors`) + StatusPages (`ktor-server-status-pages`)

---

## 运行要求

- JDK 21+（推荐 JDK 21 或 JDK 25）
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+

---

## 快速启动

### 1. 初始化数据库

导入项目自带的 SQL 架构与初始数据：

```bash
mysql -u backend -p kotlin_springboot < src/main/resources/springboot_vue3.sql
```

> **默认测试账户**：
> - 用户名：`test`
> - 密码：`123456`
> - 邮箱：`1234567890@gmail.com`

### 2. 配置应用参数

检查并修改 `src/main/resources/application.yaml`（或参考 `application-example.yaml`）：

- **datasource**：数据库连接地址、用户名和密码
- **redis**：Redis 连接 URL（默认 `redis://localhost:6379`）
- **rabbitmq**：RabbitMQ 连接凭据与队列名（默认队列 `mail`）
- **mail**：SMTP 邮件发送账号与授权码
- **jwt**：JWT 签名秘钥与过期时间

### 3. 构建与运行

在项目根目录下执行 Gradle 任务：

```bash
# 构建项目
./gradlew build

# 运行服务器
./gradlew run
```

服务启动成功后，默认监听并在控制台输出：
```text
INFO Application - Responding at http://0.0.0.0:8080
```

---

## 接口列表

| 方法   | 路径                                            | 认证方式       | 说明                                         |
|:-------|:------------------------------------------------|:---------------|:---------------------------------------------|
| `POST` | `/api/v1/auth/login` 或 `/api/auth/login`       | 公开           | 用户名/邮箱与密码登录，返回 Token 与过期时间 |
| `GET`  | `/api/v1/auth/ask-code` 或 `/api/auth/ask-code` | 公开           | 发送邮箱验证码（`?email=...&type=register`） |
| `POST` | `/api/v1/auth/register` 或 `/api/auth/register` | 公开           | 邮箱注册新账号                               |
| `POST` | `/api/v1/auth/reset` 或 `/api/auth/reset`       | 公开           | 通过验证码重置密码                           |
| `GET`  | `/api/v1/auth/logout` 或 `/api/auth/logout`     | `Bearer Token` | 注销 Token 并加入 Redis 黑名单               |
| `GET`  | `/api/v1/auth/relogin` 或 `/api/auth/relogin`   | `Bearer Token` | 刷新并续签 Token                             |
| `GET`  | `/api/test/hello`                               | 公开           | 连通性测试                                   |
| `GET`  | `/api/test/wstoken`                             | 公开           | 签发 WebSocket 一次性握手 Token              |
| `WS`   | `/ws/test`                                      | 公开           | 测试 WebSocket 管道                          |
| `WS`   | `/ws/v1/example`                                | `?token=...`   | 校验一次性 Token 后建立连接                  |
