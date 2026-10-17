# 智愿AI报考平台 — 项目交接说明

打包日期：2026-08-17 ｜ Git 版本：`2a3bb89`（main，与线上部署一致）
技术栈：Spring Boot 3 (Java 17) + Vue 3 + Vite + Element Plus + MariaDB/MySQL + Redis（RocketMQ 可选）

## 一、文件与目录说明

**frontend/** — Vue 3 前端源码（hash 路由 SPA，橙色 #ff6600 主题，gaokao.cn 风格）

- `src/views/` 页面组件：
  - `HomeView.vue` 首页（轮播 Banner、模拟报志愿面板、热点资讯、快捷入口）
  - `SchoolsView.vue` 查大学、`MajorsView.vue` / `MajorDetailView.vue` 查专业
  - `RankView.vue` 院校排行、`SegmentsView.vue` 一分一段、`EnrollPlanView.vue` 招生计划
  - `ChooseView.vue` 智能选大学（冲稳保测算）
  - `VolunteerView.vue` + `components/VolunteerSheet.vue` 志愿填报（45 志愿位、冲/稳/保三段、模拟填报选校卡、详细/表格双模式、智能填充/排序、风险诊断、方案保存）
  - `NewsView.vue` / `NewsDetailView.vue` 高考资讯频道 + 文章详情（数据源为中国教育在线真实文章）
  - `LoginView.vue`、`ProfileSetupView.vue`、`RecommendationView.vue`（推荐查询）、`AgentView.vue`（问小智 AI 对话）、`HistoryRecordsView.vue`、`PlansView.vue`、`AdminView.vue`（管理后台）
- `src/components/` `GkHeader.vue` 顶部导航、`GkSidePanel.vue` 全站侧栏、`GkSchoolLogo.vue` 校徽、`XiaoZhiAvatar.vue` AI 头像、`AgentWorkspace.vue` 对话工作区、`MajorPickDialog.vue`、`RecommendSchoolRow.vue`、`RecommendationResult.vue`
- `src/utils/` 数据层：`exploreData.js`（院校/专业/一分一段等本地数据，确定性派生）、`volunteerCore.js`（志愿表模型与持久化）、`newsData.js`（11 篇 eol 真实资讯含正文）、`recommendation.js`（鉴权与推荐存取）、`mock.js`
- `src/styles.css` 全局样式（约 11000 行，`gk-*` / `mnz-*` 两套前缀）
- `public/logos/` 20 张院校校徽图片

**src/main/** — Spring Boot 后端

- `java/` 鉴权（JWT）、院校/专业推荐、AI 客户端（OpenAI 兼容，现指向 DeepSeek）、管理端等
- `resources/application*.yml` dev/prod 配置；`resources/static/` 前端构建产物（`npm run build` 自动输出至此，当前为最新）

**其他**

- `sql/schema.sql` + `sql/data.sql` 数据库初始化脚本
- `docker-compose.yml` + `Dockerfile` 全栈容器化部署（mysql/redis/rocketmq/backend 四服务）
- `pom.xml`、`mvnw` Maven 构建
- `README.md`、`PROJECT_PLAN.md`、`AI_CHAT_API_DESIGN.md`、`history.md` 等项目文档
- `check-db.bat` / `init-db.bat` 本地辅助脚本
- 压缩包已排除 `node_modules/`、`target/`、`.git/`（可重新安装/构建）

## 二、本地运行与构建

```
前端:  cd frontend && npm install && npm run dev        # http://localhost:5173
后端:  ./mvnw spring-boot:run                            # dev profile, 需本机 MySQL/Redis
容器:  docker compose up -d                              # 全栈一键起
构建:  cd frontend && npm run build                      # 产物 → src/main/resources/static
       ./mvnw -DskipTests package                        # → target/college-recommendation-0.0.1-SNAPSHOT.jar
```

## 三、公网部署信息（重要）

- 线上地址：**http://8.163.67.24:18080/#/**（阿里云 ECS，Ubuntu，无 Docker）
- 部署形态：systemd 服务 `zhiyuan.service`
  `java -Xmx256m -XX:+UseSerialGC -jar /root/zhiyuan/target/college-recommendation-0.0.1-SNAPSHOT.jar`
- 关键环境：`SPRING_PROFILES_ACTIVE=prod`、`SERVER_PORT=18080`、数据库为本机 MariaDB（库名 `college_recommendation`，用户 `zhiyuan`）、本机 Redis、`ROCKETMQ_ENABLED=false`、`CACHE_REDIS_ENABLED=false`、AI 指向 DeepSeek `deepseek-chat`（API key 在服务器 unit 文件内，不在本仓库）
- 更新流程：本地 `npm run build` → `mvnw -DskipTests package` → scp jar 到服务器 `/root/zhiyuan/target/college-recommendation-NEW.jar` → 备份旧 jar（`backup-日期.jar` 惯例）→ 替换 → `systemctl restart zhiyuan` → 约 20 秒后验证 18080 端口与页面
- 当前线上版本即本包 Git 版本（2026-08-17 部署），含志愿填报模块与高考资讯频道

## 四、Git 仓库

- 上游：`https://github.com/Zoean-z/zhiyuan`（本机凭证无直推权限）
- Fork：`https://github.com/qsm68p75m6-arch/zhiyuan`，改动分支 `feat/gaokao-redesign-20260817`（即本包内容）
