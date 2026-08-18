<script setup>
import { ElMessage } from "element-plus";
import { Document, InfoFilled, Monitor, Promotion, School, User } from "@element-plus/icons-vue";
import { computed, nextTick, onMounted, ref } from "vue";
import {
  buildPlanResult,
  flattenPlanItems,
  formatDateTime,
  mergePlanItems,
  normalizeItem,
  parsePlanResult
} from "../utils/recommendation";
import { UI_TEXT, createHttpError, normalizeUserError } from "../utils/ui";

const props = defineProps({
  token: { type: String, required: true },
  user: { type: Object, default: () => ({}) }
});

defineEmits(["jump-to-plans"]);
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
const addDialogVisible = ref(false);
const addSubmitting = ref(false);
const addTargetPlanId = ref("");
const addNewPlanName = ref("");
const pendingAddItem = ref(null);
let conversationLoadVersion = 0;

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
  const parsed = parsePlanResult(detail?.resultJson);
  return { parsed, items: flattenPlanItems(parsed) };
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
    conversationLoadVersion += 1;
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
  const loadVersion = ++conversationLoadVersion;
  activeConversationId.value = id;
  conversationLoading.value = true;
  try {
    const detail = await apiFetch(`/api/agent/conversations/${id}`, { headers: getAuthHeaders() });
    if (loadVersion !== conversationLoadVersion || String(activeConversationId.value) !== String(id)) return;
    activeConversationId.value = detail.id;
    messages.value = Array.isArray(detail.messages) ? detail.messages : [];
    await scrollMessagesToBottom();
  } catch (ex) {
    if (loadVersion !== conversationLoadVersion || String(activeConversationId.value) !== String(id)) return;
    ElMessage.error(resolveErrorMessage(ex, "加载对话失败"));
  } finally {
    if (loadVersion === conversationLoadVersion) conversationLoading.value = false;
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
    const { items: mergedItems, addedCount } = mergePlanItems(items, [pendingAddItem.value]);
    if (!addedCount) {
      ElMessage.warning("该结果已在目标志愿表中");
      return;
    }
    const result = buildPlanResult(mergedItems, {
      ...parsed,
      summary: `当前方案共选择 ${mergedItems.length} 条志愿结果。`
    });
    const body = {
      planName: detail?.planName || addNewPlanName.value.trim(),
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
    ElMessage.success(`已加入《${saved.planName}》`);
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "加入志愿表失败"));
  } finally {
    addSubmitting.value = false;
  }
}

onMounted(async () => {
  await Promise.all([loadPlans(), loadConversations()]);
  if (!conversations.value.length) await createConversation();
});
</script>

<template>
  <section class="agent-page">
    <div class="agent-workspace">
      <aside class="agent-rail">
        <div class="agent-rail__head">
          <strong>AI 志愿助手</strong>
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
          <div v-if="!messages.length" class="agent-empty-state"><strong>开始第一轮对话</strong><span>先选择当前志愿单，再让 AI 推荐、解释或协助调整。</span></div>
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

  </section>
</template>
