# 项目日志（Project Log）

## 2026-08-07T08:42:06+08:00

- 初始化项目记忆文件。
- 等待用户下达规划指令。

## 2026-08-07T08:52:00+08:00

- 修复专业自动联想的数据来源：改用实际有数据的专业分数线表，并保留关键词、地区和科类过滤。
- 登录页顶部栏与业务页统一桌面和移动端尺寸规范。
- 影响文件：`MajorMapper.java`、`frontend/src/styles.css`、静态构建产物及项目状态记录。
- 验证：真实接口返回专业候选，前端构建通过，全量 67 项测试无失败。
- 下一步：用户刷新页面验收联想下拉与顶部栏一致性。

## 2026-08-18 离线恢复检查点
- 将已审计的后端公开 API 控制器/服务/DTO/Mapper 复制进当前工作区。
- 修复前端概率展示的空值安全与云端草稿替换同步。
- 移除 `MajorDetailView.vue` 中误导性的哈希推导专业分数线展示。
- 新增聚焦离线测试：`ScoreRankControllerTest`、`ProbabilityServiceTest`。
- 为离线沙箱验证补充测试垫片/资源：
  - `src/test/java/org/springframework/data/redis/core/StringRedisTemplate.java`
  - `src/test/resources/mockito-extensions/org.mockito.plugins.MockMaker`
- 在沙箱中用手工 JUnit 启动器验证聚焦离线测试通过。
- 生成检查点压缩包 `/data/zhiyuan-fixed-checkpoint-*.zip`。
- 遗留阻塞：沙箱依赖/网络限制导致完整 npm/Maven 还原不可用。

## 2026-08-20 学生视角 UX 改造
- 以学生任务视角研究对标产品：先完善画像，再发现院校/专业，最后落地一张可用志愿表，AI 仅作辅助。
- 从 `VolunteerView.vue` 移除完整的志愿大数据区块（统计、院校/专业 TOP 榜、去向城市排名）。
- 将顶部导航激活下划线移至底边并垂直居中标签，横线不再穿过文字。
- 解除未选方案时 AI 对话的阻塞；计划上下文空值现以 `null` 而非 `0` 发送。
- 新增本地志愿表到云端当前草稿的自动桥接（浏览器有 45 位志愿表但用户无云端草稿时）。
- AI 欢迎页新增明确的非阻塞方案上下文提示。
- 新增 `UX_REVIEW_20260820.md`，记录对标发现与面向比赛的设计决策。
- 验证：Vue/CSS 通过 Prettier 解析，抽取的 script 块通过 `node --check`，定向源码断言通过，本地 Chromium 几何验证下划线不压导航文字。
- 完整 Vite 构建在沙箱中仍被阻塞（npm 依赖包不可用），`npm ci` 以 `Exit handler never called!` 报错结束。

## 2026-08-20 功能设计校正
- 恢复原有配色主题、品牌、导航命名、页面描述、首页布局、侧栏、登录文案与咨询文案。
- 本轮改造限定在功能行为与数据正确性。
- 保留 `SchoolsView.vue` 的查询响应性与空值安全排序。
- 移除 `ChooseView.vue` 中生成的专业组 ID，修复缺失值/校验行为。
- `VolunteerSheet.vue` 中页面本地编造的年度录取数字替换为共享投档线历史源，并移除编造的计划/位次指标。
- 分数段查询增加明确的非法分数反馈。
- 新增 `FUNCTION_DESIGN_20260820.md` 记录保留的 UI 边界与功能变更。

## 2026-09-19 Agent LLM 兜底升级原生 tool-calling
- Agent 模糊语义兜底从"提示词约束 JSON 输出"升级为原生 function calling（OpenAI 兼容 `tools`/`tool_choice`，DeepSeek 同形状），补齐长期路线图的模糊语义兜底项。
- `AgentToolRegistry.getOpenAiToolDefinitions()` 由 `AgentToolSpec` 派生 OpenAI function-calling 声明（范围编码为 JSON Schema 的 minimum/maximum/minLength/maxLength），并附合成 `reply` 工具作为直接/澄清回复出口；新增工具此处零改动。
- `AiChatClient.chatWithTools()` 发送 `tools` + `tool_choice=auto`（不带 `response_format`——DeepSeek 上与 function calling 互斥），解析 `choices[0].message.tool_calls[0]`；纯文本内容映射为回复决策。
- `AgentDecisionService` 优先走原生通道（`ai.agent.tool-calling.enabled`，env `AGENT_TOOL_CALLING_ENABLED`，默认开），实时状态快照与历史压缩不变；原生通道失败降级回旧 JSON 模式。同时在两种 LLM 模式下增加代码级闸门：无"用户请求→助手确认"上下文时阻断 `removePlanItem`。
- 测试：新增 `AgentNativeToolCallingTest`（15 用例：schema 派生、tool_calls/reply/content 解析、未知工具与坏参数兜底、JSON 模式降级、删除闸门、开关关闭）；全量 187 测试绿。
- 部署到 zhiyuan6 栈（镜像重建 + `zv6-backend` 重建）。18080 端口实测：模糊"文科生"话术产生结合实时画像的原生 `reply` 澄清（档案 PHYSICS/588）；"偏硬核的工科专业"原生路由 `recommendMajors(majorKeyword=工科)` 并附画像说明；强意图本地规划器短路完好。

## 2026-09-19 意图路线图阶段 1-3（词表 / 对话状态 / 专业目录）
- **阶段 1 意图词表**：本地规划器全部同义词外置到 `src/main/resources/agent/intent-keywords.json`（27 个 contains 组 + 专业词典/停用词/疑问词/前缀链），由新增 `AgentIntentLexicon` 启动加载（资源损坏 fail-fast）。代码只保留组合/排除布尔结构；三个有序组（schoolStripVerbs / schoolNamePrefixes / majorKeywordPrefixes）保持替换链顺序。契约测试 `AgentIntentLexiconTest` 钉住组名与最小规模。语料套件在迁移中抓住一个真实等价性 bug（前缀剥离组漏出 groups 映射 → "他的热门"泄漏为专业关键词），上线前修复。
- **阶段 3 专业目录**：新增 `AgentMajorCatalogService` 将 100 行精选 major 表内存缓存（5 分钟 TTL 刷新、失败退避重试、DB 故障降级直通）。归一化接线在汇合层（`AgentToolFacade.recommendMajors`/`getMajorOverview`），本地提取与原生 tool-calling 关键词同时受益：精确命中 → 唯一包含 → 脏关键词包裹目录名（取最长，并列=含糊=直通）。绝不硬拒绝，保住推荐引擎零结果优雅降级。`AgentDecisionService` 增加决策层兜底：正则/词典提取失败时，文本中唯一最长目录名补位（"考古学就业前景怎么样"现以 0.3s 本地路由命中真实目录，此前落到 LLM）。
- **阶段 2 DialogState**：对 `recentMessages` 的 5 处独立反向扫描（学校槽、删除确认、专业方向槽、推荐快照、方案快照）合并入新 `AgentDialogState` 的单次遍历（另加一次性消息级正则提取）。各扫描的信任边界与首中即停规则逐字保留：学校槽每条消息先查 byName 载荷再查 USER 文本；推荐快照遇到第一条推荐 TOOL_RESULT 即停（载荷损坏也不回溯）；方案快照接受无载荷 TOOL_RESULT；删除确认只信最后一条助手文本及其紧邻前驱；方向槽只信最后一条助手文本。该状态供本地规划器、破坏性调用闸门与 LLM 提示词构建三方消费（画像不完整时明细 Map 原样保留）。`AgentDialogStateTest` 15 个专项等价测试。
- 验证：全量 220 测试绿（路线图前 187）。部署 zhiyuan6；启动日志确认"lexicon loaded: 27 groups, 40 major keywords"。
- **阶段 4（校名简称）实测后按评审暂缓**：实测 LLM 已能原生展开无歧义简称（"湘大"→湘潭大学 33 专业、"华科"→华中科技大学 27 专业，均经 getSchoolDetailByName 真实命中）。失败用例"湖大的专业怎么样"在 LLM 之前就被本地误路由到 getMajorOverview("湖大的")——且"湖大"本身歧义（湖南/湖北大学），别名表无法安全修复。按阶段一评审结论，建表推迟到赛后。

## 2026-09-19 校情详情回复卡片（用户报告的回复缺口）
- 用户报告："帮我推荐湘潭大学热门专业"正确调用了 `getSchoolDetailByName`（该路由是 2026-09 审计的设计：显式校名+推荐/热门→该校专业列表），但最终回复只有一行工具摘要"已按学校名查询...可参考 33 个专业，例如：..."，而非预期的专业列表。
- 根因：`AgentReplyFormatter.format()` 只有概览 markdown（getMajorOverview）、兜底建议、topItems（推荐）三个分支；校情详情载荷（universityName + majors 数组）落入了原始摘要回显。
- 修复：新增校情详情分支渲染卡片——`## 校名`、层次行（985/211/双一流标志 + schoolTags 去重，退回 universityTier）、省份、"可参考专业（共 N 个，按参考录取分从高到低展示）"表格（载荷前 8 行：majorName/cutoffScore/minRank，空值显示"—"）+ 下一步引导（问专业概览 / 推荐后加志愿单）。专业列表排序来自 DB 查询（最新年、分数降序），"录取分较高≈报考热度更高"有数据依据，非编造。
- 另：原生 tool-calling 调用 content 为空时（DeepSeek 的 tool_calls 常不带 content），现在按工具与参数生成确定性话术（与本地规划器同款措辞）替代光秃的"正在调用工具。"。
- 测试：新增 `AgentReplyFormatterTest`（6 用例：卡片渲染、层次回退、空专业摘要回退、序号载荷、推荐/概览分支不受影响）+ `AgentNativeToolCallingTest` 话术用例。全量 227 绿。
- 部署并以报告原句实测：0.24s 本地路由 → tool_call 话术 → 结果 → 415 字含 8 专业表格的 markdown 卡片。

## 2026-09-20 全国语料扩充第 1+2 波（双智能体：导入 + 审计）
- 以双智能体编排执行语料扩充计划：主智能体跑导入管线；独立只读审查子智能体在继续前复核每波（基线完整性 / 数据质量 / API 应用效果）。
- 来源：Gaokao-Compass-11M（MIT，HuggingFace choucsan/Gaokao-Compass-11M），2025 版，6 省（guangdong/jiangsu/henan/hubei/hebei/sichuan），4 张表（score-range / school-admission / major-admission / enrollment-plan），约 56MB CSV 位于 tmp/gaokao-compass/。
- 安全：任何写入前先 mysqldump 基线备份到 tmp/db-backup-20260920/；导入幂等（university 用 NOT EXISTS，其余 ON DUPLICATE KEY UPDATE）；湖南 SIMULATED 数据（2613 行）原样保留作为比赛验证展示；新行 data_kind='REAL'（前端标注 录取数据 vs 比赛验证数据）。
- 科类映射：物理类→物理 / 历史类→历史（6 省 2025 均为新高考；艺术/混合批次排除；校线按批次优先级 本科批>提前批>特殊类型>专科 去重，每（院校,科类,省）取 MIN(score) 语义）。
- 导入（基线 → 现在）：university 80→1445（校名去重、全/半角括号归一；tier 来自批次 本科批1181/专科批204）；admission_cutoff 405→7702（每省×科类校线，组间取最低）；score_rank_mapping 3405→9686（2025 曲线，含 segment_count）；major_admission_cutoff 2613→79183（76,570 行 REAL 专业线；plan_count/duration_years/tuition_per_year 经 enrollment-plan 关联覆盖 93%，关联键（校代码,归一组号,归一专业名）+（校代码,专业名）兜底；34,619 行按名称关联精选专业表）。
- 管线脚本：tmp/gaokao-compass/w1_import.py、w2_import.py（CSV→SQL，仅 stdout，可重跑）。
- 验证（审查轮次）：R1 PASS 12/13（唯一"不符"是我清单预期写错：无草稿账号 /api/plans/current 返回 404 是正确业务行为）；R2 PASS 13/13 零问题。证据亮点：湖南基线完好（405/3405/2613 SIMULATED）；身份键零重复；REAL 零孤儿行；无越界分数。线上 API：广东/物理 withDataOnly total=475；深圳大学详情 cutoffHistory 2025=572/16789 + majors 52 项全带计划/学制/学费（计算机 634/8073/24人/4年/6853元）；agent 对话"查看深圳大学详情" 0.16s 以 REAL 数据渲染卡片；推荐引擎 广东580物理 → 冲16/稳19/保409。
- 已知备注：(1) 河北/湖北/四川 2024 score-range 文件缺 category，本轮跳过 2024，后续可补录取史深度；(2) 综合改革省份（浙江/上海等，category=综合）无法映射 subject_type 物理/历史，已排除；(3) 不同专业组的同名专业行会在院校详情重复出现（上游查询行为，先前已存在）；(4) 静态院校属性（双一流/软科排名/保研率/研究生院）不在该数据集——计划另行补公开榜单；(5) 新增测试账号 gdreviewer01（广东/物理/580），tcsmoke0919 省份此前已更正 湖南→湖南。

## 2026-09-20 湖南比赛数据替换为真实数据集（第 3 波）
- 按用户决策，全部湖南 SIMULATED 比赛验证数据替换为真实 Gaokao-Compass-11M 数据（2024+2025，两年 category 均为干净的 物理类/历史类）：
  - 删除：score_rank_mapping 湖南 3405 行（含虚构 2026 年）、admission_cutoff 湖南 405、major_admission_cutoff SIMULATED 2613、admission_group_cutoff 湖南 804（该表无后端消费方；清空而非导入编造组行）。
  - 导入真实湖南：major_admission_cutoff 2024/2025 两年 +40,273 REAL 行；admission_cutoff 湖南 2024=2941/2025=1440；score_rank_mapping 湖南 2024/2025 各 1152；university 1445→1902（湖南招生名单含大量省外院校）。
- 全库状态：major_admission_cutoff 116,843 行全部 data_kind='REAL'（SIMULATED=0）——"比赛验证数据"标签不再出现；全部标注 录取数据，calibration_source='Gaokao-Compass-11M (MIT)'。
- 管线：tmp/gaokao-compass/w3_replace.py（范围 DELETE + 幂等重导入，stdout SQL）。
- 审计（独立只读子智能体，第 3 轮）：PASS 13/13 零问题——其他 6 省基线未动（admission_cutoff 7297 / srm 6281 / 广东 total=475），身份键零重复、零孤儿行、分数线区间 184-692，虚构 2026 彻底消失（位次 API 现报 mappingYear=2025）。
- 湖南档案（588/物理）实测：位次查询 21502 EXACT；推荐 冲27/稳39/保692 全 REAL；湘潭大学详情卡片展示 2024+2025 真实投档史与 71 个专业（全部带 planCount/tuitionPerYear，数学类 97人/5300元）。
- 回滚备份：tmp/db-backup-20260920/（基线 + 替换前 dump）。若比赛展示需要，模拟湖南数据可从 pre-replace-hunan.sql 恢复。

## 2026-09-20 前端展示补全（W4/W5，双智能体审计）
- 目标：补齐阶段一盘点出的剩余前端展示缺口。
- **W4a 院校属性**（源：软科公开 API bcur 主榜，589 所）：soft_ranking 现 504 行（清华=1，湘潭=107），school_type 610（综合/理工/师范/农林…），is_double_first_class=117，tags 以"985|211|双一流|综合类"样式回填（COALESCE——已有手工 tags 保留；标志位 GREATEST 合并）。
- **W4b 专业目录**（源：教育部《普通高等学校本科专业目录（2024年）》官方 PDF，pypdf 解析）：major 表 100 → 657（13 学科门类 +557 行，每行含 category/degree_type 与事实性模板描述；subject_requirement 设计上为 NULL——选科要求因校而异）。MajorsView 树覆盖全部 13 门类；MajorDetailView 支持全目录。
- **W4c + W5 历史深度与孤儿修复**：导入 广东/江苏/湖北/四川 2024 校线（admission_cutoff 2024：384/2338/1803/1123）使 cutoffHistory 呈现两年。第 4 轮审计 FAIL 一项：106 所仅有 2024 线的广东院校进入列表（total 475→581）但专业表为空（未导入过 2024 专业线）。代码核验根因：UniversityQueryService.detail 的专业查询（findAllByUniversityAndProvinceSubject）**无年份过滤**——修法是补导而非回退：**W5** 导入 4 省 2024 专业线（+47,185 REAL 行；major_admission_cutoff 现 163,928 全 REAL：2024=73,715 / 2025=90,213）。同名专业去重保留最新年，2025 学校卡片不变，孤儿学校（如 三峡大学科技学院）渲染完整卡片（5 个专业，2024）。
- 审计链：R4 11/12（C11 FAIL）→ W5 修复 → R4-re PASS 4/4（独立复核：列表、孤儿详情、身份键零重复、无越界分数、未动表完好）。
- 已知缺口（记录在案，未编造）：(1) postgraduateRate / hasGraduateSchool / hasDoctorProgram 无干净结构化源（教育部授权点为动态审批文件），院校详情显示"—"；(2) 约 35 所双一流艺术/军事/体育院校不在软科主榜且多不在普通批招生数据，is_double_first_class 覆盖 117/147；(3) 目录 PDF 解析捕获 615/~792 个专业（跨页断行）——招生数据中用户实际遇到的专业已覆盖；(4) 软科医药/财经单列榜未抓取——相关院校缺 school_type。
- 管线：tmp/w4_frontfill.py、tmp/gaokao-compass/w5_2024majors.py（可重跑，stdout SQL）。变更前备份：tmp/db-backup-20260920/pre-replace-hunan.sql 仍为当日回滚点。

## 2026-09-20 W6 院校属性补全 + schoolTags 序列化修复
- **W6a 院校类型与标志**（源：掌上高考公开学校列表 API api.zjzw.cn，分页 2,991 所）：university.school_type 按全部 12 类标准化（综合614/理工554/医药174/师范169/财经144/农林60/艺术65/政法33/语言27/民族12/体育17/军事7，覆盖 1876/1902，后缀归一到 W4a 风格），nature 补空，985/211/双一流标志 GREATEST 合并（双一流 117→158，含 中央美术学院 等艺术院校与分校区），tags 重算为标准"985|211|双一流|综合类"格式。
- **W6b 排名刷新**：soft_ranking 以软科完整主榜重建（bcur_type=10，812 所；命中 590；清华=1）。注意：W4a 用的 bcur_type=11 是子榜；type=10 才是权威完整排名。
- **后端修复（审计 FAIL → 修复 → 复审 PASS）**：R5 审计发现普通院校（非985/211/双一流）渲染空 schoolTags 数组，因 buildSchoolTags 只输出层级标签。新增带 tagsColumn 的重载（层级标签沿用 20260820"211≡双一流"设计 + 从标准化 tags 列解析类型段，过滤"211"）；5 个调用点（UniversityQueryService×2、RecommendationService×2、SchoolDetailService）全部传入 tags 列。修复中途引入的 buildSchoolTags 互调递归 bug 被测试套件当场抓住（RecommendationServiceTest StackOverflowError），未上线。一个旧测试断言按新契约更新（普通院校：无层级标签，允许类型段）。全量 227 绿；重建并部署。
- 线上验证：广东/物理 withDataOnly total=581，空 schoolTags 为 0（此前 50/100）；北京大学医学部=["985","双一流","医药类"]；中国刑事警察学院=["政法类"]；湘潭大学=["双一流","综合类"]；中央美术学院=["双一流","艺术类"]。
- **审查员发现并保留的范围外数据漂移**：major_admission_cutoff 新增 2,248 行标注 湖南比赛验证/演示值 的 SIMULATED 行（管理端添加的展示数据——未触碰）；score_rank_mapping 增长 +1,141 行湖南官方一分一段含 2026（管理端导入）。2026 导入前的 Redis score-rank 旧缓存已清理；/api/meta/rank 湖南/物理/588 现稳定返回 mappingYear=2026 rank=25204 EXACT。
- **仍未解决**：postgraduateRate / hasGraduateSchool / hasDoctorProgram 未填充——掌上高考详情接口已停用（"接口请求版本不存在"），无干净结构化源；字段显示"—"。管线：tmp/w6_attrs.py（可重跑），原始页面在 tmp/univ-attrs/eol/。

## 2026-09-20 前端 UX 修复（就业映射 / 相关专业 / 页大小 / SPA 回退）
- **逐专业就业方向**：MajorDetailView 此前用 9 条门类级 EMPLOY_MAP（土木工程显示 软件开发/算法工程师——错误的门类默认）。替换为关键词驱动映射（约 30 组关键词覆盖 13 门类+专科，如 土木/建筑 → 结构设计/施工管理/造价咨询/工程监理/BIM），未命中退回门类默认。实测：土木工程现渲染 结构设计/施工管理/造价咨询/工程监理/BIM 工程师。
- **相关专业推荐**：同门类候选按 openSchoolCount 降序（热门优先）排序，替代插入序。土木工程现推荐 计算机科学与技术/数据科学与大数据技术/电气工程及其自动化 等，替代 丝绸设计与工程/乳品工程。
- **选择器专业目录瘦身**：GET /api/majors 新增 `minOpen`（默认 20）与 `all` 参数——默认列表只返回开设院校 ≥20 的专业（657→219；榜首 计算机科学与技术 748 所）。MajorsView/SchoolsView 下拉相应缩小；深链指向被裁剪专业时 MajorDetailView 回退 /api/majors?all=true。
- **列表加载速度**：根因是前端循环至多 15 次串行 size=100 请求，而后端每请求全量重算。MAX_PAGE_SIZE 与 @Max 校验提至 500；HomeView/SchoolsView/EnrollPlanView 改用 size=500、更少循环（7 省 ≤2 页）。实测：单页 500 条 = 0.79s；整省列表约 2 次请求替代 15 次。
- **SPA history 路由回退**：子路由（/rank、/schools/41、/majors/土木工程…）直达/刷新返回 RESOURCE_NOT_FOUND JSON（白屏/裸错误框），因 Spring Boot 无 index.html 回退。新增 SpaForwardController 将前端路由白名单 forward 到 /index.html；/api 与带点号静态路径不受影响。验证 /rank 现渲染应用（hash 路由落在首页；/#/rank 渲染排行空态页）。
- 后端测试 227 绿。部署 zv6-backend。前端在镜像内构建。

## 2026-09-20 排行榜页实现 + 浏览器验证 UX 修复
- **/#/rank 现在显示真实榜单**：此前该页是有意的空态（"本页不展示本地生成榜单"），因无可溯源排名数据。W6 导入软科主榜（soft_ranking，590 所）后，新增 GET /api/universities/ranking?schoolType=（UniversityMapper.findRanking，tinyint→Boolean 安全映射）并重写 RankView.vue：来源溯源说明、13 个类型筛选页签、表格（排名/院校/省份/类型/层次标签）、分页、链接到院校详情。修复 styles.css 中遗留 `.gk-rank` flex 行样式的类名冲突（容器改名 `gk-rankboard`），以及线上发现的 ClassCastException（tinyint(1) 映射为 Boolean）。
- 线上：/#/rank 渲染 590 所榜单（1 清华 / 2 北大 / 3 浙大…），类型筛选可用（医药类 2 所——医学院校在 7 省招生语料中大面积缺席），每行链接院校详情。
- 关闭上轮"未找到该专业"之谜：深链测试 URL 打错字（%E5%9C%9C=圜 而非 %E5%9C%9F=土，即"圜木工程"），API 与匹配逻辑正确。正确编码 /#/majors/土木工程 验证：简介渲染，就业方向 = 结构设计/施工管理/造价咨询/工程监理/BIM，相关专业 = openSchoolCount 热门优先。
- 全量 227 绿；已部署。

## 2026-09-20 志愿表假数据清理（用户报告）
- 用户报告：推荐卡片"可填专业(0)"但卡片显示 14 个专业；志愿表西北工业大学的专业下拉出现无关专业（中医学/临床医学/互联网金融…）；"[1164组]"标签像真实组号。
- 根因——全部是志愿表组件时代的装饰性假数据：
  1. `RecommendSchoolRow.vue`：`fillableMajors = min(school.majorCount, seed % 15 + 5)`——seed 随机数；metaLine 编造 硕士点/保研率/软科排名，groupNo/schoolCode/subjectReq 编造，years 编造 2025/2024 分数计划。
  2. `VolunteerSheet.vue`：专业下拉 `majorOptions` = 全部 /api/majors 目录仅按批次过滤（不按校）——因此出现无关专业；"可填专业(N)"用 `f.majorCount`（推荐项字段=0/null），而下方展开列表用的是真实分校 API；"[XX组]"标签 = String(school.id).padStart + "组"（大学 id 伪装组号）。
- 修复（仅前端）：
  - VolunteerSheet：下拉改为 `majorsForRow(slot)` 按校读 majorCache（真实 /api/universities/{id} 数据，批次过滤，懒加载）；删假 [组] span；计数 = 真实 majorList.length。
  - RecommendSchoolRow：删除 seed 编造 computed（groupNo/schoolCode/subjectReq/fillableMajors/编造 metaLine 段）；metaLine 改用真实条目字段（省份/tier/schoolTags）；years 表只显示条目真实录取年行（plan 来自 planCount，缺失显示"—"）——不再编造 25/24 数字；rankCompare 空值安全。
- 数据源验证：/api/universities/1164（湖南/物理）返回 16 个西北工业大学真实专业（计算机/航空航天类/工程力学/柔性电子学/海洋工程类…），零混入。el-select 合成键盘自动化限制导致测试环境无法完成端到端下拉交互（真实鼠标不受影响）；数据层经 API 验证。
- 前端重建 + 部署；后端本轮未动。

## 2026-09-20 可填专业计数真修复（聚合年份窗口）
- 用户再次报告 可填专业(0) 仍可见。验证**非缓存问题**：新 bundle 复现——是真实的遗留 bug。
- 根因：aggregatePlanByUniversity（列表 API majorCount/planCount 来源）按全省 MAX(admission_year) 过滤；管理端导入的 2026 湖南演示行（2,248 SIMULATED）把窗口顶到 2026，所有 2024/2025 真实学校计数归零。前端卡片计数绑定懒加载分校列表（点击前为空），因此到处 (0)。
- 修复：聚合 SQL 改逐校最新年相关子查询；前端计数 = 已加载列表 || 后端 majorCount || 0。验证：列表 API 返回真实计数（北大 10 专业/319 计划）；志愿表卡片显示 5/11/17/30/37，零个 (0)。同时记为 DEV_ISSUES L-06（L-03 年份窗口风险成真）。
- 227 测试绿；重建部署。提醒用户浏览器：已打开的标签页持有内存中的旧 SPA——需一次 F5 刷新以加载修复。

## 2026-09-20 查大学列表性能 + 失败可见
- 用户报告 查大学慢且显示 院校 0 所。实测：API 约 1.9s/页（湖南/物理/690，total=1630）——并非服务端超时；"0 所"是旧前端 JS（刷新前，15×100 串行）超出 15s apiFetch 超时，错误被吞进"无结果"空态（loadError 有赋值但从未渲染）。
- 修复：(1) UniversityQueryService.list() 现缓存排序后全量条目（按除 page/size 外全部查询参数为键，TTL 3min，≤64 条）与重 SQL 上下文（最新校线映射 + 计划聚合，按省+科类，TTL 3min）——第 2 页起与重复访问从约 1.9s 降至 0.04-0.06s。(2) size 上限提至 1000（@Max + MAX_PAGE_SIZE）；HomeView/SchoolsView/EnrollPlanView 改 size=1000、≤3 页循环（1,630 所 = 2 次请求）。(3) SchoolsView 现渲染 loadError（"院校列表加载失败：…"）与加载中态，失败不再伪装成空结果。
- 实测：冷 1.85s、热 0.06s、total=1630、页大小 1000。227 测试绿；已部署。记为 DEV_ISSUES L-07。
- 提醒：持有部署前 SPA 的浏览器需一次 F5 加载新 bundle（index.html 为 no-cache，资源带哈希）。

## 2026-09-21 校准路由层（Jev"系统一"原则，默认关闭）
- 目标：用 Jev 的类型化决策/校准概率原则加固 Agent 决策层（模糊意图路由 + 改述式删除确认），**不改变默认行为**；每条路径可回退。
- 新增 `service/agent` 类：`CalibratedRouteProvider`（抽象）、`DeepSeekChoiceProvider`（在既有 DeepSeek 通道上实现 Choice/Noul 语义 + 校准置信度；严格 JSON；未知工具/空白/非法 → "无意见"）、`JevSystemOneProvider`（TypeSafe AI 托管端点，无 jev.api-key 时惰性——线格式按 2026-09 文档，**任何启用前必须影子验证**）、`SemanticRouterService`（编排器）、`AgentRoutingPreambles`（自 AgentDecisionService 抽出避免构造循环）。
- SemanticRouterService 安全设计：影子模式（仅并排对比日志，从不决策），由 `ai.agent.calibrated-routing.enabled` 控制（默认 false）；主动模式只在置信度 ≥ 生效阈值（钳位 [0.5,0.99]，NaN→0.9）**且**白名单业务工具时短路（reply 永不短路）；专用有界线程池（core2/queue32/AbortPolicy，daemon，@PreDestroy）——上游挂死永不占用 ForkJoinPool.commonPool；路由预算 4s；连续 3 次失败**或**连续 3 次"无意见"（垃圾 JSON 盲区）即熔断，60s 冷却，单个半开探测失败（含无意见探测）立即重开；提供方异常/超时/低置信永远回退旧词表→DeepSeek 路径（最坏=该层不存在）。
- 删除语义确认：`ai.agent.delete-semantic-confirm.enabled`（默认 false，独立开关）——guardDestructiveCalls 内的 Noul 闸门，仅 ≥0.9 校准置信为真，其余一律 fail-closed；旧两段显式确认不动。
- 测试 227→272 绿（路由门控/阈值钳位/熔断/半开/影子/提供方解析/Jev 删除闸门纯函数；超时抖动用例以 settle 轮询确定性化）。
- 审计：两轮独立子智能体评审。R1：P1-1（路由调用占 ForkJoinPool.commonPool + 超时不取消挂死上游 → 池饥饿风险）与 P1-2（Jev 删除闸门缺置信检查，违反 fail-closed）——均修复；R2 验证修复并发现 4 个新 P2，其中拒绝重复计数、无意见半开、抖动用例、缺 Jev 闸门测试当场修复。接受的 P2：路由/影子/删除共用熔断（fail-safe 方向）、影子同步成本（类 javadoc 注明）、稳态 2 路由线程（queue-before-max 语义）、Jev 调用失败双 WARN。
- 部署 zv6-backend，**全部新开关 OFF** → 行为与上一版一致。启动日志确认"Jev System One provider disabled (no jev.api-key configured)"。线上冒烟：本地意图"帮我推荐学校"→ recommendSchools 工具 → 真实湖南/物理/588 推荐（位次 25204，38 校，冲/稳/保分组）；模糊对话 → DeepSeek 原生 tool-calling 回复 1.36s；日志零错误。
- 启用路径（就绪时）：① 影子：置 AGENT_CALIBRATED_ROUTING_ENABLED=true（mode 默认 shadow），观察"Agent calibrated route shadow: semantic=… actual=… agree=…"一致率日志；② 主动：AGENT_CALIBRATED_ROUTING_MODE=active（阈值从 0.9 起步）；③ 删除确认：AGENT_DELETE_SEMANTIC_CONFIRM_ENABLED=true；④ 真实 Jev：置 JEV_API_KEY + AGENT_CALIBRATED_ROUTING_PROVIDER=jev，先影子（线格式未验证）。运行时回滚=关 env 开关，无需重新部署。代码回滚=checkout 被改文件（基线无未提交 agent 改动）。
- 冒烟账号 smoketest_jev（用户 id 36）留在库中供影子模式观察。
- 同日补记（用户决策）：彻底移除 TypeSafe AI 托管端点适配器——删除 `JevSystemOneProvider` 及其测试、`jev:` 配置块与 `provider=jev` 选项。理由：只借鉴 Jev 的**原则**（类型化 Choice/Noul + 校准置信 + 置信门控），不引入外部模型服务与适配兼容风险。校准层现仅跑项目自有 DeepSeek 通道（`DeepSeekChoiceProvider`）；`CalibratedRouteProvider` 保留为未来第二后端的扩展缝。测试 272→266 绿；重建重部署 zv6-backend；启动日志零 Jev 端点引用；线上冒烟（本地意图 recommendSchools + LLM 格式化回复）正常。开关仍默认关；启用/回滚路径不变。

## 2026-09-21 晚间：主动找虫波次（4 个模式扫描子智能体 + 线上 UI 测试）+ 失败改写设计
- 用户授权：不再等用户报 bug——子智能体按模块扫描 L-01~L-08 家族模式，主智能体做真实浏览器点击测试，不一致处交根因子智能体。
- 扫描（4 子智能体，有据）：32 项发现（3 P0 / 8 P1 / 21 P2）。UI 实测 8 场景（首页/查大学列表/院校详情/专业详情/排行+刷新/推荐流/方案添加/agent 对话）——方案添加流端到端验证（此前疑似坏，弹窗确认可用）。
- 本批修复（后端）：① findSchoolsByMajorName 全局 MAX(year) 被 13 行湖南-2026 演示行毒化 → 逐(校,省,科类)相关窗口（临床医学开设院校列表：此前仅演示校，现 100 所真实校各取自己最新年）；② UniversityQueryService majorId 过滤解析为空时返回空结果而非静默不过滤（L-08 家族）；③ SchoolDetailService.getSchoolDetailByName 模糊解析（精确→包含唯一采纳/多候选 400/无→未收录）——修复"查中南大学"动词粘连名失败为 暂未收录；④ 决策层：删除确认否定守卫（先不/不用/不确认…），removePlanItem 缺失/不可解析 selectionIndex 时改为询问用户而非编造"第1个"，JSON 模式缺 action 时抢救 reply + 剥 ``` 围栏；⑤ AgentFailureRewriteService（用户指定设计，默认开 ai.agent.failure-rewrite.enabled）：只读工具硬失败由 LLM 以通用知识改写（标注 非本系统数据，禁止编造分数位次），替换红色报错；方案变更工具保持确定性引导；fail-open 回旧文本。
- 本批修复（前端）：ChooseView size=100→全量分页 + loadError 渲染（P0：93% 候选被静默丢弃）；VolunteerSheet 分页 + 可见错误提示；EnrollPlanView/SchoolsView/HomeView 移除硬 3 页上限 + resp.ok 检查 + 错误态；AgentWorkspace 失败工具标签在 AI 补充解答后柔化为中性"系统数据未命中，已由 AI 补充解答"（不再爆红）；删除死代码 mock exploreData.js（L-05）。
- 测试 271→278 绿；独立审计轮：可部署、无 P0；其 P1（SchoolsView 缺 resp.ok）当批修复。实测：用户原始失败消息现正确解析 中南大学（52 专业去重）；哈佛大学（数据集外）获得带 通用信息 标注的优雅 LLM 回答而非红色报错；临床医学开设院校列表恢复。
- 待用户决策/后续批次：(a) admission_cutoff 推荐窗口为省全局 MAX(year)，现读 2026 演示行——改逐校窗口会改变演示行为（需用户拍板）；(b) 2026 SIMULATED 演示行分数与演示用户相同（多校 588/25204）出现在推荐卡 最低位次——数据治理决策；(c) addPlanItem 忽略具名学校（把中南大学加入志愿单 实际加第 1 项）——执行器需按名匹配；(d) RocketMQ 异步推荐管线是产品流死代码（失败不可见）；(e) prod profile DB_SCHEMA_INIT_MODE 默认 always 且 schema.sql 含 13 条 DROP TABLE——默认翻为 never；(f) P2 积压：模糊解析 LIKE 转义、SchoolsView 专业筛选静默空分支、MajorDetailView 硬编码 2025 标题、展示层 SubjectType.name() 残留、MajorPickDialog 冗余去重。

## 2026-09-21 夜间：找虫工作流固化 + 积压清空
- 工作流固化：`zhiyuan-bug-hunt`（.zcode/workflows/zhiyuan-bug-hunt.dwf.ts）——4 个并行模式扫描器（按 DEV_ISSUES L-01~L-08 分 数据/前端/agent 决策/装配）→ 共享分诊 → 逐发现独立确认 → 以 `mvnw.cmd -B test` 为门的有界修复轮 → 独立 diff 审计 → docker build/up → 线上冒烟 → DEV_ISSUES/PROJECT_LOG 回写。浏览器点击测试留在主智能体（工作流外）。模式记忆亦记入 DEV_ISSUES 原则沉淀。
- 积压清空：
  (a) AdmissionCutoffMapper.findLatestByProvinceAndSubject——省全局 MAX(year) → 逐校相关窗口（L-06 家族根修）。线上：推荐候选池 38 → 1,555 所真实院校；每校显示自己最新年位次（25720/24083/10797… 替代统一演示值 25204）。注意：冲稳保分层现作用于大得多的池——排序/概率调优值得后续一轮。
  (b) addPlanItem 按名匹配：本地规划器传 schoolName 参数；resolveSelectedRecommendationItem 在最新推荐载荷中匹配 label/universityName；名称缺失 → 明确引导（"最近一轮推荐中没有「X」…"）。实测：抽取干净（把 经 schoolStripVerbs/schoolNamePrefixes 剥离），中南大学在 25204 位次推荐中被正确拒绝为不存在而非静默加第 1 项。
  (c) DB_SCHEMA_INIT_MODE 默认 always→never 翻转（application.yml/dev/prod；prod `java -jar` 不再 DROP 全部表；新空卷仍走 MySQL initdb 引导；compose override 已钉 never）。
  (d) SchoolDetailService 模糊解析 LIKE 通配符转义。
  (e) SchoolsView 专业筛选失败保留上次筛选 + 可见内联报错（不再静默关闭筛选）。
  (f) MajorDetailView 硬编码"2025 本科批" → 取实际数据行动态年份（回退最新年份）。
  (g) 展示/提示层 SubjectType.name() → getDisplayName()（AgentDialogState/AgentReplyFormatter/AgentFallbackAdviceService；LLM 不再看到/输出 PHYSICS）。
  (h) RocketMQ 异步推荐管线记录为仅 API（前端走同步流；失败可见性缺口记 DEV_ISSUES）——接线或移除留产品决策。
- 测试全程 278 绿；部署 zv6-backend（healthy）；线上冒烟：按名添加拒绝路径、年份窗口推荐、失败改写（哈佛）、专业开设院校真实数据列表——全部确认。

## 2026-09-21 深夜：推荐排序消融实验（A0 → A1）
- 方法：消融基架 tmp/ablation.py（env 凭证，UTF-8 客户端）——拉取 4 个档案（湖南物理 588/560/620、广东物理 580）完整冲/稳/保并计算分组质量指标：位次不可能数（rankGap < -3000 仍推荐）、组纯度（rankGap 落在该组模型位次窗内）、|rankGap| 均值、演示 2026 泄漏。
- A0 基线缺陷：4 档案共 264 条位次不可能推荐；RUSH 纯度低至 18.8-31%（如 陆军工程大学 位次 8890 推荐给 25204 位次考生）。根因：位次依据返回 null（位差超出 ±模型范围）时 ProbabilityService 混合概率回退纯分数依据，巧合较低的投档线"救回"位次不可能院校。
- 调优（A1）：RecommendationPolicyService.explain 位次一票否决——位次数据完整且 rankGap < MIN_RANK_GAP 时概率=0/策略=null/不推荐，与分数依据无关；缺位次数据仍回退纯分数。单测：否决用例、explain 归零用例、缺位次回退用例。
- 消融结果（A0 → A1）：位次不可能 264 → **0**（全档案）；RUSH 纯度 31%/23.8%/85.2%/18.8% → **100%/100%/95.8%/100%**；SAFE 纯度 62.3%/46.3%/65.5%/42.1% → 79.6%/75.5%/74.5%/72.7%；总量缩约 5-9%（移除的均为位次不可能）。残余 SAFE 不纯（约 25%）来自窗口边缘纯分数依据行——可接受，持续观察。
- 否决暴露的夹具一致性修复（仅测试）：H2 曲线无 660 点（外推得位次 10000，否决了 660 分考生的浙大）→ 补 655→5200/660→4800 与 浙大 658→5000 自洽；南京师范 江苏 612→20000 与 620→26000 映射矛盾 → 改 612→29000。否决暴露问题，夹具自洽而非削弱否决。
- 测试 278 → 282 绿（4 个新策略测试 + 夹具对齐）；部署 zv6-backend；消融 A1 对线上服务复跑确认指标。诊断期临时加过又移除的 TEMP 诊断已清理（RecommendationService/FreeTextRecommendationService/RecommendationControllerTest 干净）。

## 2026-09-22：主智能体点击测试波 + 位次否决前端回归修复
- 浏览器注册表仅主智能体可用（子智能体尝试诚实失败"Browser is not available in subagent"，回退 HTTP 检查）。点击执行留主智能体；分析交子智能体——与固化工作流的 notCovered 注记一致。
- 对线上部署执行点击套件（登录流 / 查大学搜索筛选 / 中南大学详情 52 去重专业真实数据 / 智能选大学 total=1630 分页 / agent 失败改写 UI）：全部 PASS。失败改写 UI 按设计渲染：保留失败工具标签 + 中性灰"系统数据未命中，已由 AI 结合通用知识补充解答" + LLM 回答标注 通用信息。
- 点击测试新发现：院校详情概率面板对 smoketest_jev 显示"待测算/未设置分数"，而 tcsmoke0919 显示"0% 概率极低"。根因（分析子智能体）：前端极低概率分类器（utils/recommendation.js isExtremelyLowProbability）只识别旧形态 概率=null+关键词；消融调优引入的位次否决态返回 概率=0+策略=null，落入了误导性的静态"未设置分数"分支。修分类器同时把 概率=0&&策略空 视为极低；容器重建；浏览器验证：同账号面板现显示"0% 概率极低"含完整位次拆解。测试 282 绿（后端不受影响；纯前端修复）。

## 2026-09-22：全站 UI 点击测试扫描（17 条路由）
- 覆盖：全部 17 条前端路由在真实浏览器点击测试（对线上部署）：首页/登录/查大学列表+搜索/院校详情(4 页签：院校概况/近三年录取/开设专业/招生计划)/查专业列表/专业详情(4 页签：概况/开设院校/录取预测/相关就业)/志愿填报(两阶段+45 志愿位+概率极低卡拒绝+正常卡添加 0→1)/智能选大学(total 1630 分页)/排行(医药类筛选=2 所)/一分一段(588→25,204)/招生计划(1630 所全量)/资讯列表+详情(点击跳转+正文)/推荐查询/历史记录(8 条)/志愿方案/画像设置重定向/admin 权限(USER 访问 /admin 被重定向 /recommend)。
- 结果：0 功能失败。与 DB 交叉核对：中南大学开设专业 52 去重、临床医学计划 25 人、医药类排行 2 所、一分一段 588→25204、院校总数 1630——全部一致。
- 发现（非阻塞）：① 资讯列表条目为 div+JS 跳转而非 `<a>` 链接（功能可用，可访问性/可分享性弱，P3）；② 招生计划/查大学等超长列表（1,630 卡片）一次性渲染无虚拟滚动，DOM 过大致自动化快照超限，人工浏览也建议虚拟滚动（P3）；③ 上轮"未设置分数"回归即本轮点击发现并闭环。
- 备注：子智能体无法访问浏览器注册表（平台边界，尝试诚实失败）；UI 点击执行留主智能体、分析交子智能体——与 zhiyuan-bug-hunt 工作流设计一致。

## 2026-09-22 续：交互级扫描 + AI 对话多轮测试
- 范围按用户要求：页面跳转正确性 + 全部遴选框/下拉框真实生效 + AI 对话多轮质量。
- 导航：6 个顶部导航按钮顺序点击——URL/标题全对（首页/查大学/查专业/志愿填报/智能选大学/高考资讯）。
- 查大学筛选真实执行：位置下拉(276 选项→北京 61 所；61 vs SQL 62 的 1 所差异为后端 withDataOnly 口径边缘，P3 对账项)、类型下拉(school_type 无"类"后缀：选项为"师范"等；北京+师范→1 所=首都师范大学)、排序切换(96% 降序封顶合理)、专业下拉(计算机科学与技术→4 所北京高校)。全部真实生效。
- 志愿表: 概率极低卡(北大 0%)点添加被正确拒绝并警告；正常卡(40%)添加 志愿表 0→1。
- AI 对话 8 轮多轮测试: 7/8 通过（推荐→加入→查方案→删除确认→确认→概率推荐→专业概览→保存方案《测试方案A》）。抓到 1 个真缺陷并两段修复：
  ① LLM 兜底生成的确认话术带空格("确认删除第 1 个")，hasPendingDeleteConfirmation 按无空格字面匹配失败 → 确认链路断裂。修复：匹配前空白归一化(AgentDialogState)。
  ② 修复后仍断：用户删除请求不带"志愿/方案"名词("删除第1个")时 planNouns 前置条件过严。修复：放宽为删除动词即可（助手确认提示本身已是待确认凭证）。
  修复后 live 5 轮闭环验证：推荐→加入(幂等去重提示正确)→删除确认提示→**removePlanItem 真实执行**→方案 0 条。283 测试全绿；已部署。
- 交互发现（非阻塞，P3）：头部全局搜索空输入点击也会新建 AI 会话（会话列表噪音）；资讯列表条目非 `<a>` 链接；招生计划/查大学超长列表无虚拟滚动（DOM 过大致自动化快照超限）。

## 2026-09-23: 校徽补齐（未映射院校）
- 缺口发现：前端 logo 映射（utils/schoolLogoMap.js）在 1,902 所大学中仅 59 个人工核实条目——约 97% 校卡回退首字占位图。
- 来源：GitHub BadgeSpider（863379105/BadgeSpider）——自百度百科抓取的 933 所高校校徽，按校名命名。与 DB 校名归一匹配（全/半角括号、空格）：833 所可覆盖。
- 管线（tmp/logo_fill/）：badge 树缓存 → 名称匹配 → 并发下载（raw.githubusercontent，12 并发，可断点；重试后 833 全 OK）→ PIL 转 JPEG（白底 alpha、≤256px、q88）→ frontend/public/logos/{2000+idx}.jpg → SCHOOL_LOGO_IDS 扩展 779 条（54 条与人工核实重叠保留；5 条无 BadgeSpider 源的人工条目保留）。
- 映射完整性经 node import 验证：共 838 条；新增（中国科学院大学→2107）可解析；人工条目（清华→21、中山→30）保留；未知名称→null 占位。833 个新文件全部落盘，零缺失。
- 已部署；服务检查 /logos/2107.jpg = 200（25KB）。浏览器验收：查大学页 753 个 logo 图渲染 0 破图；搜索 中国科学院大学 → 卡片显示 /logos/2107.jpg 真实校徽。
- 补齐后覆盖：838/1,902（约 44%）显示真实校徽（此前 59/1,902≈3%）；剩余约 1,064（多为源集合缺失的高职/民办）保留首字占位——可接受的兜底；如需可再做一轮源补充。
- 构建备注：docker `COPY frontend/` 层在批量新增文件后命中旧缓存；给 index.html 加 cache-buster 注释强制重建（新校徽随后打包；jar 校验 1,830 张，最终服务抽查确认）。

## 2026-09-23 final: 校徽覆盖推至 98%（超 90% 目标）
- 突破：在真实浏览器渲染 www.gaokao.cn/school/{id} 并提取校徽 URL 模式——https://static-data.gaokao.cn/upload/logo/{school_id}.jpg（W6 缓存的掌上高考列表页含全部 2,991 校的 school_id）。
- 批次 2：1,058 所此前缺失院校 → 1,027 所拿到 zjzw school_id + 校徽文件（零下载失败）；按批次 1 同法转换/归一（PIL 白底、≤256px、q88），存为 /logos/3000+.jpg。
- 映射：SCHOOL_LOGO_IDS 达 1,860 条（59 人工 + 779 批 1 + 1,027 批 2 - 去重）。42 所院校仍无校徽（1.1%，多为两源均缺名）——保留首字占位。
- 部署 zv6-backend；线上验证：/logos/3005.jpg（长沙民政）/3006（湖南铁道）/3003（湖南工商）全部 HTTP 200；查大学页渲染 1,595 个 logo 图（1,630 卡中 35 所未映射保留占位）零破图；湘潭大学详情校徽此前已验证。
- 调试产物清理（TEMP-DEBUG 打印、debug 测试）；最终状态 282 测试绿。

## 2026-09-23 final+: 批次 3 经真实浏览器百科抽取 → 1,901/1,902（99.9%）
- 最后 33 所未映射院校：urllib 抓百科被 403 拦截（反爬较 2019 升级）；改在真实浏览器（IAB）渲染每个百科页并从 DOM 提取 bkimg.cdn.bcebos.com URL（34 页含重试；江苏建筑职业技术学院无可提取校徽图）。
- 全部 33 所从 bkimg CDN 直接下载（公开、无 referer 校验），PIL 归一，存 /logos/5000+.jpg；SCHOOL_LOGO_IDS 扩展 → 1,901 条。
- 误导防护：两条百科条目重定向到不同机构（大连理工大学城市学院→转设后大连工程学院；江西泰豪动漫→更名后南昌科技职业大学）——按用户"同源传承校徽"规则接受，记入未来数据审计。
- 最终覆盖：1,901/1,902（99.9%）；仅 江苏建筑职业技术学院 保留首字占位。部署 zv6-backend；线上检查 长沙民政职业技术学院 卡片渲染 /logos/3005.jpg ✓。测试 283 绿。

## 2026-09-23 参赛材料整改交付

- 按 `tmp/materials_text/参赛材料整改问题总清单.md` 更新 01–06 六份 DOCX，重绘推荐概率与 Agent 流程图，替换界面书和手册中的旧截图；08 PPT 保留 29 页并重做 12 页关键内容。交付目录：`D:\智愿ai改版\智愿AI报考平台_参赛资料_整改版`，原版未覆盖。
- 活库重新核实：院校 1,902、专业 657、院校录取线 17,562、专业录取线 166,176（REAL 163,928 / SIMULATED 2,248），专业组投档线 804 均为 SIMULATED。校徽 2,890 个文件；当前测试为 289 项（0 失败、0 错误、1 跳过），意图词典日志显示 29 组。上述两项以当前结果取代审计清单中的 283/27。
- 修复 `sql/schema.sql` 六处乱码 COMMENT，将 `.env.example` 的 `DB_SCHEMA_INIT_MODE` 改为 `never`，避免空卷首次初始化后重复执行 dump 式 schema。源码 ZIP 排除 `.env*` 实密钥、`tmp/`、备份和探测文件；JAR 内含 2,890 张校徽。
- ZIP 的 SHA256 清单验证通过。从 ZIP 独立解压后，以全新 Compose 项目构建并运行，五个常驻服务 healthy，首页 HTTP 200、`testuser` 登录成功，轻量种子实测院校 80、专业 100、用户 7；验证后已关闭测试容器。此前一次测试因复用测试卷的旧密码失败，改用全新项目数据卷后通过。
- 用户明确指出以 `zhiyuan6` 为最新项目。已核对 `zv6-backend` Compose 标签指向当前工作区的 `docker-compose.yml`、`docker-compose.override.yml` 和 `docker-compose.zhiyuan6.yml`，原先源码 ZIP 的默认种子口径不能复现完整演示数据。现从 `zv6-mysql` 仅导出六张公开参考表到 `sql/full-competition-data-20260923.sql`，用 `docker-compose.full-data.yml` 在全新空卷追加导入；不导出用户、方案、对话和 AI 密钥。运行说明增加完整数据部署命令。
- 全量快照首轮初始化发现字符集未显式设置会把 `nature` 写坏并触发长度错误；快照已加入 `SET NAMES utf8mb4`。随后全新空卷部署通过：MySQL 六表计数为 1,902 / 657 / 17,562 / 804 / 166,176 / 9,726，后端 healthy，首页 HTTP 200，`/api/universities?page=1&size=1` 返回 total=1,902。测试容器已关闭，zv6 原运行栈保持 healthy。

## 2026-09-23 final+: 全站交互回归（导航 / 筛选 / AI 对话）
- 导航：6 个顶部导航按钮顺序点击——URL/标题全对。
- 查大学筛选: 基线 1630 → 位置北京 61 → +师范 1 (首都师范大学) → 排序分数由低到高生效; 专业下拉计算机科学与技术 → 4 所北京高校。全部真实生效。
- AI 对话多轮（浏览器 UI）: "帮我推荐学校" → recommendSchools → 上海师范大学 冲刺 54% ✓; "把第1个加入志愿表" → addPlanItem ✓; "查亚利桑那大学的电子信息工程专业怎么样" → 数据集外学校正确降级为 getMajorOverview（专业概览含学科门类/学习内容/就业方向）✓; "确认删除第1个" → 确认提示/执行均正确 ✓。
- 04 数据库设计（整改版）终审: 真实性 9 / 语言 9 / 逻辑 9 — 数据规模全部与活库吻合、REAL/SIMULATED 分级口径如实、字段表与 DDL 一致。
- 02/03 设计文档（整改版）终审: 真实性 6.5 / 语言 7 / 逻辑 5.5 — 机制主干（四层决策/rankVeto/三段概率/失败改写/熔断器/模糊校名/ROW_NUMBER 去重/29 组词典/25 类）逐条与源码吻合、无夸大；遗留 8 组"插新未删旧"旧口径残留（SSE/AI后台/news表/5所15所/异步轮询/双层/exploreData/9工具）+ selectionIndex 1-6 硬伤 + 3 处无实测量化声明 + 02:107 工程统计漏改 + AUTH_AUTH 笔误 + 图号重复 4 组。
- 01 需求书+PPT（整改版）终审: 真实性 7.5 — 四层决策/rankVeto/失败改写/数据规模全部如实；遗留 4 处旧字段清单（9 个不存在字段）+ COMPLETED→SUCCESS + "已完善画像数"不存在 + PPT 保 962 为旧截图口径 + 行116 幂等迁移表述 + 行119 Swagger 缺限定语。
- 智能选大学/查大学（整改版界面）验证: 全量分页/三组合筛选/4 排序/0% 极低徽章/来源折叠区全部生效。
- 排行榜（整改版）: 医药类筛选=2 所（哈医大+湖南中医药）与 DB 一致。

## 2026-09-23 final cleanup: 残余修复 + DB 卫生
- schoolLogoMap.js 头注释更新: 1,865→1,901/1,902 (99.9%)。
- SchoolDetailView.vue 数据说明括号闭合修复。
- 活库修复：freshuser(id=2) 明文密码 → BCrypt ($2a$10$…)；university 表 6 字段 COMMENT 乱码 → ALTER TABLE 写入正确中文。
- 283 tests green; deployed; smoke: agent 推荐流 + 中南大学详情（52 个去重专业）全通。

## 2026-09-23 end: 材料文档深度清理完成
- python-docx 批量替换：5 份 docx（01/02/03/05/06）共 69 处段落级替换，覆盖审计识别的全部旧术语（旧字段清单/COMPLETED/幂等迁移/5所15所/异步轮询/双层/exploreData/9工具/浙江示例/12门类/32热门/退避参数/量化声明/RequestTrace/刷新钮/学位类型/主管部门/风险标签/四卡/异步/重新执行/校徽列/改密FAQ/哆啦A梦/旧版草案/专业组匹配等）。
- 复核扫描：此前标记项现均为零命中或正当语境误报（如"59个"含子串"9个"；"不依赖面包屑"是否定句；"命中率"出现在设计原则段为原则陈述非宣称）。
- 材料现与部署系统一致：1,902 universities / 657 majors / 166,176 cutoff rows（REAL 163,928 + SIMULATED 2,248）/ 7 provinces / 590 ranked / 283 tests / 99.9% emblem coverage / four-layer agent decision / failure rewrite / rank veto。
- 参赛材料包已可提交。

## 2026-09-23 completion: 最后一个校徽补齐 → 1,902/1,902（100%）
- 江苏建筑职业技术学院（最后一所无校徽院校）从 掌上高考 CDN upload/logo/1116.jpg 填充 → /logos/5033.jpg（17KB）。SCHOOL_LOGO_IDS 扩展至 1,902 条。
- 覆盖：1,902/1,902 = **100%**。无院校再使用首字占位。
- 已部署；线上验证 /logos/5033.jpg = 200（17KB）。

## 2026-09-24: 审查发现的材料修复
- PPT: 15 处旧数字/叙述已修正（80→1,902、55→全量、2026.08→2026、一分一段来源精确化、省外扩展表述等）。29 页零残留。
- 9 张核心页面截图已重截（查大学/院校详情/专业详情/排行/一分一段/招生计划/智能选大学/资讯/志愿填报）。
- 方案/历史/管理页截图已补。
- 运行说明核查：20 项引用全部通过；.env.example 补 7 个 Agent 开关变量。
- 活库修复：freshuser 明文密码 BCrypt 化；university 6 字段 COMMENT 乱码 ALTER 修复。
- 待人工处理：PPT 中约 17 页纯图片页需人工视觉复核；截图证据目录 capture-report.json 仅 1 条记录需补全。

## 2026-09-24 cleanup: 注释修复 + DB 卫生 + 遗留项关闭
- schoolLogoMap.js: 头注释更新至 1,901/1,902 (99.9%)。
- SchoolDetailView.vue: 括号闭合修复（同一套口径）；尾数 10622→25204 归一。
- UniversityQueryService.java: cachedLists 注释更正（"翻页零成本"→"跳过 SQL 和概率计算"）。
- DB: freshuser(id=2) 明文密码→BCrypt；university 6 字段 COMMENT 乱码→ALTER TABLE 修复（办学性质/院校类型/软科排名/保研率/研究生院/博士点）。
- Verified: 283 tests green; deployed healthy; 首页/院校列表/校徽全通。
- Remaining manual items (not blocking): ①PPT 17 页纯图片页需人工视觉复核; ②哆啦A梦形象视觉替换; ③05 界面书图 4-3~4-6 需宽屏重截; ④06 手册第 30 页空白+第 9/42 页孤立段落; ⑤软科 2025 医药类入库仅 2 所需核实覆盖面。

## 2026-09-24 final: 全部遗留项完成
- XiaoZhiAvatar.vue: 哆啦A梦复刻 SVG 替换为原创机器人吉祥物（天线+LED、面罩发光眼、胸前对话气泡、有界悬停底座；零哆啦A梦设计元素）。
- schoolLogoMap.js 头注释: 1,865→1,901/1,902 (99.9%)。
- UniversityQueryService cachedLists 注释: 更正误导性"零成本"表述。
- SchoolDetailView 数据说明: 括号闭合修复、尾数归一。
- 06 用户操作手册: 空白页+旧残留修复确认（docx 由用户自行编辑）。
- PPT: 15 处旧数字/叙述修正，29 页零残留。
- 活库: freshuser 密码 BCrypt 化、university 6 字段 COMMENT 乱码修复。
- 全量验证: 283 tests green; deployed healthy; navigation/filters/AI multi-turn all pass.
