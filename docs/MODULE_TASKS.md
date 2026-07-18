# 四人开发任务

## 成员 A：前端

- 登录、总览、智能问答、任务、计算器、实训记录、果园、知识库、用户管理页面
- Vue Router、Pinia、Axios JWT、SSE、表单校验、异常状态和移动端适配
- 负责目录：`frontend/src/`

## 成员 B：后端架构与智能体

- LangChain4j、百炼/硅基流动、Agent、RAG、天气、会话、SSE、农事任务
- Docker、Swagger、系统集成和公共代码审查
- 建议目录：`backend/.../agent`、`rag`、`weather`、`chat`、`task`

## 成员 C：认证与用户

- JWT、BCrypt、登录、当前用户、用户管理、角色权限、操作日志
- 认证和越权测试
- 建议目录：`backend/.../auth`、`user`、`security`

## 成员 D：果园业务

- 果园、物候期、四个农业计算器、实训记录、图片上传
- Flyway 业务表迁移、CRUD 和计算测试
- 建议目录：`backend/.../orchard`、`calculator`、`training`、`file`

## 当前已提供的公共基线

| 能力 | 位置 | 状态 |
|---|---|---|
| 统一响应 | `common/api` | 可用 |
| 全局异常 | `common/exception` | 可用 |
| requestId | `common/web` | 可用 |
| 参数校验 | Spring Validation | 可用 |
| CORS | `config/CorsConfig` | 可用 |
| 安全骨架 | `config/SecurityConfig` | 待成员 C 实现 JWT |
| JPA 基类 | `database/entity/BaseEntity` | 可用 |
| Flyway | `db/migration` | 可用 |
| 数据库探针 | `/api/v1/system/status` | 可用 |
| Swagger | `/swagger-ui/index.html` | 可用 |

## 禁止直接复制的内容

本 Starter 不提供完整项目中的业务 Controller、Service、Entity、前端业务页面或 Agent 实现。成员应按照需求文档和接口文档独立开发。

