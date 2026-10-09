# Ktor Server Demo

English / [简体中文](./README.md)

---

A Ktor 3.x backend project rewritten in an idiomatic Ktor manner, fully aligned with the features and data schemas of `Springboot4-Vue3`, providing seamless support for the Vue 3 frontend.

---

## Tech Stack

- **Language & Build**: Kotlin (`jvmToolchain(21)` / `25`) + Gradle (Kotlin DSL)
- **Web Framework**: Ktor 3.6.0 (Engine: CIO)
- **Database & ORM**:
  - MySQL 8+ (`mysql-connector-j:9.2.0`)
  - HikariCP Connection Pool (`5.1.0`)
  - JetBrains Exposed ORM (`0.55.0`, including Core, JDBC, DAO, and `exposed-kotlin-datetime`)
- **Cache & Rate Limiting**: Redis 6+ + Lettuce Client (`6.3.2.RELEASE`)
- **Message Queue**: RabbitMQ 3.8+ (`amqp-client:5.34.0`)
- **Mail Service**: Jakarta Mail (`org.eclipse.angus:jakarta.mail:2.0.4`)
- **Authentication & Encryption**:
  - Auth0 Java-JWT (`4.4.0`) + Ktor Auth JWT (`ktor-server-auth-jwt`)
  - BCrypt Password Hashing (`at.favre.lib:bcrypt:0.10.2`, 100% compatible with Spring Security BCrypt)
- **Serialization & Datetime**: `kotlinx-serialization-json` + `kotlinx-datetime`
- **Real-time Communication**: Ktor WebSockets (`ktor-server-websockets`)
- **CORS & Error Handling**: Ktor CORS (`ktor-server-cors`) + StatusPages (`ktor-server-status-pages`)

---

## Prerequisites

- JDK 21+ (JDK 21 or JDK 25 recommended)
- MySQL 8.0+
- Redis 6.0+
- RabbitMQ 3.8+

---

## Quick Start

### 1. Initialize Database

Import the included database schema and sample data:

```bash
mysql -u backend -p kotlin_springboot < src/main/resources/springboot_vue3.sql
```

> **Default Test Account**:
> - Username: `test`
> - Password: `123456`
> - Email: `1234567890@gmail.com`

### 2. Configure Application Properties

Review and edit `src/main/resources/application.yaml` (or refer to `application-example.yaml`):

- **datasource**: JDBC database URL, username, and password
- **redis**: Redis connection URL (default: `redis://localhost:6379`)
- **rabbitmq**: RabbitMQ credentials and queue name (default queue: `mail`)
- **mail**: SMTP account and authorization code
- **jwt**: JWT secret key and expiration hours

### 3. Build & Run

Run Gradle tasks in the project root directory:

```bash
# Build project
./gradlew build

# Run server
./gradlew run
```

Upon successful startup, the server listens and logs:
```text
INFO Application - Responding at http://0.0.0.0:8080
```

---

## API Endpoints

| Method | Path                                            | Authentication | Description                                                         |
|:-------|:------------------------------------------------|:---------------|:--------------------------------------------------------------------|
| `POST` | `/api/v1/auth/login` or `/api/auth/login`       | Public         | Login via username/email and password, returns Token and expiration |
| `GET`  | `/api/v1/auth/ask-code` or `/api/auth/ask-code` | Public         | Send email verification code (`?email=...&type=register`)           |
| `POST` | `/api/v1/auth/register` or `/api/auth/register` | Public         | Register a new account via email                                    |
| `POST` | `/api/v1/auth/reset` or `/api/auth/reset`       | Public         | Reset password with verification code                               |
| `GET`  | `/api/v1/auth/logout` or `/api/auth/logout`     | `Bearer Token` | Invalidate token and add to Redis blacklist                         |
| `GET`  | `/api/v1/auth/relogin` or `/api/auth/relogin`   | `Bearer Token` | Refresh and renew JWT token                                         |
| `GET`  | `/api/test/hello`                               | Public         | Health and connectivity test                                        |
| `GET`  | `/api/test/wstoken`                             | Public         | Issue a one-time handshake token for WebSocket                      |
| `WS`   | `/ws/test`                                      | Public         | Test WebSocket echo pipeline                                        |
| `WS`   | `/ws/v1/example`                                | `?token=...`   | Establish WebSocket connection with one-time token verification     |
