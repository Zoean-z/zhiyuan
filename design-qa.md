# Design QA — 全局视觉、院校专业组、志愿单与 Agent

## Comparison target
- Source visual truth: `.project-loop/reference-home-1440x900.png`，队友分支 `2a3bb895` 的未登录首页。
- Final implementation: `.project-loop/implementation-home-final-1440x900.png`，当前分支 `/home` 未登录首页。
- Full comparison: `.project-loop/comparison-home-final.png`。
- Focused comparison: `.project-loop/comparison-header-hero-final.png`。
- Viewport: 1440 × 900 CSS px；两张截图均为 1425 × 891 px，设备像素比约 1，同一浏览器、同一未登录状态，无密度缩放。

## Findings
- 当前无未解决的 P0 / P1 / P2 问题。
- 字体与排版：均使用中文系统字体；顶部导航、Hero 标题与辅助文案的层级和字重保持一致。新版标题内容不同，但仍保持两行大标题和同等视觉重心。
- 间距与布局：保留双层页头、橙色横向导航、左侧主视觉 + 右侧操作区和下方快捷入口；新版删去重复板块后，首屏信息密度更低但结构未漂移。
- 色彩与 token：橙色主导航、蓝色 Hero、白色卡片和浅灰页面背景与参照一致；没有新增与参照冲突的装饰色。
- 图片与图标：公共首页使用 Element Plus 同源图标；院校页使用队友分支的原始 Logo 文件并保留文字回退，没有用自制 SVG、emoji 或 CSS 图形替代可见资产。Logo 来源许可仍是比赛交付前的独立阻断项。
- 文案与内容：首页不再显示无来源的冲稳保数量、院校热度、排行、招生计划或重复资讯入口；静态演示内容带有明确提示。
- 响应式：768 px 和 390 px 下 `bodyScrollWidth === bodyClientWidth`；390 px 最终检查没有被裁切的按钮或输入框，导航改为两行四列。

## Comparison history
1. 初次桌面对照发现公共搜索文案承诺“大学或专业”但只落到院校页，院校/专业卡片 CTA 也暗示会自动带入查询；已收窄搜索文案并改为明确的“进入志愿填报”。最终桌面截图与浏览器交互验证通过。
2. 初次 390 px 检查发现 4 个导航项位于横向滚动区外，属于 P2 可发现性问题；已在 640 px 以下改成两行四列，修订后截图为 `.project-loop/implementation-home-mobile-final.png`，布局探针显示无裁切交互控件。

## Interaction and console evidence
- 顶部搜索“北京大学”进入 `/schools?q=北京大学`，仅显示北京大学。
- “查专业”进入 `/majors`，搜索“人工智能”后仅显示对应条目。
- 资讯列表可进入详情页，来源原文链接可见。
- 未登录点击“志愿单”跳转 `/login?redirect=/plans`。
- Mock 登录后 `/recommend` 可见，文本推荐返回冲刺 5、稳妥 5，共 10 张院校卡片。
- 上述流程控制台 0 error / 0 warn。

## Residual P3 / constraints
- 参照 Hero 中的装饰圆形和轮播点未复制；它们属于代码绘制装饰，当前实现采用纯色蓝底，避免把装饰当业务功能。
- 参照的“免费注册”使用独立胶囊按钮；当前认证后端仍是同一登录/注册页入口，因此首页只保留真实可用的认证路径。

## 院校专业组补充对照
- Source visual truth：用户提供的院校专业组弹窗截图 `codex-clipboard-6a40071a-37c0-4dff-a6c6-b48fda535fd5.png`。
- Final implementation：`.project-loop/professional-group-1280x800.jpg`；并排对照为 `.project-loop/comparison-professional-group.jpg`。
- 结构还原：保留院校摘要、横向专业组标签、组内专业列表和底部加入操作；采用同一橙色强调、浅色边框与大面积留白。
- 业务差异：没有复制参照中无来源的“录取概率/计划人数/学费”等字段；改为显示现有专业最低分、最低位次以及可审计的固定样例提示。
- 交互验收：化学+地理用户进入 `[101]` 可选并成功写入现有志愿表；`[102]` 要求生物，页面可查看但专业和加入按钮均禁用。
- 响应式：`.project-loop/professional-group-mobile-390x844.jpg` 显示 390 px 下专业组横向滚动、专业卡片纵向排列，底部操作未被裁切。
- 当前无未解决的 P0 / P1 / P2；视觉差异属于为现有产品壳、真实字段和较窄抽屉做的必要适配。

## 登录后导航、志愿单与 Agent
- 桌面证据：`.project-loop/plans-orange-1280x800.jpg`、`.project-loop/agent-orange-final-1280x800.jpg`；移动证据：`.project-loop/plans-orange-mobile-final-390x844.jpg`、`.project-loop/agent-orange-mobile-final-390x844.jpg`。
- 登录后普通用户壳已改为与公共页一致的白色品牌栏和橙色横向导航；管理员继续使用原侧栏，避免破坏高密度管理表格。
- 390 px 下四个主入口“推荐查询 / AI 对话 / 历史记录 / 志愿单”均在首屏完整可见，页面探针 `body.scrollWidth === body.clientWidth`，没有横向溢出。
- 志愿单概览首轮窄屏出现最近更新被横向裁切，已改为两列换行布局并复检通过。
- Agent 保留三栏/两栏响应式结构、真实会话和志愿单选择，只把蓝色强调迁移到橙色；消息发送后收到 Mock 回复，四个主导航入口均到达正确路由。
- 最终新建浏览器页控制台 0 error / 0 warn；旧页曾记录一次已修复的 CSS 语法热更新错误，不作为最终证据。

final result: passed

# 推荐页与数据库专业组 QA（2026-08-18）

- 桌面：推荐查询头部、文本/分数切换、结果统计、筛选和横向院校结果已统一为橙色视觉语言；查询得到 15 条结果，控制台无 error/warn。
- 专业组：从学校详情响应读取 `[101]`/`[102]`；化学资料下前者可选、后者禁选，界面明确标注演示数据库来源。
- 窄屏：390×844 验证 `scrollWidth === clientWidth`，查询表单、工具栏和结果卡片按单列重排，无横向裁切。
- 数据边界：未复用队友按 ID 生成计划数、排名、三年分数等代码；录取概率仍仅显示原推荐服务结果。

## 普通用户统一顶栏 QA（2026-08-18）

- Source visual truth：用户截图 `codex-clipboard-0d44edec-81ca-4c0e-bc38-5699e053c593.png`，要求删除单独的页面标题行并把用户资料移入品牌行。
- 桌面实现：品牌位于左侧，成绩资料、编辑、头像、用户名和退出位于同一白色顶栏；下方仅保留完整橙色导航，没有第二条“推荐查询 / AI 对话”等页面标题栏。
- 入口完整性：浏览器逐一到达首页、查大学、查专业、志愿填报和 AI 助手；导航同时保留志愿单、历史记录和高考资讯。
- Agent 回归：视口高度 720px 时 `bodyScrollHeight === innerHeight`，工作区从导航下方开始并占满剩余空间，没有为已删除标题栏保留空白。
- 390px：品牌、编辑、用户名和退出同排；8 个导航入口两行四列；`scrollWidth === clientWidth`，无水平裁切。
- 当前无未解决 P0 / P1 / P2。

final result: passed

# 志愿表合并与统一宽度 QA（2026-08-18）

- 参照：用户本轮提供的详细模式、专业下拉、表格模式和历史方案四张截图。
- 已实现：同校合并专业、服从调剂开关、详细/表格模式、历史后端方案、普通业务页 `1180px` 内容宽度。
- 自动验证：前端 10 tests、Mock/生产构建与 `git diff --check` 通过；后端 88 tests（1 skipped）通过。
- 用户明确要求“视觉测试我来做”，因此本阶段没有打开、点击或截图对照页面，不能把构建成功表述为视觉验收成功。

final result: blocked
blocker: final visual comparison and interaction inspection delegated to the user by explicit request

# 登录文案与公共顶栏横向基准 QA（2026-08-18）

- 参照：用户提供的登录页局部截图，以及主页现有 `GkHeader` 作为公共顶栏位置标准。
- 已修复：登录页辅助文案取消 64px 左偏移，与品牌标题左边线一致；根滚动容器固定预留滚动条槽，避免主页、志愿填报和 AI 助手因页面滚动状态不同而整体横移。
- 自动验证：前端 11 tests、生产构建、JAR 打包、实际服务静态资源一致性和 `git diff --check` 通过。
- 用户继续负责视觉测试，因此本阶段没有打开页面、点击控件或生成同视口实现截图，不能宣称视觉对照通过。

final result: blocked
blocker: same-viewport visual comparison across home, recommendation, agent and login is delegated to the user

# 志愿表标签、登录稳定尺寸与 Agent 两栏 QA（2026-08-18）

- 参照：用户提供的志愿表详细模式和 Agent 右侧志愿表预览截图。
- 已实现：档位标签改为 8px 圆角矩形；桌面登录卡片使用固定视口高度，表单区域可内部滚动；Agent 删除右侧志愿表预览，改为 250px 会话栏加自适应聊天区。
- 推荐学校卡片、消息行、气泡、卡片标题/统计/底部文本均增加 `min-width: 0`、最大宽度和长文本换行约束。
- 自动验证：前端 11 tests、生产构建、JAR 打包、实际服务资源检查和 `git diff --check` 通过。
- 用户继续负责视觉测试，因此没有打开或点击页面，也未生成同视口实现截图。

final result: blocked
blocker: final visual comparison for plan tags, login mode switching and agent card wrapping is delegated to the user

# 管理员登录、注册滚动与资料页次级按钮 QA（2026-08-18）

- 管理员登录：真实接口已验证 `adminuser` 返回 200/ADMIN，令牌可访问管理接口；错误密码保持 401，前端在该提交场景显示“管理员账号或密码错误”。
- 注册区域：桌面认证面板由内部 `overflow-y: auto` 改为 `overflow: hidden`；980px 以下仍沿用外层页面自适应布局，不新增内部滚动容器。
- 资料页：保存按钮下方的“取消编辑/返回登录”次级按钮及对应无用跳转代码已删除。
- 自动验证：前端 11 tests、生产构建、JAR 打包、生产 chunk 内容与 8080 静态入口一致性检查通过。
- 用户继续负责视觉测试，因此没有打开页面、点击控件或生成对照截图。

final result: blocked
blocker: final visual inspection of registration height and profile form spacing is delegated to the user

# 认证页与管理员入口 QA（2026-08-18）

- 登录、注册与首次资料页已统一使用 `GkHeader`，认证控件、选中态、验证码按钮和管理员入口使用现有橙色体系。
- 登录页新增管理员弹窗表单；注册页新增邮箱和六位验证码，并提供 60 秒重发倒计时。
- 自动验证：前端 11 tests、生产构建、生产资源哈希匹配、后端 93 tests（1 skipped）和 JAR 打包通过。
- 用户继续负责视觉测试，因此本阶段没有打开页面、点击控件或生成截图对照；不能把构建与接口成功表述为视觉验收成功。

final result: blocked
blocker: final visual comparison and interaction inspection delegated to the user by explicit request

# 志愿单生产资源同步 QA（2026-08-18）

- 用户截图显示页面模板存在但局部样式整体缺失；源码 `PlansView.vue` 与 Mock 产物中的样式完整，问题不在视觉实现本身。
- 根因是此前生产构建验证后恢复了 `src/main/resources/static`，而 Spring Boot/Docker 正是从该目录交付前端，形成当前模板与旧懒加载资源混用。
- 已保留最新生产构建；入口主包明确映射当前 `PlansView-DdKWnfDG.js` 和 `PlansView-BmLvyVQ9.css`，CSS 含 `.plan-overview` 与 `.plan-school-row` 的 scoped 规则。
- 源码静态目录、`target/classes/static` 与重新生成的 Spring Boot JAR 已完成文件存在性和哈希一致性检查。
- 按用户要求没有打开或点击页面；服务重启和强制刷新后的最终视觉确认仍由用户完成。

final result: blocked
blocker: final visual verification after backend restart and browser cache refresh delegated to the user

# 首页热点资讯与热门院校 QA（2026-08-18）

- 参照：用户提供的热点资讯和热门院校两张截图。
- 已实现：全宽热点资讯主次层级、8 张院校卡片、类型筛选、换一换和现有路由跳转。
- 自动验证：前端 11 tests、Mock/生产构建与 `git diff --check` 通过。
- 用户继续负责视觉测试，因此本阶段没有打开页面、点击控件或生成实现截图进行对照。

final result: blocked
blocker: final visual comparison and interaction inspection delegated to the user by explicit request
