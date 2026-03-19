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
- 数据库密码当前统一为 `1234`

## 后续可继续做但本次未做

- 把前端构建进一步接入 Maven 打包流程
- 为志愿方案增加删除/重命名
- 为方案保存添加更细的前端校验
