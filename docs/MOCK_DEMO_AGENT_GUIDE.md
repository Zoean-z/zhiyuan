# Mock 演示 Agent 指南

## 目的

本指南用于让队友或自动化 Agent 在没有 Java、MySQL、Redis、RocketMQ 和 AI Key 的环境中查看当前前端。Mock 模式只服务于界面浏览、讲解、截图和文档编写，不是正式接口或真实数据验收环境。

项目已经进入比赛交付冻结。部署 Agent 不得借部署之机新增页面、修改业务流程、调整接口/数据结构、重做视觉体系或生成第二套 Mock 数据。

## 最短启动方式

在仓库根目录执行：

```powershell
cd frontend
npm ci
npm run dev:mock
```

访问 `http://localhost:5173`。

如果 5173 端口被占用：

```powershell
npm run dev:mock -- --port 5174
```

## 构建并预览静态 Mock 包

```powershell
cd frontend
npm ci
npm test
npm run build:mock
npm run preview:mock -- --port 4173
```

访问 `http://localhost:4173`。构建产物位于 `frontend/dist-mock`，可交给支持 SPA 回退的静态托管服务。不要把 `dist-mock` 复制到 `src/main/resources/static`，后者是正式后端托管的生产前端。

## 演示账号

| 身份 | 用户名 | 密码 |
| --- | --- | --- |
| 普通用户 | `testuser` | 任意非空密码 |
| 管理员 | `admin` | `admin123` |

若浏览器保留了旧登录状态，先点击右上角“退出”，再重新登录。

## 建议检查路径

1. 首页：热门院校、热点资讯及公共导航可见。
2. 查大学、查专业：列表、搜索、专业详情和开设院校可打开。
3. 志愿填报：分数查询或文本查询能返回学校，学校专业可以加入志愿单。
4. 志愿单：同校专业合并、服从调剂、详细/表格模式和方案保存可演示。
5. AI 助手：可以创建会话、发送消息并看到 Mock 回复；不代表真实模型或后端 Tool Calling 已联网。
6. 管理端：使用管理员演示账号查看列表和表单交互。

## Mock 边界

- 数据只存在于浏览器当前会话内存，刷新后恢复初始状态。
- 不连接数据库，不发送真实邮件，不调用真实 AI，也不验证真实 JWT 权限。
- 推荐分组和概率只用于演示交互；真实业务验收必须运行 Spring Boot 与 MySQL。
- 2026 录取线属于固定竞赛演示数据，不得在文档中写成考试院官方数据。
- Mock 通过 `frontend/src/utils/mock.js` 提供；不得另建 localStorage、JSON 服务或第二份数据模型。

## 给部署 Agent 的可复制任务

```text
你只负责启动和验证本仓库的前端 Mock，不修改任何业务代码、接口、数据结构、样式或 Mock 数据。
1. 切换到指定分支并进入 frontend。
2. 执行 npm ci、npm test、npm run build:mock。
3. 执行 npm run preview:mock -- --port 4173，并确认 http://localhost:4173 可访问。
4. 用 testuser/任意非空密码检查普通用户入口，用 admin/admin123 检查管理端入口。
5. 只报告命令结果、访问地址、失败日志和截图；不要自行修复或提交代码。发现问题后交回项目负责人决定。
```

## 失败处理

- Node 版本不满足时，使用 Node `20.19+` 或 `22.12+`。
- 依赖安装失败时，保留完整错误信息，不删除 lockfile、不升级依赖。
- 页面请求真实 `/api` 通常说明没有使用 `mock` mode；重新执行 `npm run dev:mock` 或 `npm run build:mock`。
- 部署后直接访问子路由 404，说明托管服务没有把未知路径回退到 `index.html`；修复托管配置，不修改 Vue 路由。
- 测试或构建失败时停止，不要用跳过测试、改测试或临时硬编码掩盖问题。
