# 开发约定

## 后端分包

```text
com.lanyuan.starter
├─ common        公共响应、异常与 Web 基础
├─ config        CORS、Security、OpenAPI
├─ database      数据库公共实体和探针
├─ system        工程状态接口
├─ auth/user     成员 C 开发
├─ orchard       成员 D 开发
├─ calculator    成员 D 开发
├─ training      成员 D 开发
├─ agent/rag     成员 B 开发
└─ chat/task     成员 B 开发
```

## 接口要求

- 地址统一以 `/api/v1` 开头。
- Controller 只处理协议转换，业务逻辑进入 Service。
- 所有返回值使用 `ApiResponse<T>`。
- 数量和用量计算使用 `BigDecimal`。
- 数据库结构通过 Flyway 管理，禁止依赖 `ddl-auto=update`。
- 新增接口必须包含参数校验、Swagger 描述和测试。

