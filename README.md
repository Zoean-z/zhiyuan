# 高考志愿智能推荐与对话式填报助手

一个面向高考志愿填报场景的 AI 应用项目，核心目标不是“把推荐全交给大模型”，而是让 AI 负责自然语言理解与对话编排，后端保留可控、可解释、可审计的推荐逻辑。

项目当前已经具备完整单体工程闭环：
- 用户登录注册、画像维护、推荐历史、志愿方案保存
- 基于分数 / 位次 / 省份 / 科类 / 偏好的院校与专业推荐
- `冲 / 稳 / 保` 概率评分与规则解释
- 管理员维护院校、专业、录取数据
- 对话式 Agent 工作台，可读取画像、生成推荐、查询学校详情、操作当前志愿单

## 项目亮点

### 1. AI 只做抽取和助手，不直接决定推荐结果
- 自由文本输入先由 AI 解析成结构化条件，例如省份、分数、位次、偏好专业、排除专业
- 推荐核心仍由后端规则、概率评分和排序逻辑完成
- 每条结果都会返回推荐理由、风险等级、规则解释，便于展示和追踪

### 2. 从固定阈值升级到基础版“冲稳保概率评分”
- 不再只按固定 `scoreGap / rankGap` 生硬分档
- 结合位次差、分数差、年份波动、偏好命中等因素计算基础概率分
- 再按概率区间映射到 `冲 / 稳 / 保`，结果更接近真实推荐场景

### 3. 做了受控多轮对话 Agent，而不是自由放权
- Agent 通过受控工具调用现有后端能力：`getUserProfile`、`recommendSchools`、`recommendMajors`、`getSchoolDetail`、`addPlanItem`、`savePlan`
- 单轮工具调用上限显式限制，写操作带确认边界
- 工具失败有统一错误分类和兜底提示，不会直接把接口打成 500

### 4. 工程化不是停在 CRUD
- 引入 `Spring Security + JWT + RBAC`
- 引入 `Redis` 做基础元数据缓存、热门推荐缓存、AI 去重保护
- 增加异步自由文本推荐任务、OpenAPI 文档、Docker Compose、本地测试分层

## 技术栈

### 后端
- Java 17
- Spring Boot 3.3.5
- Spring Security
- JWT
- MyBatis-Plus
- MySQL 8
- Redis
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

### 1. 启动后端

```powershell
.\mvnw.cmd spring-boot:run
```

默认访问：
- 应用首页：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- OpenAPI JSON：`http://localhost:8080/v3/api-docs`

### 2. 启动前端开发环境

```powershell
cd frontend
npm install
npm run dev
```

默认地址：
- 前端开发环境：`http://localhost:5173`

说明：
- Vite 会把 `/api` 代理到 `http://localhost:8080`

### 3. 构建前端并交给后端托管

```powershell
cd frontend
npm run build
```

构建产物输出到 `src/main/resources/static`，随后重新启动后端即可通过 `http://localhost:8080` 访问完整页面。

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

### 2. 启动整套服务

```powershell
docker compose up -d --build
```

默认宿主机端口：
- 后端服务：`http://localhost:8080`
- Swagger UI：`http://localhost:8080/swagger-ui.html`
- MySQL：`localhost:3307`
- Redis：`localhost:6380`

说明：
- `mysql` 会自动执行 `sql/schema.sql` 和 `sql/data.sql`
- `backend` 默认使用 `prod` profile 启动
- `backend` 会在 `mysql` 和 `redis` 健康后启动
- 为避免和本机已有 MySQL / Redis 冲突，Compose 使用了单独宿主机端口映射

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
- 数据库：`DB_HOST` `DB_PORT` `DB_HOST_PORT` `DB_NAME` `DB_USER` `DB_PASSWORD`
- Docker 应用账号：`DB_APP_USER` `DB_APP_PASSWORD`
- Redis：`REDIS_HOST` `REDIS_PORT` `REDIS_HOST_PORT` `CACHE_REDIS_ENABLED`
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

## 当前状态

当前版本已经完成从“普通 CRUD + 简单 AI 调用”向“可展示工程能力的单体 AI 应用项目”的升级，重点体现在：
- 可控推荐逻辑，而不是把结果直接交给大模型
- 受控对话式 Agent，而不是纯聊天外壳
- 完整鉴权、缓存、异步任务、管理端、Docker、测试与文档能力
