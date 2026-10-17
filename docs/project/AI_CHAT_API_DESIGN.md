# 智愿AI报考平台 — AI 对话后端接口实现设计

> 版本：V1.0　日期：2026-08-14
> 关联前端：`frontend/src/components/AgentWorkspace.vue`（已按「掌上高考」风格完成界面重构）
> 参考界面：https://www.gaokao.cn/chat

---

## 1. 背景与目标

前端 AI 对话界面已重构为类「掌上高考」风格：左侧会话栏 + 居中对话列 + 空态引导（推荐问题池、换一批）+ 问答气泡 + 工具轨迹 + 推荐卡片。

当前后端 `POST /api/agent/conversations/{id}/messages` 为**全量回合返回**（一次请求返回整轮 `generatedMessages`），前端只能等待完成后一次性渲染，缺少目标站的核心体验：**流式打字机输出**。

本设计在不改变既有数据模型（`agent_conversation` / `agent_message`，含 `request_id` 幂等键、`payload_json` 扩展字段）的前提下，新增流式链路与若干会话管理接口，并保持旧接口兼容降级。

### 体验目标

| 指标 | 目标 |
| --- | --- |
| 首 token（首字）延迟 | P95 < 1.5s（LLM 可用时） |
| 无 AI 降级（规则模板） | 全量 < 2s |
| 断线重连恢复 | 3s 内自动续流，不丢消息 |
| 工具调用可见性 | tool_call / tool_result 实时上屏 |

---

## 2. 现状梳理（既有接口，保持兼容）

| 方法 | 路径 | 说明 | 状态 |
| --- | --- | --- | --- |
| POST | /api/agent/conversations | 创建会话（title 可选） | 既有 |
| GET | /api/agent/conversations | 会话列表（含 messageCount / updatedAt） | 既有 |
| GET | /api/agent/conversations/{id} | 会话详情（含 messages 全量） | 既有 |
| POST | /api/agent/conversations/{id}/messages | 发送消息，返回整轮 generatedMessages | 既有（保留为降级路径） |

消息模型（agent_message 表）核心字段：`role`（user/assistant）、`messageType`（text/tool_call/tool_result）、`toolName`、`content`、`payloadJson`（JSON 扩展）、`requestId`（幂等）、`createdAt`。

---

## 3. 新增接口清单

### 3.1 流式对话（核心新增）

**POST /api/agent/conversations/{id}/stream**

请求体（与旧接口一致，便于复用 DTO）：

```json
{
  "content": "湖南物理类560分能上哪些大学？",
  "planId": 3,
  "requestId": "uuid-v4"          // 幂等键，可空（后端自动生成）
}
```

响应：`Content-Type: text/event-stream; charset=utf-8`，`Cache-Control: no-cache`，`X-Accel-Buffering: no`（关闭 Nginx 缓冲）。

SSE 事件协议：

```
event: heartbeat      data: {}                              // 每 30s 心跳，保活与探测
event: tool_call      data: {"toolName":"recommendSchools","args":{}}
event: tool_result    data: {"toolName":"recommendSchools","content":"已生成6条院校推荐","payload":{"topItems":[...]}}
event: delta          data: {"seq":1,"text":"根据你的 **560 分** 与位次，"}   // 文本增量
event: message        data: {"message":{ ...完整 agent_message JSON... }}    // 消息落库后广播（用于历史一致性）
event: done           data: {"conversationId":12,"messageCount":6}
event: error          data: {"code":"LLM_TIMEOUT","message":"模型响应超时，已切换规则模板"}
```

事件顺序约定：

1. 请求到达 → 用户消息立即落库 → 广播 `message`（user）
2. 若触发工具调用 → 依次广播 `tool_call` → 工具执行 → 广播 `tool_result`（工具消息**即时落库**并广播，前端据此渲染轨迹与卡片）
3. 文本生成 → 按增量广播 `delta`（每片约 20~60 字符，含标点边界），流结束后整体落库并广播 `message`（assistant）
4. 全部完成 → 广播 `done`

`seq` 为全局递增序号（用户消息=1，之后每条工具消息、每个 delta 增量、每条落库消息各占一个），用于断线重连。

### 3.2 会话管理（补齐侧栏交互）

| 方法 | 路径 | 请求体 | 说明 |
| --- | --- | --- | --- |
| PUT | /api/agent/conversations/{id} | `{"title":"...","favorite":true}` | 重命名 / 收藏，字段均可选，支持部分更新 |
| DELETE | /api/agent/conversations/{id} | - | 软删除（deleted 标记），返回 204 |
| GET | /api/agent/conversations/{id}/messages | `?afterSeq=12&limit=50` | 增量拉取（断线重连补数据），按 seq 升序 |

### 3.3 推荐问题池（空态引导）

**GET /api/agent/quick-questions**

无参数；后端结合用户画像（省份/科类/分数已填时）动态拼接个性化问题，未填画像时返回通用池：

```json
{
  "questions": [
    "湖南丨物理类丨560分能上哪些大学？",
    "帮我看看我的画像信息",
    "看看我当前的志愿方案",
    "帮我推荐计算机专业"
  ],
  "generatedAt": "2026-08-14T10:00:00+08:00"
}
```

个性化拼接规则（服务端规则模板，不依赖 LLM）：

- 已有 `score + subjectType + examProvince` → 首条替换为「{省份}丨{科类}丨{分数}分能上哪些大学？」
- 已有志愿表 → 插入「帮我分析当前方案的冲稳保比例」
- 池内问题与前端 `QUESTION_POOL` 保持同一来源（后续前端改从该接口拉取，替换本地常量）

---

## 4. 服务端实现要点

### 4.1 控制器层

新增 `AgentStreamController`（或扩展 `AgentController`）：

```java
@PostMapping("/{id}/stream")
public SseEmitter streamMessage(@PathVariable Long id,
                                @Valid @RequestBody AgentMessageCreateRequest request) {
    SseEmitter emitter = new SseEmitter(300_000L);   // 5min 超时
    agentChatService.streamMessage(currentUserId(), id, request, emitter, currentUser());
    return emitter;
}
```

要点：

- `SseEmitter` 超时 5min，`onTimeout` / `onError` / `onCompletion` 统一释放线程与任务句柄
- 使用 `CompletableFuture` 或独立线程池执行，不占用 Tomcat 工作线程（`emitter.send` 线程安全，需同步）
- 认证沿用 `UserContext` + JWT（`Authorization: Bearer`，fetch 流式不受 EventSource 无 header 限制）

### 4.2 服务层改造（AgentChatService）

新增 `streamMessage(...)`，与既有 `sendMessage(...)` 共享核心决策链（AgentDecisionService → 工具注册表 → LLM 客户端），仅在**输出通道**上分叉：

```
AgentChatService.streamMessage
 ├─ 1. 幂等检查：request_id 已存在 → 直接重放既有消息流（广播 message 后 done）
 ├─ 2. 用户消息落库 + 广播
 ├─ 3. 决策循环（maxToolCallsPerTurn=3，沿用现有配置）：
 │     ├─ LLM 输出含工具调用 → 广播 tool_call → AgentToolExecutor 执行 → 落库 + 广播 tool_result
 │     └─ 纯文本 → 流式 token 增量 → 累积
 ├─ 4. 文本消息落库 + 广播 message
 └─ 5. 广播 done；异常 → 广播 error（降级规则模板后仍补发 done）
```

**LLM 流式接入**：Qwen 客户端增加 `stream` 模式（OpenAI 兼容 chat completions 的 `stream=true`），按 `choices[0].delta.content` 切片转发；流中断/超时 → 抛 `LLMStreamException` → 服务层捕获后**回退规则模板**（将模板文本以同样方式逐片推流），保证前端无感。

**RocketMQ 异步任务协同**：推荐类工具若命中异步任务路径（`recommendation_task` 状态机），`tool_result` 事件由任务完成回调推送；超过 20s 未完成时先广播 `tool_result`（`errorCategory=PENDING` 提示「任务处理中」），完成后由 `done` 前的补发事件收敛。前端对同一 `toolName` 的多次 tool_result 做合并覆盖渲染。

### 4.3 断线重连与消息一致性

- 前端断线后以 `Last-Event-ID: {seq}` 重连：后端从会话消息表中查出 `seq > lastSeq` 的已落库消息（消息表新增 `seq` 列，迁移脚本走既有幂等模式），先补发 `message` 事件，再继续当前流
- 正在生成的流若断线，重连请求携带同一 `requestId` → 幂等命中 → 完整重放已落库消息 + 若生成未完成继续推送（生成任务句柄注册在内存 Map<requestId, emitter>，单实例部署可用；多实例部署时退化为「重放已完成消息 + 提示刷新」，不阻塞主流程）
- 心跳 30s：Nginx 等代理默认 60s 空闲断开，心跳保证链路存活；客户端 45s 无任何事件即触发重连

---

## 5. 前端接入约定（AgentWorkspace.vue）

当前前端已就绪的 UI 状态（空态/对话态/工具轨迹/推荐卡片/抽屉）与后端事件一一对应：

| 后端事件 | 前端行为 |
| --- | --- |
| message（user） | 渲染右侧橙色气泡 |
| tool_call | 渲染虚线 chip「正在调用工具 · xxx」 |
| tool_result | 渲染结果 chip + 载荷卡片（画像/志愿表/推荐卡片） |
| delta | 追加到当前回答气泡，`renderMarkdown` 增量渲染，自动滚动 |
| done | 结束流，刷新会话列表（messageCount/updatedAt），关闭打字指示器 |
| error | 气泡下方展示错误条，不中断会话 |

消费方式：`fetch` + `response.body.getReader()` 解析 `text/event-stream`（POST 需 body，故不用原生 EventSource）；若响应头非 `text/event-stream`（后端降级），直接按旧接口 JSON 处理。

**实现状态（已落地并通过浏览器实测）**：

- 前端 SSE 消费链路已实现：`runStreamTurn`（事件解析 + delta 增量追加）+ `runLegacyTurn`（旧接口降级，客户端打字机 `revealTextProgressively` 补齐体验，两条路径 UI 状态一致）
- 打字光标 `.gk-cursor`、生成中停止按钮（`AbortController` 中断并保留已生成部分）、工具轨迹 chip 顺序出现均已实测验证
- mock 层已提供 `/api/agent/conversations/{id}/stream` 伪 SSE（`ReadableStream` Response），`dev:mock` 模式走真实 SSE 代码路径，可完整演示
- 后端 `POST .../stream`（SseEmitter）仍待实现，当前真实后端自动走降级路径

---

## 6. 兼容与降级矩阵

| 场景 | 行为 |
| --- | --- |
| 旧客户端调 POST .../messages | 全量 JSON 返回（现状不变） |
| QWEN_ENABLED=false | 规则模板文本，逐片 delta 推流（体验一致） |
| LLM 流中断/超时 | 捕获后切规则模板，广播 error(code=LLM_TIMEOUT) 再 done |
| 前端不支持流式 | 回退旧接口 |
| RocketMQ 关闭 | 推荐工具同步执行（现状逻辑），tool_result 正常推送 |
| 网络断线 | Last-Event-ID 续流 + 增量拉取兜底 |

---

## 7. 验收标准

1. 100 轮混合对话（含工具调用）无丢失、无重复消息；`request_id` 幂等重放通过
2. 断网 5s 恢复：3s 内续流，最终消息序列与不断网基线一致
3. 首 token P95 < 1.5s；无 AI 降级全量 < 2s（复用 duration_ms 埋点，按 P95 统计）
4. 会话重命名/收藏/删除 + 增量拉取接口全部通过契约测试
5. 前端在 mock 模式与真实后端两套环境均可完整演示（保留 `dev:mock` 演示链路）

---

## 8. 实施顺序（建议）

1. **P0**：`streamMessage` 服务层改造（决策链复用 + 文本流式落库）+ SseEmitter 控制器 + 前端 fetch 流接入
2. **P1**：会话管理接口（PUT/DELETE/增量拉取）+ 前端侧栏交互（重命名/删除/收藏）
3. **P2**：推荐问题池接口 + 前端替换本地 QUESTION_POOL
4. **P3**：多实例下的 requestId → emitter 注册表迁移（Redis pub/sub 或 RocketMQ 广播），支撑水平扩展

> 注：前端界面、SSE 流式消费与 mock 演示链路（含伪 SSE）已完成并实测；后端 `streamMessage` 服务层与 SseEmitter 控制器待确认后动工，P0 中「前端 fetch 流接入」已就绪。
