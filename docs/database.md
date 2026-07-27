# 数据库部署与初始化

## 数据库模式

- 本地默认环境使用 H2 文件数据库，Flyway 加载 `db/migration`。
- PostgreSQL 环境启用 `postgres` profile，加载公共迁移和 `db/migration-postgresql`。
- PostgreSQL Docker 镜像使用 `pgvector/pgvector:pg16`，用于执行 `V100__enable_pgvector.sql`。

公共迁移当前版本为 `V102`。已有迁移文件不得修改，应继续新增更高版本迁移。

## Docker 演示环境

复制 `.env.example` 为 `.env` 后，至少修改数据库密码和演示密码：

```text
POSTGRES_PASSWORD=请替换为数据库密码
DB_PASSWORD=与上面保持一致
DEMO_DATA_ENABLED=true
DEMO_PASSWORD=请替换为演示账号密码
```

启动：

```powershell
docker compose up --build
```

当 `DEMO_DATA_ENABLED=true` 时，系统幂等初始化以下数据：

- 管理员账号：`admin`
- 学生账号：`student`
- 学校东区橄榄实训果园及一条物候期记录

已有同名账号不会被覆盖，也不会被重置密码。正式环境应设置
`DEMO_DATA_ENABLED=false`。

## 迁移检查

查看迁移历史：

```sql
SELECT installed_rank, version, description, success
FROM flyway_schema_history
ORDER BY installed_rank;
```

确认 pgvector：

```sql
SELECT extname, extversion FROM pg_extension WHERE extname = 'vector';
```

## 备份

```powershell
docker compose exec postgres pg_dump -U olive -d olive_starter -Fc -f /tmp/olive_starter.dump
docker compose cp postgres:/tmp/olive_starter.dump ./olive_starter.dump
```

备份文件可能包含账号和业务数据，不应提交到 Git。
