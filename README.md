# 高考志愿智能推荐与对话式填报助手

一个面向高考志愿填报场景的 AI 应用项目，核心目标不是"把推荐全交给大模型"，而是让 AI 负责自然语言理解与对话编排，后端保留可控、可解释、可审计的推荐逻辑。

项目当前已经具备完整单体工程闭环：
- 用户登录注册、画像维护、推荐历史、志愿方案保存
- 基于分数 / 位次 / 省份 / 科类 / 偏好的院校与专业推荐
- `冲 / 稳 / 保` 概率评分与规则解释
- 基于 RocketMQ 的自由文本推荐异步任务、失败重试与状态查询
- 管理员维护院校、专业、录取数据
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
  -e MYSQL_ROOT_PASSWORD=123456 `
  -e MYSQL_DATABASE=college_recommendation `
  -p 3306:3306 `
  mysql:8.4
```

### 2. 启动后端

```powershell
# Windows
.\mvnw.cmd spring-boot:run

# macOS / Linux
./mvnw spring-boot:run
```

默认访问：
- 应用首页：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

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
- 支持所有页面的基本功能展示
- 默认账号：用户名 `testuser`，密码任意
- 管理员账号：用户名 `admin`，密码 `admin123`

### 4. 构建前端并交给后端托管

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

### 1. 准备环境变量

```powershell
Copy-Item .env.example .env
```

至少需要检查这些字段：
- `DB_PASSWORD`
- `DB_APP_USER`
- `DB_APP_PASSWORD`
- `AUTH_JWT_SECRET`
- `QWEN_ENABLED`
- `QWEN_API_KEY`

没有配置 AI Key 时保持 `QWEN_ENABLED=false`，系统仍可使用本地规则完成登录、推荐、志愿表和 Agent 基础工具流程；填入有效 Key 后再改为 `true`。

### 2. 启动整套服务

```powershell
docker compose up -d --build
```

默认宿主机端口：
- 后端服务：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- MySQL：`localhost:3307`
- Redis：`localhost:6380`
- RocketMQ NameServer：`localhost:9876`
- RocketMQ Broker：`localhost:10911`

说明：
- `mysql` 会自动执行 `sql/schema.sql` 和 `sql/data.sql`
- `backend` 启动时会再次执行幂等 schema 校验，因此复用旧数据卷时也能补齐新增表和兼容字段
- `backend` 默认使用 `prod` profile 启动
- `backend` 会在 `mysql`、`redis` 和 RocketMQ Broker 健康后启动
- Compose 会先初始化 RocketMQ 持久化卷权限，再启动 NameServer 与 Broker
- 为避免和本机已有 MySQL / Redis 冲突，Compose 使用了单独宿主机端口映射
- MySQL、Redis 和 RocketMQ 的宿主机端口默认只绑定 `127.0.0.1`，远程服务器只需要对外开放应用端口
- `backend` 提供容器健康检查，可用 `docker compose ps` 确认状态为 `healthy`

### 3. 远程服务器部署

服务器安装 Docker Engine 与 Compose 插件后执行：

```bash
git clone <仓库地址> zhiyuan
cd zhiyuan
cp .env.example .env
```

编辑 `.env`，至少替换 `DB_PASSWORD`、`DB_APP_PASSWORD` 和 `AUTH_JWT_SECRET`；需要真实 AI 对话时再填写 `QWEN_API_KEY` 并设置 `QWEN_ENABLED=true`。然后启动：

```bash
docker compose up -d --build
docker compose ps
docker compose logs --tail=200 backend
```

直接访问 `http://服务器公网IP:8080`。云服务器安全组和系统防火墙只需放行 TCP 8080；MySQL、Redis、RocketMQ 不需要开放公网端口。已有域名时，可让 Nginx/Caddy 反向代理到 `127.0.0.1:8080` 并配置 HTTPS。

更新部署：

```bash
git pull
docker compose up -d --build
```

停止服务：

```powershell
docker compose down
```

删除数据卷：

```powershell
docker compose down -v
```

## 环境变量说明

参考仓库根目录的 `.env.example`。

主要变量：
- 数据库：`DB_HOST` `DB_PORT` `DB_HOST_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD` `DB_SCHEMA_INIT_MODE`
- Docker 应用账号：`DB_APP_USER` `DB_APP_PASSWORD`
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
- `GET /api/admin/majors`
- `GET /api/admin/admission-cutoffs`
- `GET /api/admin/major-admission-cutoffs`

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
1. 检查 MySQL 是否运行：`docker ps | grep mysql`
2. 检查 `.env` 中的 `DB_HOST`、`DB_PORT`、`DB_PASSWORD` 是否正确
3. 手动测试连接：`mysql -h localhost -P 3306 -u root -p`

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
修改 `.env` 中的 `SERVER_PORT` 和 `SERVER_HOST_PORT`，或停止占用端口的程序。

## 当前状态

当前版本已经完成从"普通 CRUD + 简单 AI 调用"向"可展示工程能力的单体 AI 应用项目"的升级，重点体现在：
- 可控推荐逻辑，而不是把结果直接交给大模型
- 受控对话式 Agent，而不是纯聊天外壳
- 完整鉴权、缓存、异步任务、管理端、Docker、测试与文档能力
