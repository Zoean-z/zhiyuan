# 项目记录

## 项目概览

这是一个高考志愿推荐系统，当前采用：

- 后端：Spring Boot 3 + MyBatis-Plus
- 前端：Vue 3 + Vite + Element Plus
- 数据库：MySQL

当前系统已经具备的核心能力：

- 用户登录
- 分数推荐
- 文本推荐
- AI 总结
- 推荐结果展示
- 历史记录

## 当前主要页面

- 推荐查询
  - 支持分数查询
  - 支持文本查询
  - 展示冲刺 / 稳妥 / 保底结果
- 历史记录
  - 展示用户历史查询
  - 支持查看历史结果详情
- 志愿方案
  - 展示用户保存的志愿方案
  - 支持查看方案详情

## 当前后端主要接口

- `/api/auth/login`
- `/api/auth/logout`
- `/api/recommendations`
- `/api/recommendations/free-text`
- `/api/recommendations/final-advice`
- `/api/history`
- `/api/history/{id}`
- `/api/plans`
- `/api/plans/{id}`

## 本次修改：新增“志愿方案”功能

### 目标

为已登录用户提供“保存推荐结果为志愿方案”的能力，并支持后续查看。

### 后端实现

新增数据表：

- `application_plan`
  - `id`
  - `user_id`
  - `plan_name`
  - `source_type`
  - `source_query`
  - `result_json`
  - `ai_summary`
  - `created_at`

新增能力：

- 保存方案：`POST /api/plans`
- 查询当前用户的方案列表：`GET /api/plans`
- 查询当前用户的方案详情：`GET /api/plans/{id}`

实现要点：

- 复用了现有登录态和 `UserContext`
- 方案列表和详情都按当前用户隔离
- `source_type` 目前支持：
  - `score`
  - `text`
- 推荐接口本身没有改算法

### 前端实现

在推荐结果区域新增：

- 按钮：`保存为志愿方案`

交互流程：

1. 用户先生成推荐结果
2. 点击 `保存为志愿方案`
3. 输入方案名称
4. 保存成功后显示中文成功提示

新增页面：

- `志愿方案`

页面能力：

- 展示方案名称
- 展示创建时间
- 展示来源类型
- 展示来源内容
- 支持查看方案详情

详情展示方式：

- 使用弹窗展示
- 复用了现有推荐结果组件
- 如果能解析存储结果，则展示冲刺 / 稳妥 / 保底和 AI 总结
- 如果不能解析，则回退展示原始 JSON

## 本次新增/修改的主要文件

### 后端

- `sql/schema.sql`
- `src/test/resources/schema-h2.sql`
- `src/main/java/com/zhiyuan/college/config/WebConfig.java`
- `src/main/java/com/zhiyuan/college/controller/ApplicationPlanController.java`
- `src/main/java/com/zhiyuan/college/service/ApplicationPlanService.java`
- `src/main/java/com/zhiyuan/college/mapper/ApplicationPlanMapper.java`
- `src/main/java/com/zhiyuan/college/model/entity/ApplicationPlan.java`
- `src/main/java/com/zhiyuan/college/model/dto/ApplicationPlanCreateRequest.java`
- `src/main/java/com/zhiyuan/college/model/dto/ApplicationPlanRecordResponse.java`
- `src/main/java/com/zhiyuan/college/model/dto/ApplicationPlanDetailResponse.java`
- `src/test/java/com/zhiyuan/college/controller/RecommendationControllerTest.java`

### 前端

- `frontend/src/App.vue`
- `frontend/src/utils/recommendation.js`
- `frontend/src/components/RecommendationResult.vue`
- `frontend/src/components/ApplicationPlanView.vue`
- `frontend/src/components/HistoryView.vue`
- `frontend/src/components/AiSummaryPanel.vue`
- `frontend/src/components/UniversityCard.vue`
- `frontend/src/styles.css`

### 其他

- `src/main/resources/static/*`
  - 由前端构建产物更新

## 启动方式

后端：

```bat
.\mvnw.cmd spring-boot:run
```

前端开发：

```bat
cd frontend
npm run dev
```

前端构建：

```bat
cd frontend
npm run build
```

## 验证结果

已完成验证：

- `.\mvnw.cmd test -q` 通过
- `cd frontend && npm run build` 通过
- 数据库密码当前统一为 `123456`

## 后续可继续做但本次未做

- 把前端构建进一步接入 Maven 打包流程
- 为志愿方案增加删除/重命名
- 为方案保存添加更细的前端校验

---

## 最近补充修改：学校层次标签改为多标签并存

### 目标

把原本单一的学校层次表示方式，调整为可并存标签方式，用于更准确表示：

- `985`
- `211`
- `双一流`

要求是三类标签可以同时存在，普通院校则三个标签均为 false。

### 后端实现

学校相关返回结构新增并使用：

- `is985`
- `is211`
- `isDoubleFirstClass`
- `schoolTags`

实现要点：

- 保留了原有 `tier / universityTier` 字段用于兼容旧代码和旧数据
- 推荐结果返回时，额外携带三个布尔字段和 `schoolTags`
- 学校优先、专业优先推荐结果都已兼容新字段
- 自由文本推荐中 `schoolLevels` 的筛选逻辑改为基于布尔标签判断
- “普通”学校按 `is985=false` 且 `is211=false` 且 `isDoubleFirstClass=false` 处理

兼容处理：

- 当前查询层仍可通过旧 `tier` 数据推导新布尔标签
- 初始化数据和测试数据也已补齐布尔字段
- 保证旧字段未删除、项目仍可正常运行

### 前端实现

推荐结果卡片的学校标签展示已调整为支持同时展示多个标签：

- 同一学校可同时显示 `985`
- 同一学校可同时显示 `211`
- 同一学校可同时显示 `双一流`

实现要点：

- 前端优先读取后端返回的 `is985 / is211 / isDoubleFirstClass / schoolTags`
- 如果读取到旧数据，则仍可根据 `universityTier` 做兼容推导
- 学校优先、专业优先、历史结果、方案详情都可正常展示多标签

### 本次涉及的主要文件

#### 后端

- `sql/schema.sql`
- `sql/data.sql`
- `src/test/resources/schema-h2.sql`
- `src/test/resources/data-h2.sql`
- `src/main/java/com/zhiyuan/college/model/entity/University.java`
- `src/main/java/com/zhiyuan/college/model/dto/AdmissionCutoffWithUniversity.java`
- `src/main/java/com/zhiyuan/college/model/dto/RecommendationItemResponse.java`
- `src/main/java/com/zhiyuan/college/mapper/AdmissionCutoffMapper.java`
- `src/main/java/com/zhiyuan/college/mapper/MajorAdmissionCutoffMapper.java`
- `src/main/java/com/zhiyuan/college/service/RecommendationService.java`
- `src/main/java/com/zhiyuan/college/service/FreeTextRecommendationService.java`
- `src/main/java/com/zhiyuan/college/util/UniversityTagUtils.java`
- `src/test/java/com/zhiyuan/college/controller/RecommendationControllerTest.java`

#### 前端

- `frontend/src/utils/recommendation.js`
- `frontend/src/components/UniversityCard.vue`
- `frontend/src/App.vue`

### 验证结果

- `.\mvnw.cmd test -q` 通过
- `cd frontend && npm run build` 通过

## 最近补充修改：前端错误提示中文化与常用提示统一

### 目标

统一当前前端用户可见的错误、成功、失败、空状态提示文案，避免直接向用户暴露英文异常和技术错误。

### 实现内容

新增轻量提示与错误处理工具：

- `frontend/src/utils/ui.js`

统一处理内容包括：

- 请求状态码错误中文化
  - `400` → 请求参数有误
  - `401` → 登录状态已失效，请重新登录
  - `403` → 无权限执行该操作
  - `404` → 请求资源不存在
  - `500` → 服务器异常，请稍后重试
- 网络异常中文化
  - 网络连接异常，请检查网络后重试
- 超时中文化
  - 请求超时，请稍后重试
- 未知异常兜底
  - 操作失败，请稍后重试
  - 系统开小差了，请稍后重试

### 前端改动要点

- `App.vue` 中的请求入口增加了统一错误映射与超时处理
- 登录、分数推荐、文本推荐、保存方案等常见入口增加了最小表单校验提示
- 常见成功提示已统一，例如：
  - 加入方案成功
  - 保存方案成功
  - 删除历史记录成功
  - 删除方案成功
- 常见失败提示已统一，例如：
  - 查询推荐结果失败，请稍后重试
  - 保存方案失败，请稍后重试
  - 删除历史记录失败，请稍后重试
  - 删除方案失败，请稍后重试
- 常见空状态提示已统一，例如：
  - 暂无历史记录
  - 暂无志愿方案
  - 当前方案为空
  - 暂无推荐结果
  - 暂无 AI 总结

### 本次涉及的主要文件

- `frontend/src/utils/ui.js`
- `frontend/src/App.vue`
- `frontend/src/components/HistoryView.vue`
- `frontend/src/components/ApplicationPlanView.vue`
- `frontend/src/components/CurrentPlanPanel.vue`
- `frontend/src/components/RecommendationResult.vue`
- `frontend/src/components/AiSummaryPanel.vue`

### 验证结果

- `cd frontend && npm run build` 通过
