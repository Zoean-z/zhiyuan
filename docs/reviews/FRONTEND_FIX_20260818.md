# 前端六大问题修复 + 前后端联调说明（2026-08-18）

> 基准：`feat/gaokao-redesign-20260817`（打包版本 2a3bb89）
> 设计参考：https://mnzy.gaokao.cn/ （已舍弃 VIP 与复杂功能）
> 本次只改前端，**后端 Java 代码、数据库、接口完全未动**

---

## 一、根因：同一个“分数→位次→概率”在前端有 5 套公式

审计后发现，Word 文档里“分数没用上、没有位次对比、概率是啥”这几条，其实是同一个根因：

| 位置 | 原来的写法 | 600 分算出的位次 |
| --- | --- | --- |
| HomeView | `780000 * (1 - score/760)^1.6` | ≈ 61,000 |
| VolunteerView | `(720 - score) * 240` | 28,800 |
| VolunteerSheet | `780000 * (1 - score/760)^1.6` | ≈ 61,000 |
| RecommendSchoolRow | `(720 - score) * 240` | 28,800 |
| exploreData.buildChooseResults | `701 - rank*7` / `7200 + (rank-1)*4860` | — |

同时院校最低分也有三套（`698 - id*3`、`calLine`、`42 + (id*37)%52` 直接当概率）。
所以本次先做了两个“单一真相”模块，再改页面：

- **`frontend/src/utils/scoreModel.js`**（新增，344 行）
  分省份、分科类的一分一段模型：24 个尾部锚点插值 + 31 省考生基数 + 物卢2也：物理类 62% / 历史类 38%。
  导出 `rankOfScore`、`scoreOfRank`（二分反查）、`beatPercent`、`admissionProbability`、`strategyOf`、`probabilityExplain`。
- **`frontend/src/utils/examProfile.js`**（新增，214 行）
  考生信息全站唯一数据源（`reactive` 单例 + localStorage `zhiyuan_exam_profile` 持久化），
  字段：省份/年级/本专/首选/再选/分数/批次/考生类型/手填位次；登录后自动 `syncFromAuth()` 合并后端 profile。

---

## 二、概率到底怎么算（你问的那个问题）

概率的真正口径在**后端** `RecommendationPolicyService`，前端以前是胡写的。现在前端 1:1 对齐了后端：

```
① 分数 → 位次：userRank = ScoreRankMappingService.resolveUserRank(province, subjectType, score)
            （后端查 score_rank_mapping 表；前端未登录时用 scoreModel 本地推算）
② 两个差值：scoreGap = 我的分数 - 院校投档分
            rankGap  = 院校最低位次 - 我的位次   （正数 = 我靠前）
③ 分段线性映射（scale）：
     rankGap  < -3000        → 丢弃
     -3000 … 1000           → rush      35–54%
     1000 … 10000           → safe      55–74%
     > 10000                 → guarantee 75–96%
     scoreGap 同理：-10 / 5 / 20 三个断点
④ 加权：probability = rankProb * 0.75 + scoreProb * 0.25   （位次权重 75%）
⑤ 分档：≥75% 保底 / ≥55% 稳妥 / ≥35% 冲击 / <35% 直接不推荐（minimumProbability=35）
⑥ 同一档位最多 5 所（MAX_PER_GROUP）
```

对应后端配置：`RecommendationScoringProperties`（rankWeight=0.75、scoreWeight=0.25、rush 35-54、safe 55-74、guarantee 75-100）。
前端 `scoreModel.js` 里的常量名与数值与后端完全一致：`MIN_RANK_GAP=-3000`、`RUSH_MAX_RANK_GAP=1000`、`SAFE_MAX_RANK_GAP=10000`、`MIN_SCORE_GAP=-10`、`RUSH_MAX_SCORE_GAP=5`、`SAFE_MAX_SCORE_GAP=20`。
**以后要调概率只改两处：后端 yml + scoreModel.js 顶部常量。**

页面上现在都会把依据写出来，不再是一个孤零零的百分数：
“26 年最低分 612 / 最低位次 8,420；你位次靠前 1,260 名、分数高出 6 分 → 录取概率 68%（位次差权重 75% / 分差权重 25%）”

---

## 三、六个问题逐条修复

### ① 登录页多了个蓝色图片
`views/LoginView.vue`：删除 `journeyImage` 插图，改为 3 条文字卖点（一分一段·位次换算 / 冲稳保·45 个志愿位 / 历年录取对比）。

### ② 首页两个按钮作用一样 + ③ 内容大量重复
- `views/HomeView.vue` 重写：3 张轮播卡的主/副按钮改成 6 个不同去向；删除 4 个 `TILES`、热点资讯块、9 个报考专题。
- **高考资讯按你的意思只保留首页一处**：同时删了 `components/GkSidePanel.vue` 里的侧边资讯卡（全站每页都在重复出现的那个）。
- 院校热度与热门专业并排，卡片点击直达 `/schools/:id`。

### ④ 导航只有首页能进（招生计划 / 院校排行 / 一分一段）
`components/GkHeader.vue`：主导航加二级下拉，任意页都能跳：
- 查大学 → 查大学 / 院校排行 / 招生计划 / 一分一段
- 志愿填报 → 志愿填报 / 我的志愿表
- 智能选大学 → 智能选大学 / AI 推荐查询
子页（SegmentsView / EnrollPlanView / RankView）的 `active` 也改成自己的 label，高亮不再串位。

### ⑤ 右上角按钮点不了 / 智能推荐大学跳废弃界面
首页模拟报志愿面板里的“普通类 / 艺术类”从 `span` 改成真 `button`（绑 `profile.entrantType`）；
“智能推荐大学”不再跳废弃的 `/recommend`，而是带着 `score/rank/subject/second/province` 进 `/choose`。

### ⑥ 志愿填报界面按钮看不懂（重新设计）
原页有 **4 组功能重叠**的按钮，其中“智能填报 / 开始智能填报 / 智能志愿推荐 / 模拟填报”四个干同一件事。
`views/VolunteerView.vue` 重写后：
- 顶部三步步骤条：填考生信息 → 选院校专业（45 个志愿位）→ 保存方案
- 只留 **2 个主按钮**（各带一行说明）：「开始模拟填报」「 AI 定制方案」+ 2 个文字链（我的志愿方案 / 防掉档诊断）
- 删掉 `mnz-quick` 四入口、`gk-vol-entries` 三入口、hero 里的重复 CTA，大数据区退化为纯展示

### ⑥⑦ 表单提交的数据没真正用上
- 全部表单字段改为写入 `examProfile`，**刷新/跳页不丢**，查大学、智能选大学、院校详情、志愿表全部复用同一份。
- 位次不再是假公式，并新增「位次修正」输入框（可直接填成绩单上的真实位次）。
- 卡片都加了「我的位次 vs 院校最低位次」对比行。

### ⑦ 点志愿表不是管理界面 / 里面又有一个志愿表
`views/PlansView.vue` 重写：删掉内部重复的“院校库选校”与第二个志愿表编辑器、删掉四页签工作台。
现在只做方案管理：当前志愿表（已填 N/45 + 冲稳保统计）→ 云端草稿 → 已保存方案，动作全部回到 `/volunteer`。

### ⑦ 选“冲击”背景变橙色
`styles.css` 末尾追加补丁：`.mnz-pcard__shield.is-rush` 等橙色渐变实心底改为浅底 + 橙字 + 描边；
并补上 `is-risk` / `is-unknown`（<35% 与未填分数）的配色。

### ⑧ 概率是啥 / 看不了院校详情
- **新增 `views/SchoolDetailView.vue` + 路由 `/schools/:id`**：四个 tab（院校概况 / 近三年录取 / 开设专业 / 招生计划），
  顶部直接展示录取概率与拆解依据；未填分数时就地输分。
- 查大学 / 院校排行 / 智能选大学 / 首页热门院校 的跳转全部指向该页（之前是拉起 AI 对话）。
- 概率按钮带 tooltip，写清楚“哪年最低分/最低位次 + 你靠前多少名 + 高出多少分”。

---

## 四、前后端联调（后端不改的前提下）

### 1. 两套志愿数据结构的冲突（Word 里“旧的和新的只能留一个”）
新增 **`frontend/src/utils/planSync.js`** 作为唯一桥接，不再在页面里各自拼字段：

```
本地 45 个志愿位（编辑真相）        后端 items（存储真相）
slots[0..14]   冲   ─────────────▶  strategy = "rush"
slots[15..29]  稳   ─────────────▶  strategy = "safe"
slots[30..44]  保   ─────────────▶  strategy = "guarantee"
majorNames[]   ─── join("、") ──▶  majorName
回流时：volunteerIndex 优先回原位，没有则按 strategy 落到对应段第一个空位
```

API：`PUT /api/plans/current`（保存到云端）、`GET /api/plans/current`（载入填报器）、`GET/POST /api/plans`、`DELETE /api/plans/{id}`。

### 2. 联调时需要注意的点
1. **未登录也能算概率**：公开页（查大学/详情/选大学）用 `scoreModel` 本地模型；
   登录后建议优先读后端 `/api/recommendations`（`examProfile.toRecommendationRequest()` 已准备好请求体），后端失败自动降级本地。
2. **省份下拉**可接 `/api/meta/options`（permitAll）取代 `PROVINCES` 常量。
3. **401 处理**：`/api/plans/**` 需要 `Authorization: Bearer`；`PlansView` 的“保存到云端”已做 try/catch 提示。
4. **mock 模式**仍可用：`npm run dev:mock`（MOCK_USER = testuser / 630 分 / PHYSICS / 浙江）。
5. `router.beforeEach` 里 ADMIN 强跳 `/admin`、profile 未完成强跳 `/profile-setup` 的逻辑未改，
   新增的 `/schools/:id` 是公开页，不受影响。
6. 后端 `score_rank_mapping` 表数据越全，位次越准；前端模型只是兑底。

---

## 五、改动文件清单

**新增**
- `frontend/src/utils/scoreModel.js`、`frontend/src/utils/examProfile.js`、`frontend/src/utils/planSync.js`
- `frontend/src/views/SchoolDetailView.vue`

**重写**
- `frontend/src/views/HomeView.vue`、`VolunteerView.vue`、`ChooseView.vue`、`PlansView.vue`
- `frontend/src/utils/volunteerCore.js`

**修改**
- `frontend/src/views/LoginView.vue`、`SchoolsView.vue`、`RankView.vue`、`SegmentsView.vue`、`EnrollPlanView.vue`、`MajorDetailView.vue`
- `frontend/src/components/GkHeader.vue`、`GkSidePanel.vue`、`VolunteerSheet.vue`、`RecommendSchoolRow.vue`
- `frontend/src/router/index.js`（新增 `/schools/:id`）、`frontend/src/styles.css`（末尾补丁块）

**未改动**：所有后端 Java / SQL / 配置；`exploreData.js` 的基础数据（其中 `buildChooseResults` 已不再被引用，可以下次清掉）。

---

## 六、构建 / 部署

```bash
cd frontend
npm install          # 沙箱无网，没执行；本地请先跑这一步
npm run dev          # 或 npm run dev:mock
npm run build         # 产物输出到 ../src/main/resources/static
cd .. && ./mvnw -DskipTests package
# scp target/college-recommendation-*.jar → ECS 上 /root/zhiyuan/target/，备份旧 jar 后 systemctl restart zhiyuan
```

> 注：本沙箱无网络、`frontend/node_modules` 为空，所以**没有跑 vite build**。
> 已做静态校验：所有本地 `import { ... }` 的具名导入均能在目标模块找到导出，`<template>`/`<script setup>` 标签配对。
> 本地第一次跑 `npm run build` 若有报错，大概率是样式类名漏补，把报错发我即可。
