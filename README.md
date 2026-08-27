# 智愿AI报考平台

一个面向高考志愿填报场景的 AI 应用项目，核心目标不是"把推荐全交给大模型"，而是让 AI 负责自然语言理解与对话编排，后端保留可控、可解释、可审计的推荐逻辑。

项目当前已经具备完整单体工程闭环：
- 用户登录注册、画像维护、推荐历史、志愿方案保存
- 基于分数 / 位次 / 省份 / 科类 / 偏好的院校与专业推荐
- `冲 / 稳 / 保` 概率评分与规则解释
- 基于 RocketMQ 的自由文本推荐异步任务、失败重试与状态查询
- 管理员维护院校、专业、录取数据
- 管理员查看用户报考资料与业务计数，并维护账号角色和启停状态
- 对话式 Agent 工作台，可读取画像、生成推荐、查询学校详情、操作当前志愿单

## 项目亮点

### 1. AI 只做抽取和助手，不直接决定推荐结果
- 自由文本输入先由 AI 解析成结构化条件，例如省份、分数、位次、偏好专业、排除专业
- 推荐核心仍由后端规则、概率评分和排序逻辑完成
- 每条结果都会返回推荐理由、风险等级、规则解释，便于展示和追踪

### 2. 从固定阈值升级到基础版"冲稳保概率评分"
- 不再只按固定 `scoreGap / rankGap` 生硬分档
- 结合位次差、分数差、年份波动、偏好命中等因素计算基础概率分
- 再按概率区间映射到 `冲 / 稳 / 保`，结果更接近真实推荐场景

### 3. 做了受控多轮对话 Agent，而不是自由放权
- Agent 通过受控工具调用现有后端能力：`getUserProfile`、`recommendSchools`、`recommendMajors`、`getSchoolDetail`、`addPlanItem`、`savePlan`
- 单轮工具调用上限显式限制，写操作带确认边界
- 工具失败有统一错误分类和兜底提示，不会直接把接口打成 500

### 4. 数据建模和后端边界按真实业务拆开了
- 把 `院校 / 专业主数据`、`院校录取线 / 专业录取线事实数据`、`用户画像 / 推荐历史 / 志愿方案 / Agent 会话消息` 分开建模，而不是塞进一张大表
- AI 解析、推荐计算、志愿方案、Agent 工具调用分别走独立 service，避免大模型直接穿透数据库和核心推荐逻辑
- 这样既方便管理员维护基础数据，也方便后端做规则解释、历史追踪和后续索引优化

## 技术栈

### 后端
- Java 17
- Spring Boot 3.3.5
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8
- Redis（可选）
- RocketMQ 5（可选）
- springdoc-openapi

### 前端
- Vue 3
- Vite
- Element Plus

### 测试与部署
- Spring Boot Test
- H2
- Docker / Docker Compose

## 核心功能

### 用户侧
- 分数推荐：按省份、科类、推荐模式生成院校推荐
- 自由文本推荐：输入自然语言后由 AI 抽取条件，再走后端推荐链路
- 异步推荐任务：通过 RocketMQ 投递自由文本推荐任务，支持任务状态查询、消费重试和失败原因记录
- 推荐解释：返回命中偏好、冲稳保分层、风险指数、判断依据
- AI 总结：对结果进行简洁总结和填报建议生成
- 推荐历史：保存分数推荐与自由文本推荐记录
- 志愿方案：支持当前草稿、方案保存、方案回看
- AI 对话：支持查看画像、获取推荐、查询学校详情、加入志愿单、保存方案

### 管理侧
- 院校基础信息维护
- 专业基础信息维护
- 院校录取线维护
- 专业录取线维护

## 系统设计

### 推荐链路
1. 用户输入分数表单或自然语言需求
2. AI 将自然语言抽取为结构化条件
3. 后端做参数校验、规则补全、分数位次转换
4. 推荐服务按条件筛选候选院校/专业
5. 计算基础概率分并映射为 `冲 / 稳 / 保`
6. 返回推荐结果、解释字段和 AI 总结

### Agent 链路
1. 用户在 `AI 对话` 页面输入问题
2. 后端先做意图识别和工具决策
3. Agent 只调用白名单工具，不做自由查库
4. 工具结果结构化落库，支持会话回放和追踪
5. 写操作继续复用现有志愿方案服务，不绕过业务边界

### RocketMQ 异步任务链路
1. 接口先创建 `PENDING` 任务，再将任务 ID、用户 ID、请求 ID 和原始请求投递到 RocketMQ
2. Producer 使用请求 ID 作为消息 Key；发送失败时将数据库任务直接标记为 `FAILED`
3. Consumer 通过 `PENDING -> RUNNING` 条件更新抢占任务，重复消息无法重复执行已完成任务
4. 可重试异常将任务恢复为 `PENDING` 并交给 Broker 重试；达到上限后记录 `FAILED` 和失败原因
5. 超时停留在 `RUNNING` 的任务允许重新抢占，不额外引入分布式锁或 Outbox

## 目录结构

```text
.
├─ src/main/java                # Spring Boot 后端
├─ src/main/resources           # 配置与静态资源
├─ frontend                     # Vue 3 + Vite 前端
├─ sql                          # 建表、初始化与导入脚本
├─ Dockerfile
└─ docker-compose.yml
```

## 本地启动

### 前置条件

- **Java 17+**
- **Node.js 18+** 和 npm
- **MySQL 8**（或使用 Docker）
- **Maven**（项目自带 Maven Wrapper `mvnw`/`mvnw.cmd`）

### 1. 启动数据库

如果本地没有 MySQL，可以用 Docker 启动：

```powershell
docker run -d --name zhiyuan-mysql `
  -e MYSQL_ROOT_PASSWORD=zhiyuan_root_2026 `
  -e MYSQL_DATABASE=college_recommendation `
  -e MYSQL_USER=zhiyuan `
  -e MYSQL_PASSWORD=zhiyuan123 `
  -p 127.0.0.1:3307:3306 `
  mysql:8.4
```

应用默认连接 `localhost:3307`，使用专用账号 `zhiyuan`，不再依赖本机 MySQL 的 `root` 密码。若使用自定义账号，只需通过 `DB_HOST`、`DB_PORT`、`DB_USER`、`DB_PASSWORD` 覆盖。

### 2. 启动后端

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

默认访问：
- 应用首页：`http://localhost:8080`
- 公共健康冒烟：`http://localhost:8080/api/meta/options`

Swagger / OpenAPI 默认关闭。仅在可信开发环境同时设置
`SPRINGDOC_API_DOCS_ENABLED=true`、`SPRINGDOC_SWAGGER_UI_ENABLED=true` 和
`SECURITY_EXPOSE_API_DOCS=true` 后，才可访问 `/swagger-ui.html` 与 `/v3/api-docs`。

本地默认关闭 RocketMQ，并通过同一任务处理器同步执行，便于开发和测试；`prod` profile 与 Docker Compose 默认开启 RocketMQ。

后端每次启动都会执行打包在应用内的幂等 `sql/schema.sql`，自动补齐当前版本缺少的表和兼容字段，但不会执行 `sql/data.sql`。如运行账号明确没有 DDL 权限，可设置 `DB_SCHEMA_INIT_MODE=never`，并在发布前手动执行 schema。

### 3. 启动前端开发环境

```powershell
cd frontend
npm install
npm run dev
```

默认地址：
- 前端开发环境：`http://localhost:5173`

说明：
- Vite 会把 `/api` 代理到 `http://localhost:8080`
- 若后端不在默认地址，请复制 `frontend/.env.example` 为 `frontend/.env.local`，并设置
  `VITE_API_PROXY_TARGET`（例如 `http://127.0.0.1:18080`）。这样浏览器仍通过相对 `/api`
  请求后端，避免开发环境因目标地址不一致而报“网络连接异常”。

### 4. 演示模式（无需后端）

如果只需要展示前端效果，可以使用演示模式，无需启动后端服务：

```powershell
cd frontend
npm install
npm run dev:mock
```

默认地址：
- 前端开发环境：`http://localhost:5173`

演示模式特点：
- 使用模拟数据，无需数据库和后端服务
- 支持普通用户页面和管理员管理页面的基本功能展示

演示账号：

| 身份 | 用户名 | 密码 | 登录后页面 |
| --- | --- | --- | --- |
| 普通用户 | `testuser` | 任意非空密码 | 推荐查询 |
| 管理员 | `admin` | `admin123` | 管理界面 |

管理员演示包含：
- 用户概览、用户名/角色/状态筛选和账号设置
- 院校、专业、院校录取线、专业录取线的列表与筛选
- 上述四类基础数据的新增和编辑交互

Mock 管理操作只保存在当前页面会话内存中，刷新页面后会恢复初始演示数据，不会连接或修改真实数据库。若浏览器保留了上一次登录状态，可先点击右上角“退出”，再使用对应演示账号登录。

Docker Compose 使用全新 MySQL 数据卷并执行 `sql/data.sql` 后，测试账号为：
- 普通用户：`testuser / 123456`
- 管理员：`adminuser / 123456`

生产环境不要保留示例密码。若不导入示例数据，可先注册普通账号，再由数据库管理员执行受控 SQL 将指定账号的 `role` 更新为 `ADMIN`。

### 5. 构建前端并交给后端托管

**重要：前端构建后需要将产物复制到 Spring Boot 的静态资源目录才能被正确加载。**

```powershell
cd frontend
npm run build

# 将构建产物复制到 Spring Boot 静态资源目录
Copy-Item -Path ..\src\main\resources\static\index.html -Destination ..\target\classes\static\index.html -Force
Copy-Item -Path ..\src\main\resources\static\assets\*.js -Destination ..\target\classes\static\assets\ -Force
Copy-Item -Path ..\src\main\resources\static\assets\*.css -Destination ..\target\classes\static\assets\ -Force
```

然后重新启动后端即可通过 `http://localhost:8080` 访问完整页面。

**或者使用 Maven 编译（会自动复制静态资源）：**

```powershell
# Windows
.\mvnw.cmd compile

# macOS / Linux
./mvnw compile
```

## Docker Compose 启动

这是推荐的首次部署方式。Dockerfile 会在镜像内构建 Vue 前端和 Spring Boot 后端，
不需要在宿主机另外安装 Java、Node.js、Maven、MySQL、Redis 或 RocketMQ。

### 1. 部署前提

- Docker Engine 24+ 或 Docker Desktop
- Docker Compose v2（命令为 `docker compose`，不是旧版 `docker-compose`）
- Git
- 建议至少预留 4 GB 可用内存和 8 GB 磁盘；首次构建需要访问 Docker Hub 和 Maven/npm 软件源
- 服务器只需向公网放行应用端口，默认是 TCP 8080

先确认工具可用：

```powershell
docker version
docker compose version
git --version
```

### 2. 获取指定分支

```powershell
git clone --branch codex/final-admin-integration --single-branch https://github.com/Zoean-z/zhiyuan.git
Set-Location -LiteralPath .\zhiyuan
```

Linux / macOS 将第二行替换为 `cd zhiyuan`。

### 3. 准备环境变量

```powershell
Copy-Item -LiteralPath .env.example -Destination .env
```

Linux / macOS 使用 `cp .env.example .env`。`.env` 已被 Git 忽略，不要提交它。

首次启动前必须修改：

- `MYSQL_ROOT_PASSWORD`：MySQL 管理密码
- `DB_PASSWORD`：应用数据库账号密码
- `AUTH_JWT_SECRET`：JWT 签名密钥，解码后至少 32 字节

可选项：

- `SERVER_HOST_PORT`：宿主机访问端口，默认 `8080`
- `QWEN_ENABLED` / `QWEN_API_KEY`：需要真实 AI 调用时才开启并填写
- `SPRINGDOC_*` / `SECURITY_EXPOSE_API_DOCS`：仅可信开发环境临时开启接口文档

PowerShell 生成 JWT 密钥：

```powershell
$jwtBytes = [byte[]]::new(48)
[System.Security.Cryptography.RandomNumberGenerator]::Fill($jwtBytes)
[Convert]::ToBase64String($jwtBytes)
```

Linux / macOS 可执行：

```bash
openssl rand -base64 48
```

把输出完整填入 `.env` 的 `AUTH_JWT_SECRET=` 后面。不要把真实密钥粘贴到日志、截图或提交记录中。
没有配置 AI Key 时保持 `QWEN_ENABLED=false`；基础登录、推荐、志愿表和本地 Agent 工具仍可使用。

### 4. 启动前校验

```powershell
docker compose config --quiet
```

命令无输出且退出码为 0 才继续。若提示 `AUTH_JWT_SECRET is required`，说明 `.env` 中的密钥仍为空；
若提示端口已占用，修改 `.env` 中对应的 `*_HOST_PORT`，其中应用端口改 `SERVER_HOST_PORT`。

### 5. 首次构建并启动

```powershell
docker compose up -d --build
```

首次启动会下载基础镜像、构建前后端，并在**空的** `mysql-data` 数据卷中依次执行：

1. `sql/schema.sql`
2. `sql/data.sql`
3. `sql/upgrade-20260822-unique-keys.sql`
4. `sql/init-competition-admission-cutoffs.sql`

初始化完成前不要中断 MySQL。查看状态和日志：

```powershell
docker compose ps
docker compose logs --tail=200 mysql
docker compose logs --tail=200 backend
```

成功标准：`mysql`、`redis`、`rocketmq-nameserver`、`rocketmq-broker` 和 `backend`
均为 `running`/`healthy`，`rocketmq-init` 为正常退出的 `exited (0)`。首次构建和初始化所需时间取决于网络与磁盘速度。

默认宿主机端口：

- 后端服务：`http://localhost:8080`
- MySQL：`localhost:3307`
- Redis：`localhost:6380`
- RocketMQ NameServer：`localhost:9876`
- RocketMQ Broker：`localhost:10911`

说明：

- Swagger / OpenAPI 默认关闭，不应把 `/swagger-ui.html` 当作部署健康检查
- `backend` 在 Compose 中固定使用 `DB_SCHEMA_INIT_MODE=never`，不会重复执行 `schema.sql`
- `docker-entrypoint-initdb.d` 只在 MySQL 数据卷为空时运行；复用旧卷不会自动重放新增或修改后的 SQL
- `backend` 默认使用 `prod` profile，并在 MySQL、Redis 和 RocketMQ Broker 健康后启动
- Compose 会先初始化 RocketMQ 持久化卷权限，再启动 NameServer 与 Broker
- MySQL、Redis 和 RocketMQ 的宿主机端口只绑定 `127.0.0.1`，不需要向公网开放

### 6. 首次部署验收

PowerShell：

```powershell
$appBaseUri = 'http://127.0.0.1:8080'
(Invoke-WebRequest -Uri "$appBaseUri/" -UseBasicParsing -TimeoutSec 15).StatusCode
(Invoke-RestMethod -Uri "$appBaseUri/api/meta/options" -TimeoutSec 15) | ConvertTo-Json -Depth 4
```

Linux / macOS：

```bash
curl -fsS http://127.0.0.1:8080/ >/dev/null
curl -fsS http://127.0.0.1:8080/api/meta/options
```

预期首页返回 HTTP 200，元数据接口返回省份与科类。全新演示库可用：

- 普通用户：`testuser / 123456`
- 管理员：`adminuser / 123456`

两种账号都调用 `POST /api/auth/login`，前端根据响应中的 `USER` / `ADMIN` 角色进入不同页面。
这些仅是比赛演示账号；公开或共享部署前必须删除、禁用或修改示例账号密码。

### 7. 远程服务器部署

服务器安装 Docker Engine 与 Compose 插件后执行：

```bash
git clone --branch codex/final-admin-integration --single-branch https://github.com/Zoean-z/zhiyuan.git
cd zhiyuan
cp .env.example .env
```

编辑 `.env`，至少替换 `MYSQL_ROOT_PASSWORD`、`DB_PASSWORD` 和 `AUTH_JWT_SECRET`；需要真实 AI 对话时再填写 `QWEN_API_KEY` 并设置 `QWEN_ENABLED=true`。然后启动：

```bash
docker compose config --quiet
docker compose up -d --build
docker compose ps
docker compose logs --tail=200 backend
```

直接访问 `http://服务器公网IP:8080`。若修改了 `SERVER_HOST_PORT`，访问端口也要相应修改。
云服务器安全组和系统防火墙只需放行应用端口；MySQL、Redis、RocketMQ 不需要开放公网端口。
已有域名时，可让 Nginx/Caddy 反向代理到 `127.0.0.1:8080` 并配置 HTTPS。

### 8. 更新部署

更新前先备份数据库：

```powershell
docker compose exec -T mysql sh -c 'mysqldump -uroot -p"$MYSQL_ROOT_PASSWORD" --single-transaction "$MYSQL_DATABASE"' > zhiyuan-backup.sql
```

然后拉取和重建：

```powershell
git pull
docker compose config --quiet
docker compose up -d --build
docker compose ps
```

注意：`docker compose up -d --build` 不会在已有 `mysql-data` 卷中重新执行
`docker-entrypoint-initdb.d`。如果新版本明确要求数据库升级，应先备份，再按该版本说明手动执行指定的
`sql/upgrade-*.sql`，不能直接重放 `schema.sql` 或 `data.sql`。

PowerShell 执行某个经过确认的升级脚本示例：

```powershell
Get-Content -LiteralPath .\sql\upgrade-20260822-unique-keys.sql -Raw -Encoding UTF8 |
  docker compose exec -T mysql sh -c 'mysql -uroot -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"'
```

### 9. 停止、重启与查看日志

```powershell
docker compose down
docker compose up -d
docker compose logs -f --tail=200 backend
```

`docker compose down` 会删除容器和默认网络，但保留数据卷。以下命令会永久删除数据库、Redis 和 RocketMQ 数据，
仅允许在确认已有备份且确实要重置全新环境时使用：

```powershell
docker compose down -v
```

### 10. 首次部署常见失败

- `AUTH_JWT_SECRET is required`：`.env` 未填写 JWT 密钥，重新生成后再执行配置校验
- `port is already allocated`：修改 `.env` 中对应 `*_HOST_PORT`；Docker 部署的应用端口只改 `SERVER_HOST_PORT`
- `backend` 一直不健康：先看 `docker compose logs --tail=200 backend`，再确认依赖服务均健康
- MySQL 初始化报 SQL 错误：保留日志，不要反复执行 `up`；失败后的卷可能是半初始化状态，确认无业务数据后才能 `down -v` 重试
- 修改 `.env` 中数据库密码后仍认证失败：MySQL 只在空卷首次创建账号；已有卷需要迁移账号密码，改 `.env` 不会自动修改库内账号
- 页面仍是旧版本：确认 `backend` 镜像已重新构建，并用 `docker compose up -d --build backend` 替换运行容器

## 环境变量说明

参考仓库根目录的 `.env.example`。

主要变量：
- 数据库应用账号：`DB_HOST` `DB_PORT` `DB_HOST_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` `DB_SCHEMA_INIT_MODE`
- Docker 初始化账号：`MYSQL_ROOT_PASSWORD`（只供 MySQL 容器初始化和健康检查，后端不使用）
- Redis：`REDIS_HOST` `REDIS_PORT` `REDIS_HOST_PORT` `CACHE_REDIS_ENABLED`
- RocketMQ：`ROCKETMQ_ENABLED` `ROCKETMQ_NAME_SERVER` `ROCKETMQ_RECOMMENDATION_TOPIC` `ROCKETMQ_RECOMMENDATION_TAG` `ROCKETMQ_PRODUCER_GROUP` `ROCKETMQ_CONSUMER_GROUP`
- 服务端口：`SERVER_PORT` `SERVER_HOST_PORT`
- JWT：`AUTH_JWT_SECRET` `AUTH_JWT_ISSUER`
- AI：`QWEN_ENABLED` `QWEN_BASE_URL` `QWEN_MODEL` `QWEN_API_KEY`
- 文档：`SPRINGDOC_SWAGGER_UI_PATH` `SPRINGDOC_API_DOCS_PATH`

## 测试与构建

后端测试：

```powershell
.\mvnw.cmd test
```

后端打包：

```powershell
.\mvnw.cmd package
```

前端构建：

```powershell
cd frontend
npm run build
```

## 主要接口

### 认证与基础数据
- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/logout`
- `GET /api/meta/options`
- `GET /api/meta/major-options`

### 推荐与历史
- `POST /api/recommendations`
- `POST /api/recommendations/free-text`
- `POST /api/recommendations/free-text/tasks`
- `GET /api/recommendations/free-text/tasks/{taskId}`
- `POST /api/recommendations/final-advice`
- `GET /api/recommendations/schools/{universityId}/majors`
- `GET /api/history`
- `GET /api/history/{id}`
- `DELETE /api/history/{id}`

### 志愿方案
- `POST /api/plans`
- `GET /api/plans`
- `GET /api/plans/current`
- `PUT /api/plans/current`
- `DELETE /api/plans/current`
- `GET /api/plans/{id}`
- `DELETE /api/plans/{id}`

### Agent
- `POST /api/agent/conversations`
- `GET /api/agent/conversations`
- `GET /api/agent/conversations/{conversationId}`
- `POST /api/agent/conversations/{conversationId}/messages`

### 管理接口
- `GET /api/admin/universities`
- `POST /api/admin/universities`
- `PUT /api/admin/universities/{id}`
- `GET /api/admin/majors`
- `POST /api/admin/majors`
- `PUT /api/admin/majors/{id}`
- `GET /api/admin/admission-cutoffs`
- `POST /api/admin/admission-cutoffs`
- `PUT /api/admin/admission-cutoffs/{id}`
- `GET /api/admin/major-admission-cutoffs`
- `POST /api/admin/major-admission-cutoffs`
- `PUT /api/admin/major-admission-cutoffs/{id}`
- `GET /api/admin/users`
- `GET /api/admin/users/overview`
- `GET /api/admin/users/{id}`
- `PUT /api/admin/users/{id}/settings`

所有管理接口都要求 `ADMIN` 角色。用户接口不会返回密码，只允许维护 `role` 和 `enabled`；当前管理员不能停用或降级自己。

## 数据准备说明

- 基础建表与初始化数据：`sql/schema.sql`、`sql/data.sql`
- 分数位次批量导入说明：`sql/score-rank-mapping-guide.md`
- 位次导入 SQL 模板：`sql/import-score-rank-mapping.sql`

## 常见问题

### 1. 前端页面显示空白或旧版本

**原因**：Spring Boot 从 `target/classes/static/` 加载静态资源，而不是 `src/main/resources/static/`。

**解决**：
```powershell
# 方法1：重新编译
.\mvnw.cmd compile

# 方法2：手动复制
Copy-Item -Path src\main\resources\static\* -Destination target\classes\static\ -Recurse -Force
```

### 2. Maven Wrapper 报错 "command not found"

**原因**：Linux/macOS 下 `mvnw` 没有执行权限。

**解决**：
```bash
chmod +x mvnw
./mvnw spring-boot:run
```

### 3. 数据库连接失败

**原因**：MySQL 未启动或配置错误。

**解决**：
1. 推荐执行 `docker compose up -d mysql`，避免连接到本机密码不确定的 MySQL 服务
2. 检查 `.env` 中的 `DB_HOST=localhost`、`DB_PORT=3307`、`DB_USER=zhiyuan` 和 `DB_PASSWORD`
3. 执行 `check-db.bat`，或手动测试：`mysql -h localhost -P 3307 -u zhiyuan -p`
4. 已有 Docker 数据卷不会因修改 `.env` 自动更换账号密码；不要直接删除数据卷，先备份后再决定迁移或重建

### 4. Redis 连接失败

**原因**：Redis 未启动（但可以禁用）。

**解决**：
设置 `CACHE_REDIS_ENABLED=false` 即可禁用 Redis 缓存。

### 5. RocketMQ 连接失败

**原因**：RocketMQ 未启动（本地开发可禁用）。

**解决**：
设置 `ROCKETMQ_ENABLED=false` 即可禁用 RocketMQ，系统会使用同步任务处理。

### 6. AI 功能不可用

**原因**：未配置 API Key（但可以降级使用）。

**解决**：
AI 功能有本地降级机制，即使不配置 API Key，基础的意图识别和工具调用仍然可用。如需完整 AI 功能：
1. 设置 `QWEN_ENABLED=true`
2. 填写 `QWEN_API_KEY`

### 7. 端口被占用

**原因**：8080 端口已被其他程序占用。

**解决**：
Docker Compose 部署修改 `.env` 中的 `SERVER_HOST_PORT`；直接运行 JAR 时修改 `SERVER_PORT`。也可以停止占用端口的程序。

## 当前状态

当前版本已经完成从"普通 CRUD + 简单 AI 调用"向"可展示工程能力的单体 AI 应用项目"的升级，重点体现在：
- 可控推荐逻辑，而不是把结果直接交给大模型
- 受控对话式 Agent，而不是纯聊天外壳
- 完整鉴权、缓存、异步任务、管理端、Docker、测试与文档能力
