# 代码审查修复记录（2026-08-22）

- 审查输入：`zhiyuan1-code-review.md`（高危 3 项、中危 7 项、低危 6 项，另含 Agent 专项 10 项）
- 分支：`fix/code-review-20260822`（基于 `feature/admin-console-20260821`）
- 每一条结论都已对照真实代码复核，下表区分“已修”与“暂不修及原因”。

## 一、已修复

| 编号 | 问题 | 修复方式 |
| --- | --- | --- |
| 高危 #1 | `AiRequirementParserService.lastAiResponse` 是单例可变字段，并发下会把其他用户的 AI 原文写进本次 trace | 改为调用栈内的 `AtomicReference`，字段删除 |
| 高危 #2 | JWT 密钥不足 32 字节时自动拼接 `a-z` 补齐，可预测密钥可伪造 token；并且代码/仓库内置了默认密钥 | 删除补齐逻辑，不足 32 字节直接启动失败；去掉构造函数内联默认值；识别到仓库示例密钥时 WARN；`docker-compose.yml` 改为 `${AUTH_JWT_SECRET:?...}` 必填 |
| 高危 #3 | `/v3/api-docs`、`/swagger-ui.html` 在 `anyRequest().permitAll()` 下完全公开，生产 profile 还显式打开 Swagger | 新增 `security.expose-api-docs`（默认 false），文档路径默认 `denyAll()`；`springdoc.api-docs/swagger-ui.enabled` 默认 false，仅 dev profile 打开 |
| 中危 #4 | 明文密码登录兼容分支无开关，生产也接受种子数据里的明文口令 | 新增 `auth.allow-legacy-plaintext-login`（默认 false，prod 硬编码 false，dev/test 为 true），关闭时拒绝并记 WARN |
| 中危 #5 | 注册并发时 “先查后插” 会抛库异常，返回 500 | `register` 捕获 `DuplicateKeyException` 转 409；`GlobalExceptionHandler` 同时增加全局 `DuplicateKeyException -> 409` 兜底 |
| 中危 #6 | 管理后台用户列表硬编码 `LIMIT 200`，数据变多后静默截断 | `findAdminUsers` 改为 `LIMIT/OFFSET` 参数，新增 `countAdminUsers`；接口新增可选 `page`/`size`（size 上限 200）与 `GET /api/admin/users/count` |
| 中危 #7 | 注册 DTO 只有 `@NotBlank`，超长输入直接撞库字段上限 | `RegisterRequest` 补 `@Size`：用户名 <= 64、密码 6-64、省份 <= 32 |
| 中危 #8 | `.env.example` 与 compose/prod 的 Redis、密钥等配置不一致 | `CACHE_REDIS_ENABLED` 统一为 true，补全新增开关与超时变量，密钥改为空值 + 生成命令注释 |
| 中危 #9 | 录取数据表缺唯一约束，重复导入会产生重复行 | 新增 `sql/upgrade-20260822-unique-keys.sql`（幂等，存量重复时自动跳过）并挂载到 MySQL initdb |
| 中危 #10 | AI 调用无超时，上游挂住会耗尽请求线程 | `AiChatClient` 新增 connect/read 超时（默认 5s/30s，可配置） |
| 低危 #12 | 资料里的省份无条件覆盖文本解析出的考生省份 | 改为仅在解析结果为空时回填 |
| 低危 #13 | 分数正则 `([3-7]\d{2})` 会把“600公里”“600人”当成高考分 | 改为三级策略：“NNN分” > “考了/成绩/总分 NNN” > 独立三位数（排除量词、限 300-750） |
| 低危 #前端 | 管理员登录后被路由守卫死锁在 `/admin`，无法浏览任何其他页面 | 删除强制重定向，仅保留“登录后默认进后台” |
| Agent #4 | `removePlanItem` 缺 `selectionIndex` 时默认删第 1 条 | 删除操作必传 `selectionIndex`，缺失返回 400；读取类工具保留默认首条 |
| 运维 | backend 健康检查探测 `/v3/api-docs`（文档已关闭则永远 unhealthy） | 换为探测公开业务接口 `/api/meta/options` |

## 二、升级注意事项（破坛式变更）

1. **`AUTH_JWT_SECRET` 现在是必填项**。`docker compose up` 前先生成：
   ```bash
   openssl rand -base64 48
   ```
   密钥解码后不足 32 字节时应用会拒绝启动（而不是您默默使用弱密钥）。本地 `mvn spring-boot:run` 仍可使用 `application.yml` 里的开发默认值。
2. **接口文档默认关闭**。需要 Swagger 时同时设置 `SECURITY_EXPOSE_API_DOCS=true`、`SPRINGDOC_API_DOCS_ENABLED=true`、`SPRINGDOC_SWAGGER_UI_ENABLED=true`（dev profile 已默认打开）。
3. **明文口令登录默认关闭**。如果生产库里仍有 `sql/data.sql` 带来的明文口令账号，请先重置密码，或临时设置 `AUTH_ALLOW_LEGACY_PLAINTEXT_LOGIN=true` 完成一次登录（登录成功会自动改写为 BCrypt）后再关闭。
4. **唯一索引需对存量库手动执行**：
   ```bash
   mysql -u root -p college_recommendation < sql/upgrade-20260822-unique-keys.sql
   ```
   脚本在发现存量重复数据时会输出 `skipped_duplicates` 并跳过，需先去重（脚本头部附去重 SQL 示例）。`src/main/resources/db/schema.sql`（应用启动时执行的那份）未改动，避免 `continue-on-error: false` 下因 ALTER 不幂等而启动失败。

## 三、暂不修及原因

| 编号 | 问题 | 不修原因 |
| --- | --- | --- |
| Agent #1 | `addPlanItem`/`savePlan` 缺二步确认 | `AgentControllerTest` 有 4 个用例断言单步完成；改为二步确认属于产品行为变更，需先对齐需求与测试 |
| Agent #2 | 删除确认无时间窗口，过期“确认”仍生效 | 需改写 `AgentChatService` 会话状态机，与待确认流程耦合较深，建议单独一轮改动并补测试 |
| Agent #3/#15 | `MAX_TOOL_CALLS_PER_TURN = 1` 使多工具循环分支成为死代码 | 纯死代码，无运行时影响；清理与“单轮单工具”策略调整一并做更合理 |
| 中危 #6（其余表） | universities/majors/录取分数管理接口也有 LIMIT 限制 | 本轮只改用户列表（改动面可控）；其余 4 个管理模块建议统一引入分页 DTO 后一次改完 |
| 低危 #11 | 前端多个页面仍用 mock 数据 | 属于未完成功能，需后端新接口配合，不属于“修 bug”范畴 |
| 低危 #14 | MAJOR_FIRST 模式会按专业关键词 N 次重算推荐 | 需要重构推荐引擎的批量查询能力，属于性能优化，风险高于收益 |
| 低危 #16 | 登录附带更新资料、`RecommendationService` 重复代码、`updateSettings` 全行回写 | 登录附带更新是前端当前依赖的行为（已加注释说明）；其余为可读性重构，不在本轮修复范围 |

## 四、建议的验证步骤

本次修改在无 JDK/Maven 的环境下完成，**未执行编译与测试**，合并前请至少跑一遍：

```bash
mvn -q clean test
cp .env.example .env && printf 'AUTH_JWT_SECRET=%s\n' "$(openssl rand -base64 48)" >> .env
docker compose up -d --build
curl -f http://127.0.0.1:8080/api/meta/options
```

重点回归：登录/注册（含重名）、自由文本推荐（含“我是XX考生”与带量词的数字）、Agent 删除计划项、管理后台用户列表与分页。
