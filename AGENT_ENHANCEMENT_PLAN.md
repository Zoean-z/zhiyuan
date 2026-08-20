# Agent Enhancement Plan

## 1. Goal

在保留现有推荐内核、用户体系、志愿方案能力的前提下，为项目新增一个 **自研轻量 tool-calling 编排层**，使其支持：

- 多轮对话
- 读取用户画像和当前志愿信息
- 调用受控工具完成推荐查询和志愿单操作
- 对用户操作结果做自然语言反馈

目标不是做通用 autonomous agent，而是做一个高考志愿场景下的受控对话式助手。

## 2. Non-goals

- 不重写现有 `RecommendationService`
- 不把推荐决策交给大模型
- 不引入微服务、MQ、向量库、工作流引擎
- 不让模型直接写数据库
- 不做无限循环自主 agent

## 3. Recommended Route

推荐路线：

1. 保留现有推荐 / 志愿单 service
2. 新增一个 `chat/agent orchestration` 模块
3. 通过自研 `tool calling` 流程接现有 service
4. 先做受控多轮会话，不做通用 autonomous agent

## 4. Architecture

建议新增模块：

- `controller/AgentController`
- `service/agent/AgentChatService`
- `service/agent/AgentConversationService`
- `service/agent/AgentToolRegistry`
- `service/agent/AgentToolExecutor`
- `service/agent/AgentToolFacade`
- `service/agent/tool/...`
- `service/agent/model/...`

职责划分：

- `AgentController`
  - 接收聊天请求
  - 查询会话
  - 返回对话响应

- `AgentChatService`
  - 组织一轮 agent 调用
  - 加载会话上下文
  - 调用现有大模型 client
  - 控制工具调用边界

- `AgentConversationService`
  - 保存会话和消息
  - 维护会话状态

- `AgentToolFacade`
  - 暴露受控工具
  - 复用现有业务 service

- `AgentToolRegistry`
  - 注册工具定义、参数约束、说明

- `AgentToolExecutor`
  - 按工具名执行对应工具
  - 负责工具参数校验和执行结果包装

## 5. Tool Design

第一版只建议开放以下工具：

- `getUserProfile`
  - 读取当前登录用户分数、科类、省份

- `getCurrentPlan`
  - 读取用户当前志愿方案或当前选择项

- `recommendSchools`
  - 基于条件调用现有 `RecommendationService`

- `recommendMajors`
  - 基于条件调用现有 `RecommendationService`

- `addPlanItem`
  - 向当前志愿单加入学校/专业

- `removePlanItem`
  - 从当前志愿单移除学校/专业

- `savePlan`
  - 将当前志愿单保存成正式方案

原则：

- 工具参数必须做后端校验
- 删除/覆盖类操作必须要求明确意图
- 工具只暴露必要字段，不暴露底层表结构

## 6. Data Model

建议采用 **结构化表 + JSON 扩展字段** 的混合建模，而不是把整个对话直接塞进一条 JSON 大记录。

原因：

- 会话列表、最近更新时间、状态等字段需要高频查询
- 消息需要分页读取，不能每轮都整段重写 JSON
- 工具调用需要审计和排错
- 同时又要保留对模型返回和工具结果的扩展性

### `agent_conversation`

建议字段：

- `id`
- `user_id`
- `title`
- `status`
- `last_message_at`
- `message_count`
- `created_at`
- `updated_at`

用途：

- 支持用户查看自己的会话列表
- 支持会话分页和最近会话排序
- 支持快速判断会话是否活跃

### `agent_message`

建议字段：

- `id`
- `conversation_id`
- `role` (`user` / `assistant` / `tool`)
- `message_type` (`text` / `tool_call` / `tool_result`)
- `content`
- `tool_name`
- `payload_json`
- `created_at`

字段说明：

- `content`
  - 存主文本内容，例如用户输入、assistant 最终回复、tool 简要结果摘要

- `payload_json`
  - 存扩展信息，例如：
  - tool 调用参数
  - tool 原始结果
  - 模型原始回复片段
  - token usage
  - trace / metadata

这种方式的好处是：

- 结构化字段可查、可分页、可排序
- JSON 字段保留灵活扩展能力
- 不需要频繁改表来适配 agent 消息格式变化

### Not Recommended

不建议第一版采用：

- `agent_conversation.messages_json`

这种“整段会话只放一列 JSON” 的方案，因为它会带来：

- 消息分页困难
- 每轮追加都要整段重写
- 工具调用审计困难
- 后续统计和排错成本高

## 7. API Draft

建议新增接口：

- `POST /api/agent/conversations`
  - 创建新会话

- `GET /api/agent/conversations`
  - 获取当前用户会话列表

- `GET /api/agent/conversations/{id}`
  - 获取会话详情和消息记录

- `POST /api/agent/conversations/{id}/messages`
  - 发送一条用户消息并触发一轮 agent

返回结果建议包含：

- `conversationId`
- `assistantMessage`
- `toolCalls`
- `toolResults`
- `planChanged`
- `currentPlanSnapshot`

## 8. Orchestration Strategy

建议采用自研轻量编排：

1. 读取当前会话最近 N 轮消息
2. 拼装 system prompt + 历史消息 + 工具列表
3. 调用现有大模型 client
4. 解析模型响应：
   - 直接回复
   - 或请求调用某个工具
5. 后端执行工具
6. 将工具结果再回填给模型
7. 返回最终 assistant 回复
8. 将用户消息、assistant 消息、tool 调用和 tool 结果全部落库

实现原则：

- 单轮工具调用次数限制，例如 `maxToolCallsPerTurn = 3`
- 工具执行失败要中断并返回可解释错误
- 工具调用参数必须后端校验
- 删除/覆盖类工具必须要求显式意图

## 9. Phase Plan

### Phase A: Agent Skeleton

- 新增 agent 会话与消息存储
- 打通单轮对话
- 实现 `getUserProfile` / `getCurrentPlan`
- 保存会话和消息

### Phase B: Recommendation Tools

- 接入 `recommendSchools`
- 接入 `recommendMajors`
- 让 agent 能基于用户画像生成推荐

### Phase C: Plan Operation Tools

- 接入 `addPlanItem`
- 接入 `removePlanItem`
- 接入 `savePlan`
- 完成“对话里操作志愿单”的闭环

### Phase D: Experience Hardening

- 对敏感操作增加确认策略
- 限制每轮工具调用次数
- 补充测试和错误处理

## 10. Risks

- 如果一开始就让 agent 直接接所有能力，边界会失控
- 如果没有消息落库和工具轨迹，后续很难排查问题
- 如果让模型决定推荐结果，会破坏当前项目的可解释后端规则定位

## 11. Acceptance

第一版完成标准：

- 用户可以创建 agent 会话
- 用户可以多轮追问
- agent 可以读取用户画像
- agent 可以调用推荐工具
- agent 可以把学校/专业加入或移出志愿单
- 全过程有会话和工具调用记录
- 推荐结果仍然由后端规则层决定
