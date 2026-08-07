<script setup>
import { ElMessage } from "element-plus";
import { Document, InfoFilled, Monitor, Promotion, School, User } from "@element-plus/icons-vue";
import { computed, nextTick, onMounted, ref, watch } from "vue";
import {
  buildGroupedFromResult,
  buildPlanItemKey,
  formatDateTime,
  normalizeItem
} from "../utils/recommendation";
import { UI_TEXT, createHttpError, normalizeUserError } from "../utils/ui";

const props = defineProps({
  token: { type: String, required: true },
  user: { type: Object, default: () => ({}) }
});

const emit = defineEmits(["jump-to-plans"]);
const conversationsLoading = ref(false);
const conversationLoading = ref(false);
const sending = ref(false);
const creating = ref(false);
const conversations = ref([]);
const activeConversationId = ref(null);
const messages = ref([]);
const draft = ref("");
const messageListRef = ref(null);

const plansLoading = ref(false);
const plans = ref([]);
const activePlanId = ref("");
const activePlanDetail = ref(null);
const activePlanItems = ref([]);
const addDialogVisible = ref(false);
const addSubmitting = ref(false);
const addTargetPlanId = ref("");
const addNewPlanName = ref("");
const pendingAddItem = ref(null);
const createPlanDialogVisible = ref(false);
const createPlanName = ref("");

const quickPrompts = [
  { label: "帮我看看我的画像信息", icon: User },
  { label: "看看我当前的志愿方案", icon: Document },
  { label: "帮我推荐学校", icon: School },
  { label: "帮我推荐计算机专业", icon: Monitor }
];

const conversationTitle = computed(() =>
  conversations.value.find((item) => item.id === activeConversationId.value)?.title || "新的志愿对话"
);
const activePlan = computed(() => plans.value.find((item) => String(item.id) === String(activePlanId.value)) || null);
const canSend = computed(() => !!draft.value.trim() && !sending.value && !!activePlanId.value);

function getAuthHeaders(extraHeaders) {
  return { ...(extraHeaders || {}), Authorization: `Bearer ${props.token}` };
}

async function apiFetch(url, options = {}) {
  const { timeoutMs = 15000, ...fetchOptions } = options;
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...fetchOptions, signal: controller.signal });
    const isJson = response.headers.get("content-type")?.includes("application/json");
    const data = isJson ? await response.json() : null;
    if (!response.ok) throw createHttpError(response, data, UI_TEXT.common.requestFailed);
    return data;
  } catch (ex) {
    if (ex?.name === "AbortError") throw new Error(UI_TEXT.common.timeout);
    throw ex;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

function resolveErrorMessage(ex, fallback) {
  return normalizeUserError(ex, fallback || UI_TEXT.common.operationFailed);
}

function parsePlanDetail(detail) {
  let parsed = {};
  try {
    parsed = detail?.resultJson ? JSON.parse(detail.resultJson) : {};
  } catch {
    parsed = {};
  }
  return { parsed, items: flattenPlanItems(parsed) };
}

function flattenPlanItems(resultObj) {
  const grouped = buildGroupedFromResult(resultObj || {});
  const items = [];
  [["rush", grouped.rush], ["safe", grouped.safe], ["guarantee", grouped.guarantee]].forEach(([strategy, list]) => {
    (list || []).forEach((item) => {
      const normalized = normalizeItem(item, strategy);
      items.push({ ...normalized, strategy: normalized.strategy || strategy, planKey: buildPlanItemKey(item, strategy) });
    });
  });
  return items;
}

function buildPlanResult(items, base = {}) {
  const groups = { rush: [], safe: [], guarantee: [] };
  (items || []).forEach((item) => {
    const normalized = normalizeItem(item, item?.strategy);
    const group = ["rush", "safe", "guarantee"].includes(normalized.strategy) ? normalized.strategy : "safe";
    groups[group].push({
      recommendationMode: normalized.recommendationMode,
      universityId: normalized.universityId ?? null,
      universityName: normalized.universityName,
      majorName: normalized.majorName || null,
      universityProvince: normalized.universityProvince || null,
      universityTier: normalized.universityTier || null,
      is985: normalized.is985 === true,
      is211: normalized.is211 === true,
      isDoubleFirstClass: normalized.isDoubleFirstClass === true,
      schoolTags: Array.isArray(normalized.schoolTags) ? normalized.schoolTags : [],
      universityTags: normalized.universityTags || null,
      cutoffScore: normalized.cutoffScore ?? null,
      scoreGap: normalized.scoreGap ?? null,
      userRank: normalized.userRank ?? null,
      minRank: normalized.minRank ?? null,
      rankGap: normalized.rankGap ?? null,
      recommendationBasis: normalized.recommendationBasis || null,
      admissionProbability: normalized.admissionProbability ?? null,
      strategy: group.toUpperCase(),
      strategyLabel: normalized.strategyLabel || null,
      riskScore: normalized.riskScore ?? null,
      matchReasons: Array.isArray(normalized.matchReasons) ? normalized.matchReasons : [],
      explanation: normalized.explanation || null
    });
  });
  return {
    recommendationMode: base.recommendationMode || items?.[0]?.recommendationMode || "SCHOOL_FIRST",
    rush: groups.rush,
    safe: groups.safe,
    guarantee: groups.guarantee,
    summary: `当前方案共选择 ${items.length} 条志愿结果。`,
    aiSummary: base.aiSummary || "",
    finalAdvice: base.finalAdvice || "",
    tips: Array.isArray(base.tips) ? base.tips : []
  };
}

async function loadPlans(preferredId = activePlanId.value) {
  plansLoading.value = true;
  try {
    const records = await apiFetch("/api/plans", { headers: getAuthHeaders() });
    plans.value = Array.isArray(records) ? records : [];
    const matched = plans.value.find((item) => String(item.id) === String(preferredId));
    const fallback = plans.value.find((item) => item.planName !== "当前方案草稿") || plans.value[0];
    activePlanId.value = matched ? String(matched.id) : fallback ? String(fallback.id) : "";
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "加载志愿表失败"));
  } finally {
    plansLoading.value = false;
  }
}

async function loadActivePlan() {
  if (!activePlanId.value) {
    activePlanDetail.value = null;
    activePlanItems.value = [];
    return;
  }
  try {
    const detail = await apiFetch(`/api/plans/${activePlanId.value}`, { headers: getAuthHeaders() });
    activePlanDetail.value = detail;
    activePlanItems.value = parsePlanDetail(detail).items;
  } catch (ex) {
    activePlanDetail.value = null;
    activePlanItems.value = [];
    ElMessage.error(resolveErrorMessage(ex, "加载当前志愿表失败"));
  }
}

async function createEmptyPlan() {
  const name = createPlanName.value.trim();
  if (!name) {
    ElMessage.warning("请输入志愿表名称");
    return;
  }
  creating.value = true;
  try {
    const result = buildPlanResult([], {});
    const saved = await apiFetch("/api/plans", {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ planName: name, sourceType: "score", sourceQuery: "AI 对话创建", resultJson: JSON.stringify(result), aiSummary: "" })
    });
    createPlanDialogVisible.value = false;
    createPlanName.value = "";
    await loadPlans(saved.id);
    await loadActivePlan();
    ElMessage.success(`已创建《${saved.planName}》`);
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "创建志愿表失败"));
  } finally {
    creating.value = false;
  }
}

async function loadConversations() {
  conversationsLoading.value = true;
  try {
    const data = await apiFetch("/api/agent/conversations", { headers: getAuthHeaders() });
    conversations.value = Array.isArray(data) ? data : [];
    if (!activeConversationId.value && conversations.value.length) await openConversation(conversations.value[0].id);
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "加载对话列表失败"));
  } finally {
    conversationsLoading.value = false;
  }
}

async function createConversation(title = "新的志愿对话") {
  creating.value = true;
  try {
    const detail = await apiFetch("/api/agent/conversations", {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ title })
    });
    activeConversationId.value = detail.id;
    messages.value = Array.isArray(detail.messages) ? detail.messages : [];
    await loadConversations();
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "创建对话失败"));
  } finally {
    creating.value = false;
  }
}

async function openConversation(id) {
  conversationLoading.value = true;
  try {
    const detail = await apiFetch(`/api/agent/conversations/${id}`, { headers: getAuthHeaders() });
    activeConversationId.value = detail.id;
    messages.value = Array.isArray(detail.messages) ? detail.messages : [];
    await scrollMessagesToBottom();
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "加载对话失败"));
  } finally {
    conversationLoading.value = false;
  }
}

async function scrollMessagesToBottom() {
  await nextTick();
  if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
}

async function sendMessage(content = draft.value) {
  const text = String(content || "").trim();
  if (!text || sending.value) return;
  if (!activePlanId.value) {
    ElMessage.warning("请先选择或新建当前操作的志愿表");
    return;
  }
  if (!activeConversationId.value) {
    await createConversation(text.slice(0, 12));
    if (!activeConversationId.value) return;
  }
  const optimistic = { id: `temp-${Date.now()}`, role: "user", messageType: "text", content: text, createdAt: new Date().toISOString() };
  messages.value.push(optimistic);
  draft.value = "";
  sending.value = true;
  await scrollMessagesToBottom();
  try {
    const turn = await apiFetch(`/api/agent/conversations/${activeConversationId.value}/messages`, {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ content: text, planId: Number(activePlanId.value) }),
      timeoutMs: 25000
    });
    messages.value.push(...(Array.isArray(turn.generatedMessages) ? turn.generatedMessages : []));
    if ((turn.generatedMessages || []).some((message) => ["addPlanItem", "removePlanItem", "savePlan"].includes(message.toolName))) {
      await loadPlans(activePlanId.value);
      await loadActivePlan();
    }
    await loadConversations();
    await scrollMessagesToBottom();
  } catch (ex) {
    messages.value = messages.value.filter((item) => item.id !== optimistic.id);
    ElMessage.error(resolveErrorMessage(ex, "发送消息失败"));
  } finally {
    sending.value = false;
  }
}

function recommendationCards(message) {
  return Array.isArray(message?.payload?.topItems) ? message.payload.topItems : [];
}
function hasProfilePayload(message) { return message?.toolName === "getUserProfile" && !!message?.payload; }
function hasPlanPayload(message) { return message?.toolName === "getCurrentPlan" && !!message?.payload; }
function hasErrorPayload(message) { return message?.messageType === "tool_result" && !!message?.payload?.errorCategory; }
function bubbleClass(message) {
  if (message.role === "user") return "agent-bubble agent-bubble--user";
  if (message.messageType === "tool_call") return "agent-bubble agent-bubble--toolcall";
  if (message.messageType === "tool_result") return "agent-bubble agent-bubble--toolresult";
  return "agent-bubble agent-bubble--assistant";
}
function roleLabel(message) {
  if (message.role === "user") return "你";
  if (message.messageType === "tool_call") return "工具调用";
  if (message.messageType === "tool_result") return "工具结果";
  return "智愿 AI";
}
function formatStrategyLabel(value) {
  const text = String(value || "").toUpperCase();
  if (text.includes("RUSH") || text.includes("冲")) return "冲刺";
  if (text.includes("GUARANTEE") || text.includes("保")) return "保底";
  return "稳妥";
}

function openAddDialog(item) {
  pendingAddItem.value = normalizeItem(item, item?.strategy || item?.group);
  addTargetPlanId.value = activePlanId.value || (plans.value[0] ? String(plans.value[0].id) : "new");
  addNewPlanName.value = `2026${props.user?.examProvince || ""}志愿方案`;
  addDialogVisible.value = true;
}

async function confirmAddItem() {
  if (!pendingAddItem.value || !addTargetPlanId.value) return;
  if (addTargetPlanId.value === "new" && !addNewPlanName.value.trim()) {
    ElMessage.warning("请输入志愿表名称");
    return;
  }
  addSubmitting.value = true;
  try {
    let detail = null;
    let parsed = {};
    let items = [];
    if (addTargetPlanId.value !== "new") {
      detail = await apiFetch(`/api/plans/${addTargetPlanId.value}`, { headers: getAuthHeaders() });
      ({ parsed, items } = parsePlanDetail(detail));
    }
    const key = buildPlanItemKey(pendingAddItem.value, pendingAddItem.value.strategy);
    if (items.some((item) => buildPlanItemKey(item, item.strategy) === key)) {
      ElMessage.warning("该结果已在目标志愿表中");
      return;
    }
    items.push(pendingAddItem.value);
    const result = buildPlanResult(items, parsed);
    const body = {
      planName: detail?.planName || addNewPlanName.value.trim(),
      sourceType: detail?.sourceType || "score",
      sourceQuery: detail?.sourceQuery || "AI 推荐卡片加入",
      resultJson: JSON.stringify(result),
      aiSummary: detail?.aiSummary || result.aiSummary || result.summary
    };
    const saved = await apiFetch(addTargetPlanId.value === "new" ? "/api/plans" : `/api/plans/${addTargetPlanId.value}`, {
      method: addTargetPlanId.value === "new" ? "POST" : "PUT",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify(body)
    });
    addDialogVisible.value = false;
    pendingAddItem.value = null;
    await loadPlans(saved.id);
    await loadActivePlan();
    ElMessage.success(`已加入《${saved.planName}》`);
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "加入志愿表失败"));
  } finally {
    addSubmitting.value = false;
  }
}

watch(activePlanId, loadActivePlan);
onMounted(async () => {
  await Promise.all([loadPlans(), loadConversations()]);
  if (!conversations.value.length) await createConversation();
  await loadActivePlan();
});
</script>

<template>
  <section class="agent-page">
    <div class="agent-workspace">
      <aside class="agent-rail">
        <div class="agent-rail__head">
          <strong>对话会话</strong>
          <el-button type="primary" plain size="small" :loading="creating" @click="createConversation()">新建</el-button>
        </div>
        <div class="agent-quick-title">快速提问</div>
        <div class="agent-quick-actions">
          <button v-for="prompt in quickPrompts" :key="prompt.label" type="button" @click="sendMessage(prompt.label)">
            <el-icon><component :is="prompt.icon" /></el-icon><span>{{ prompt.label }}</span>
          </button>
        </div>
        <div class="agent-conversation-title">最近对话</div>
        <div class="agent-conversation-list" v-loading="conversationsLoading">
          <button
            v-for="item in conversations"
            :key="item.id"
            type="button"
            :class="['agent-conversation-item', { 'is-active': item.id === activeConversationId }]"
            @click="openConversation(item.id)"
          >
            <strong>{{ item.title || "未命名会话" }}</strong>
            <span>{{ item.messageCount || 0 }} 条消息 · {{ formatDateTime(item.updatedAt || item.createdAt) }}</span>
          </button>
        </div>
      </aside>

      <main class="agent-chat-panel">
        <div class="agent-chat-panel__head">
          <div>
            <strong>{{ conversationTitle }}</strong>
            <div class="agent-operation-plan">
              <span>当前操作志愿表</span>
              <el-select v-model="activePlanId" placeholder="请选择志愿表" :loading="plansLoading">
                <el-option v-for="plan in plans" :key="plan.id" :label="plan.planName" :value="String(plan.id)" />
              </el-select>
              <el-tooltip content="AI 的读取、加入和移除操作均作用于这里选择的志愿表" placement="bottom">
                <el-icon><InfoFilled /></el-icon>
              </el-tooltip>
            </div>
          </div>
        </div>

        <div ref="messageListRef" class="agent-message-list" v-loading="conversationLoading">
          <article v-for="message in messages" :key="message.id" :class="['agent-message-row', `agent-message-row--${message.role}`]">
            <div :class="bubbleClass(message)">
              <div class="agent-bubble__meta"><span>{{ roleLabel(message) }}</span><time>{{ formatDateTime(message.createdAt) }}</time></div>
              <div class="agent-bubble__content">{{ message.content }}</div>
              <el-tag v-if="message.toolName" class="agent-bubble__tool" size="small" effect="plain">{{ message.toolName }}</el-tag>

              <div v-if="hasProfilePayload(message)" class="agent-payload-grid">
                <div><span>分数</span><strong>{{ message.payload.score ?? "-" }}</strong></div>
                <div><span>科类</span><strong>{{ message.payload.subjectType || "-" }}</strong></div>
                <div><span>省份</span><strong>{{ message.payload.examProvince || "-" }}</strong></div>
              </div>
              <div v-if="hasPlanPayload(message)" class="agent-inline-plan">
                <strong>{{ message.payload.planName || "当前志愿表" }}</strong>
                <span>{{ message.payload.itemCount ?? 0 }} 条志愿</span>
              </div>
              <div v-if="recommendationCards(message).length" class="agent-recommend-list">
                <article v-for="(item, index) in recommendationCards(message)" :key="`${message.id}-${index}`" class="agent-recommend-card">
                  <div class="agent-recommend-card__head">
                    <div><strong>{{ item.label || item.universityName }}</strong><span>{{ item.universityProvince || "-" }} · {{ item.majorName || "院校志愿" }}</span></div>
                    <el-tag size="small" type="success" effect="light">{{ formatStrategyLabel(item.strategyLabel || item.strategy) }}</el-tag>
                  </div>
                  <div class="agent-recommend-card__stats">
                    <span>录取概率 <strong>{{ item.admissionProbability ?? "-" }}%</strong></span>
                    <span>最低位次 <strong>{{ item.minRank ?? "-" }}</strong></span>
                    <span>位次差 <strong>{{ item.rankGap ?? "-" }}</strong></span>
                  </div>
                  <div class="agent-recommend-card__foot">
                    <span>{{ Array.isArray(item.matchReasons) ? item.matchReasons.slice(0, 2).join("；") : "" }}</span>
                    <el-button type="primary" plain size="small" @click="openAddDialog(item)">加入志愿表</el-button>
                  </div>
                </article>
              </div>
              <div v-if="hasErrorPayload(message)" class="agent-error-inline">{{ message.payload.errorMessage || message.payload.errorCode }}</div>
            </div>
          </article>
          <div v-if="!messages.length" class="agent-empty-state"><strong>开始第一轮对话</strong><span>选择右侧志愿表后，可以让 AI 推荐并协助调整方案。</span></div>
        </div>

        <div class="agent-input-panel">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="2"
            resize="none"
            maxlength="1000"
            show-word-limit
            :disabled="!activePlanId"
            placeholder="输入你的问题，AI 将结合当前志愿表回答"
            @keydown.enter.exact.prevent="sendMessage()"
          />
          <el-button class="agent-send-button" type="primary" circle :disabled="!canSend" :loading="sending" @click="sendMessage()">
            <el-icon><Promotion /></el-icon>
          </el-button>
        </div>
      </main>

      <aside class="agent-plan-panel">
        <div class="agent-plan-panel__head">
          <strong>当前志愿表</strong>
          <p>AI 推荐、加入和修改操作将写入当前选择的志愿表。</p>
        </div>
        <label class="agent-plan-selector">
          <span>当前操作志愿表</span>
          <el-select v-model="activePlanId" placeholder="请选择" :loading="plansLoading">
            <el-option v-for="plan in plans" :key="plan.id" :label="plan.planName" :value="String(plan.id)" />
          </el-select>
        </label>
        <div class="agent-plan-panel__actions">
          <el-button type="primary" plain size="small" @click="createPlanDialogVisible = true">新建志愿表</el-button>
        </div>
        <div class="agent-plan-preview-title">志愿表预览 <span>{{ activePlanItems.length }} 条</span></div>
        <div class="agent-plan-preview-list">
          <article v-for="item in activePlanItems.slice(0, 4)" :key="item.planKey" class="agent-plan-preview-card">
            <div><strong>{{ item.universityName }}</strong><el-tag size="small" effect="light">{{ formatStrategyLabel(item.strategy) }}</el-tag></div>
            <span>{{ item.majorName || "院校志愿" }}</span>
            <small>{{ item.universityProvince || "-" }} · 概率 {{ item.admissionProbability ?? "-" }}%</small>
          </article>
          <el-empty v-if="!activePlanItems.length" description="暂无志愿" :image-size="70" />
        </div>
        <el-button class="agent-plan-panel__jump" plain @click="emit('jump-to-plans')">查看完整志愿表</el-button>
      </aside>
    </div>

    <el-dialog v-model="addDialogVisible" title="加入志愿表" width="460px" destroy-on-close>
      <p class="agent-dialog-tip">请选择目标志愿表，再确认写入。</p>
      <el-select v-model="addTargetPlanId" class="agent-dialog-select" placeholder="请选择志愿表">
        <el-option v-for="plan in plans" :key="plan.id" :label="plan.planName" :value="String(plan.id)" />
        <el-option label="新建志愿表" value="new" />
      </el-select>
      <el-input v-if="addTargetPlanId === 'new'" v-model.trim="addNewPlanName" maxlength="30" placeholder="输入志愿表名称" />
      <div v-if="pendingAddItem" class="agent-dialog-target">
        <strong>{{ pendingAddItem.universityName }}</strong><span>{{ pendingAddItem.majorName || "院校志愿" }}</span>
      </div>
      <template #footer><el-button @click="addDialogVisible = false">取消</el-button><el-button type="primary" :loading="addSubmitting" @click="confirmAddItem">确认加入</el-button></template>
    </el-dialog>

    <el-dialog v-model="createPlanDialogVisible" title="新建志愿表" width="420px" destroy-on-close>
      <el-input v-model.trim="createPlanName" maxlength="30" placeholder="例如：2026浙江志愿方案-A" />
      <template #footer><el-button @click="createPlanDialogVisible = false">取消</el-button><el-button type="primary" :loading="creating" @click="createEmptyPlan">创建</el-button></template>
    </el-dialog>
  </section>
</template>
