# 榄园知行 Starter

这是供四人团队练习开发的初始工程，只包含以下内容：

- Spring Boot 3 / Java 17 后端骨架
- Vue 3 / TypeScript / Vite 前端骨架
- 统一响应、全局异常、参数校验、requestId、CORS、Swagger
- Spring Security 空白安全基线（当前全部放行，认证模块由成员实现）
- H2 本地数据库、PostgreSQL 配置、Flyway 基线迁移
- Docker Compose 和基础测试


## 本地启动

后端要求 Java 17 和 Maven 3.6.3+。在项目根目录执行：

```powershell
cd backend
mvn spring-boot:run
```

另开终端启动前端：

```powershell
cd frontend
npm install
npm run dev
```

访问：

- 前端：http://localhost:5173
- 后端状态：http://localhost:8080/api/v1/system/status
- Swagger：http://localhost:8080/swagger-ui/index.html
- H2 Console：http://localhost:8080/h2-console

## Docker

```powershell
Copy-Item .env.example .env
docker compose up --build
```

访问 http://localhost:8088。

## 开发规则

1. `main` 只保存稳定版本，开发从 `develop` 创建功能分支。
2. 每个成员只能修改本人负责模块，公共代码变更必须发起 PR。
3. 接口字段变更同时更新 Swagger、前端类型和接口文档。
4. 禁止提交 `.env`、API Key、密码、`node_modules`、`target` 和数据库文件。
