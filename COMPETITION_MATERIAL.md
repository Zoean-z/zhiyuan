# 2026年中南林业科技大学湖南省大学生计算机程序设计大赛（应用开发类）校赛

## 参赛作品：智愿AI报考平台

---

## 一、作品概述

**作品名称：** 智愿AI报考平台

**作品类型：** Web 应用开发类

**作品简介：** 一个面向高考志愿填报场景的 AI 应用系统，核心目标是让 AI 负责自然语言理解与对话编排，后端保留可控、可解释、可审计的推荐逻辑。系统具备完整的用户认证、智能推荐、志愿方案管理和 AI 对话功能。

---

## 二、技术架构

### 2.1 技术栈详情

| 层级 | 技术 | 版本 |
|------|------|------|
| **后端框架** | Spring Boot | 3.3.5 |
| **编程语言** | Java | 17 |
| **安全框架** | Spring Security + JWT | jjwt 0.12.6 |
| **ORM 框架** | MyBatis-Plus | 3.5.7 |
| **数据库** | MySQL | 8.4 |
| **缓存** | Redis | 7.4 |
| **消息队列** | Apache RocketMQ | 5.3.2 |
| **AI 模型** | DeepSeek API | v4-flash |
| **API 文档** | SpringDoc OpenAPI | 2.6.0 |
| **前端框架** | Vue 3 | 3.5.22 |
| **构建工具** | Vite | 7.1.10 |
| **UI 组件库** | Element Plus | 2.11.4 |
| **容器化** | Docker + Docker Compose | - |

### 2.2 系统架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        用户界面层 (Vue 3)                        │
├─────────────────────────────────────────────────────────────────┤
│  推荐查询  │  AI对话  │  历史记录  │  志愿方案  │  管理后台    │
└─────────────────────────────────────────────────────────────────┘
                              │
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                      API 网关层 (Spring Boot)                    │
├─────────────────────────────────────────────────────────────────┤
│  JWT认证  │  权限控制  │  请求路由  │  异常处理  │  Swagger文档  │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│   推荐服务层    │ │   Agent服务层   │ │   管理服务层    │
├─────────────────┤ ├─────────────────┤ ├─────────────────┤
│ • 分数推荐      │ │ • 意图识别      │ │ • 院校管理      │
│ • 文本推荐      │ │ • 工具调用      │ │ • 专业管理      │
│ • 概率评分      │ │ • 对话管理      │ │ • 录取线管理    │
│ • 异步任务      │ │ • 工具注册表    │ │ • 用户管理      │
└─────────────────┘ └─────────────────┘ └─────────────────┘
          │                   │                   │
          ▼                   ▼                   ▼
┌─────────────────────────────────────────────────────────────────┐
│                        数据访问层 (MyBatis-Plus)                 │
├─────────────────────────────────────────────────────────────────┤
│  12个Mapper  │  14个实体类  │  34个DTO  │  4个枚举              │
└─────────────────────────────────────────────────────────────────┘
                              │
          ┌───────────────────┼───────────────────┐
          ▼                   ▼                   ▼
┌─────────────────┐ ┌─────────────────┐ ┌─────────────────┐
│     MySQL       │ │     Redis       │ │   RocketMQ      │
│   (主数据存储)   │ │   (缓存层)      │ │  (异步任务队列)  │
└─────────────────┘ └─────────────────┘ └─────────────────┘
```

---

## 三、数据库设计

### 3.1 数据库表结构（共12张表）

| 表名 | 用途 | 主要字段 |
|------|------|----------|
| `university` | 院校主数据 | id, name, province, tier, is_985, is_211, is_double_first_class, tags |
| `major` | 专业主数据 | id, name, category, degree_type, tags, subject_requirement |
| `admission_cutoff` | 院校录取线 | university_id, admission_year, province, subject_type, cutoff_score, min_rank |
| `major_admission_cutoff` | 专业录取线 | university_id, major_id, major_name, admission_year, cutoff_score, min_rank |
| `score_rank_mapping` | 分数位次映射 | mapping_year, province, subject_type, score, rank_value |
| `users` | 用户账户 | id, username, password, score, subject_type, exam_province, role |
| `recommendation_log` | 推荐历史 | user_id, query_type, query_content, result_json, created_at |
| `application_plan` | 志愿方案 | user_id, plan_name, source_type, source_query, result_json, ai_summary |
| `recommendation_task` | 异步任务 | user_id, request_id, status, result_count, duration_ms, error_message |
| `ai_parse_log` | AI解析日志 | task_id, provider, model_name, success_flag, raw_response, parsed_json |
| `agent_conversation` | Agent对话 | user_id, title, status, last_message_at, message_count |
| `agent_message` | Agent消息 | conversation_id, role, message_type, content, tool_name, payload_json |

### 3.2 数据库设计特点

1. **主数据与事实数据分离：** 院校/专业主数据与录取线事实数据分开存储，便于维护和查询
2. **幂等Schema迁移：** 使用 `INFORMATION_SCHEMA` 检查和条件 `ALTER TABLE` 语句，支持安全重复执行
3. **外键约束：** 录取线表通过外键关联院校和专业表，保证数据完整性
4. **索引优化：** 为常用查询字段（user_id, created_at, status等）创建索引

---

## 四、API接口设计

### 4.1 认证与用户模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/auth/login` | POST | 用户登录，返回JWT Token |
| `/api/auth/register` | POST | 用户注册 |
| `/api/auth/profile` | POST | 完善用户画像（分数、省份、科类） |
| `/api/auth/logout` | POST | 用户登出 |

### 4.2 推荐查询模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/recommendations` | POST | 分数推荐（结构化表单输入） |
| `/api/recommendations/free-text` | POST | 自然语言推荐（同步） |
| `/api/recommendations/free-text/tasks` | POST | 提交异步推荐任务 |
| `/api/recommendations/free-text/tasks/{taskId}` | GET | 查询异步任务状态 |
| `/api/recommendations/final-advice` | POST | 生成AI总结建议 |
| `/api/recommendations/schools/{universityId}/majors` | GET | 获取院校详情及专业录取线 |

### 4.3 历史记录模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/history` | GET | 获取推荐历史列表 |
| `/api/history/{id}` | GET | 获取历史详情 |
| `/api/history/{id}` | DELETE | 删除历史记录 |

### 4.4 志愿方案模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/plans` | POST | 保存新方案 |
| `/api/plans` | GET | 获取方案列表 |
| `/api/plans/current` | GET | 获取当前草稿 |
| `/api/plans/current` | PUT | 更新当前草稿 |
| `/api/plans/current` | DELETE | 删除当前草稿 |
| `/api/plans/{id}` | GET | 获取方案详情 |
| `/api/plans/{id}` | PUT | 更新方案 |
| `/api/plans/{id}` | DELETE | 删除方案 |

### 4.5 AI对话模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/agent/conversations` | POST | 创建新对话 |
| `/api/agent/conversations` | GET | 获取对话列表 |
| `/api/agent/conversations/{id}` | GET | 获取对话详情 |
| `/api/agent/conversations/{id}/messages` | POST | 发送消息（触发Agent工具链） |

### 4.6 管理后台模块

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/admin/universities` | GET/POST | 院校列表/新增 |
| `/api/admin/universities/{id}` | PUT | 更新院校 |
| `/api/admin/majors` | GET/POST | 专业列表/新增 |
| `/api/admin/majors/{id}` | PUT | 更新专业 |
| `/api/admin/admission-cutoffs` | GET/POST | 院校录取线列表/新增 |
| `/api/admin/admission-cutoffs/{id}` | PUT | 更新院校录取线 |
| `/api/admin/major-admission-cutoffs` | GET/POST | 专业录取线列表/新增 |
| `/api/admin/major-admission-cutoffs/{id}` | PUT | 更新专业录取线 |

---

## 五、核心功能详解

### 5.1 智能推荐系统

**双模式推荐：**
1. **分数推荐：** 用户通过结构化表单输入省份、科类、分数、推荐模式（学校优先/专业优先）
2. **文本推荐：** 用户输入自然语言，AI解析为结构化条件后进行推荐

**冲/稳/保概率评分算法：**
```java
// 概率评分公式
probability = rankWeight * rankScore + scoreWeight * scoreScore

// 冲稳保映射
RUSH:    35-54分 (冲刺院校)
SAFE:    55-74分 (稳妥院校)
GUARANTEE: 75-100分 (保底院校)
```

**分数位次转换：**
- 支持不同省份、年份、科类的分数位次映射
- 用于将用户分数转换为位次，或将位次转换为分数

### 5.2 AI自然语言理解

**AI解析流程：**
1. 用户输入自然语言文本
2. AI模型（DeepSeek v4-flash）解析为结构化条件
3. 提取：省份、分数、位次、偏好专业、排除专业等
4. 后端规则补全和参数校验
5. 执行推荐算法

**AI解析示例：**
```
输入： "我是浙江理科考生，630分，想学计算机专业"
输出： {
  "province": "浙江",
  "subjectType": "PHYSICS",
  "score": 630,
  "majorKeyword": "计算机",
  "recommendationMode": "MAJOR_FIRST"
}
```

### 5.3 受控Agent对话系统

**Agent工具清单（10个）：**
1. `getUserProfile` - 获取用户画像
2. `getCurrentPlan` - 获取当前志愿方案
3. `getSchoolDetail` - 获取院校详情
4. `getSchoolDetailByName` - 按名称查询院校
5. `recommendSchools` - 生成学校推荐
6. `recommendMajors` - 生成专业推荐
7. `addPlanItem` - 加入志愿单
8. `removePlanItem` - 从志愿单移除
9. `savePlan` - 保存方案
10. `reply` - 直接回复用户

**Agent决策流程：**
1. 用户发送消息
2. `AgentDecisionService` 先尝试本地正则匹配
3. 若AI启用，调用AI模型生成决策JSON
4. AI返回 `{action, reply, toolArgs}`
5. `AgentToolExecutor` 执行对应工具
6. 返回结果给用户

**安全机制：**
- 单轮工具调用上限（1次）
- 写操作需用户确认
- 工具失败有统一错误分类和兜底提示

### 5.4 异步任务队列

**RocketMQ异步任务流程：**
1. 接口创建 `PENDING` 任务
2. 将任务ID、用户ID、请求投递到RocketMQ
3. Consumer通过 `PENDING -> RUNNING` 条件抢占任务
4. 执行推荐计算
5. 成功标记为 `COMPLETED`，失败标记为 `FAILED`
6. 超时任务允许重新抢占

**状态机：**
```
PENDING → RUNNING → COMPLETED
                  → FAILED
```

### 5.5 志愿方案管理

**方案生命周期：**
1. 用户查询推荐结果
2. 选择感兴趣的院校/专业
3. 加入当前草稿（自动保存）
4. 可随时查看、编辑草稿
5. 命名保存为正式方案
6. 支持多方案管理

**方案数据结构：**
```json
{
  "recommendationMode": "SCHOOL_FIRST",
  "rush": [...],      // 冲刺院校
  "safe": [...],      // 稳妥院校
  "guarantee": [...], // 保底院校
  "summary": "...",
  "aiSummary": "...",
  "finalAdvice": "...",
  "tips": [...]
}
```

---

## 六、前端页面设计

### 6.1 页面结构

| 页面 | 路由 | 功能 |
|------|------|------|
| 登录页 | `/login` | 用户登录/注册 |
| 完善信息页 | `/profile-setup` | 填写分数、省份、科类 |
| 推荐查询页 | `/recommend` | 主推荐功能入口 |
| AI对话页 | `/agent` | Agent对话工作台 |
| 历史记录页 | `/history` | 查看推荐历史 |
| 志愿方案页 | `/plans` | 管理志愿方案 |

### 6.2 UI设计特点

1. **响应式布局：** 适配桌面和移动端
2. **侧边栏导航：** 固定左侧导航栏，清晰的功能分区
3. **卡片式设计：** 统计卡片、结果卡片、方案卡片
4. **圆角标签：** 冲刺（红色）、稳妥（黄色）、保底（绿色）
5. **自定义按钮：** 圆角药丸形状，统一的视觉风格
6. **骨架屏加载：** 优雅的加载状态展示

---

## 七、部署方案

### 7.1 Docker Compose 部署（推荐）

**服务清单：**
| 服务 | 镜像 | 端口 |
|------|------|------|
| mysql | mysql:8.4 | 3307 |
| redis | redis:7.4-alpine | 6380 |
| rocketmq-nameserver | apache/rocketmq:5.3.2 | 9876 |
| rocketmq-broker | apache/rocketmq:5.3.2 | 10911 |
| backend | 自定义Dockerfile | 8080 |

**启动命令：**
```bash
# 1. 配置环境变量
cp .env.example .env

# 2. 启动服务
docker compose up -d --build

# 3. 查看状态
docker compose ps
```

### 7.2 本地开发部署

**后端启动：**
```bash
# Windows
.\mvnw.cmd spring-boot:run

# macOS/Linux
./mvnw spring-boot:run
```

**前端启动：**
```bash
cd frontend
npm install
npm run dev        # 正常模式（需要后端）
npm run dev:mock   # 演示模式（无需后端）
```

---

## 八、项目亮点总结

### 8.1 技术创新点

1. **AI与规则引擎结合：** AI负责自然语言理解，后端规则引擎负责推荐计算，兼顾灵活性和可控性
2. **受控Agent架构：** 10个白名单工具，单轮调用限制，写操作确认机制，避免AI失控
3. **异步任务队列：** 基于RocketMQ的异步推荐，支持任务状态追踪、失败重试、超时回收
4. **幂等Schema迁移：** 数据库结构变更可安全重复执行，支持灰度升级

### 8.2 工程实践亮点

1. **分层架构清晰：** Controller → Service → Mapper → Database，职责分明
2. **主数据与事实数据分离：** 院校/专业主数据与录取线数据分开存储
3. **特征开关设计：** Redis缓存、RocketMQ、AI功能均可独立开关
4. **优雅降级：** AI不可用时自动降级到本地规则引擎
5. **完整文档：** Swagger UI自动生成API文档

### 8.3 用户体验亮点

1. **双模式推荐：** 支持表单输入和自然语言输入
2. **实时概率展示：** 每条推荐结果显示冲刺/稳妥/保底概率
3. **AI对话助手：** 自然语言交互，支持查看画像、生成推荐、管理方案
4. **方案草稿自动保存：** 防止用户数据丢失
5. **演示模式：** 无需后端即可展示前端效果

---

## 九、代码量统计

| 类型 | 文件数 | 代码行数（约） |
|------|--------|----------------|
| Java后端 | 80+ | 8,000+ |
| Vue前端 | 15+ | 5,000+ |
| SQL脚本 | 3 | 500+ |
| 配置文件 | 10+ | 300+ |
| **总计** | **110+** | **14,000+** |

---

## 十、团队分工建议

| 角色 | 负责内容 | 文档输出 |
|------|----------|----------|
| 后端开发 | Spring Boot服务、数据库设计、API实现 | 概要设计说明书、详细设计说明书、数据库设计说明书 |
| 前端开发 | Vue界面、组件开发、交互实现 | 软件界面设计书、用户操作手册 |
| AI/算法 | 推荐算法、Agent设计、NLU实现 | 功能需求说明书 |
| 测试/部署 | Docker部署、功能测试、性能测试 | 测试报告 |

---

## 十一、提交材料清单

根据比赛要求，需要提交以下材料：

- [ ] 功能需求说明书
- [ ] 概要设计说明书
- [ ] 详细设计说明书
- [ ] 数据库设计说明书
- [ ] 软件界面设计书
- [ ] 用户操作手册
- [ ] 全部源程序代码及编译后的可执行文件
- [ ] 介绍整个作品的PPT

---

**文档版本：** v1.0
**更新日期：** 2026-08-07
**项目地址：** https://github.com/Zoean-z/zhiyuan
