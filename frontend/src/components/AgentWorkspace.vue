<script setup>
import { ElMessage } from "element-plus";
import { ArrowRight, CaretBottom, CaretTop, Collection, CopyDocument, Document, Expand, Fold, Monitor, Plus, Promotion, Refresh, RefreshRight, School, Search, SwitchButton, TrendCharts, User, VideoPause } from "@element-plus/icons-vue";
import { computed, inject, nextTick, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  buildGroupedFromResult,
  buildPlanItemKey,
  formatDateTime,
  normalizeItem
} from "../utils/recommendation";
import { sheetToPlanItems } from "../utils/planSync";
import { UI_TEXT, createHttpError, normalizeUserError } from "../utils/ui";
import { readCurrentSheet } from "../utils/volunteerCore";
import XiaoZhiAvatar from "./XiaoZhiAvatar.vue";

const props = defineProps({
  token: { type: String, required: true },
  user: { type: Object, default: () => ({}) }
});

const emit = defineEmits(["jump-to-plans"]);

const route = useRoute();
const router = useRouter();
const workspace = inject("workspace", null);
const sideCollapsed = ref(false);

const conversationsLoading = ref(false);
const conversationLoading = ref(false);
const sending = ref(false);
const creating = ref(false);
const conversations = ref([]);
const activeConversationId = ref(null);
const messages = ref([]);
const draft = ref("");
const messageListRef = ref(null);
const planDrawerVisible = ref(false);

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

const QUICK_PROMPTS = [
  { label: "帮我看看我的画像信息", icon: User },
  { label: "看看我当前的志愿方案", icon: Document },
  { label: "帮我推荐学校", icon: School },
  { label: "帮我推荐计算机专业", icon: Monitor }
];

const NAV_PILLS = [
  { label: "查大学", icon: School, to: "/schools" },
  { label: "查专业", icon: Monitor, to: "/majors" },
  { label: "院校排行", icon: TrendCharts, to: "/rank" },
  { label: "一分一段", icon: Search, to: "/segments" },
  { label: "招生计划", icon: Document, to: "/enroll" }
];

const TOOL_CARDS = [
  { label: "智能推荐", desc: "冲稳保方案", ask: "帮我基于分数、位次和选科，生成冲稳保梯度志愿方案", icon: Promotion, theme: "orange" },
  { label: "智能选大学", desc: "按分匹配", to: "/choose", icon: Search, theme: "blue" },
  { label: "志愿填报", desc: "大数据看板", to: "/volunteer", icon: TrendCharts, theme: "purple" },
  { label: "我的志愿表", desc: "方案管理", to: "/plans", icon: Collection, theme: "green" },
  { label: "历史记录", desc: "对话回顾", to: "/history", icon: Refresh, theme: "cyan" }
];

const QUESTION_POOL = [
  "广东丨物化生丨550分能上哪些大学？",
  "帮我看看我的画像信息",
  "看看我当前的志愿方案",
  "帮我推荐计算机专业",
  "帮我分析当前方案的冲稳保比例",
  "湖南物理类600分适合冲哪些学校？",
  "推荐几所省内就业前景好的大学",
  "我的位次能报什么层次的学校？"
];
const quickQuestions = ref(QUESTION_POOL.slice(0, 4));

const TOOL_LABELS = {
  getUserProfile: "已读取用户画像",
  getCurrentPlan: "已读取当前志愿表",
  getMajorOverview: "已读取专业资料",
  recommendSchools: "已生成院校推荐",
  recommendMajors: "已生成专业推荐",
  addPlanItem: "已将志愿加入志愿表",
  removePlanItem: "已从志愿表移除",
  savePlan: "已保存志愿表"
};

const conversationTitle = computed(() =>
  conversations.value.find((item) => item.id === activeConversationId.value)?.title || "新的志愿对话"
);
const activePlan = computed(() => plans.value.find((item) => String(item.id) === String(activePlanId.value)) || null);
const canSend = computed(() => !!draft.value.trim() && !sending.value);
const planContextText = computed(() => {
  if (activePlan.value) return `正在参考《${activePlan.value.planName}》（${activePlanItems.value.length} 条志愿）`;
  return "可直接提问；需要读取或修改志愿表时，再选择或新建方案";
});
const userAvatarText = computed(() => String(props.user?.username || props.user?.phone || "用").slice(0, 1));

const groups = computed(() => {
  const result = [];
  let current = null;
  for (const message of messages.value) {
    if (message.role === "user") {
      current = { user: message, assistant: [], tools: [] };
      result.push(current);
    } else if (current) {
      if (["tool_call", "tool_result"].includes(message.messageType)) current.tools.push(message);
      else current.assistant.push(message);
    } else {
      current = { user: null, assistant: [message], tools: [] };
      result.push(current);
    }
  }
  return result;
});

function escapeHtml(text) {
  return String(text)
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;");
}

function renderMarkdown(text) {
  if (!text) return "";
  // 预处理 markdown 表格：| header | + |---| 分隔 + | data |，转成 <table> 占位符
  const tableHtmls = [];
  text = text.replace(/((?:\|[^\n]*\|\r?\n)+)(\|[\s:|-]+\|\r?\n)((?:\|[^\n]*\|\r?\n?)*)/g, (match) => {
    const lines = match.trim().split(/\r?\n/);
    if (lines.length < 2) return match;
    const headers = lines[0].split("|").slice(1, -1).map((s) => s.trim());
    const bodyLines = lines.slice(2);
    let t = '<table class="gk-md-table"><thead><tr>';
    headers.forEach((h) => {
      t += "<th>" + escapeHtml(h) + "</th>";
    });
    t += "</tr></thead><tbody>";
    bodyLines.forEach((line) => {
      const cells = line.split("|").slice(1, -1).map((c) => c.trim());
      t += "<tr>" + cells.map((c) => "<td>" + escapeHtml(c) + "</td>").join("") + "</tr>";
    });
    t += "</tbody></table>";
    const ph = "__TABLE_" + tableHtmls.length + "__";
    tableHtmls.push(t);
    return ph;
  });
  const lines = escapeHtml(text).split(/\r?\n/);
  let html = "";
  let listTag = "";
  const closeList = () => {
    if (listTag) {
      html += `</${listTag}>`;
      listTag = "";
    }
  };
  lines.forEach((line) => {
    const trimmed = line.trim();
    if (/^__TABLE_\d+__$/.test(trimmed)) {
      closeList();
      html += trimmed;
    } else if (/^#{1,3}\s+/.test(trimmed)) {
      closeList();
      html += `<div class="gk-md-h">${trimmed.replace(/^#{1,3}\s+/, "")}</div>`;
    } else if (/^[-•*]\s+/.test(trimmed)) {
      if (listTag !== "ul") {
        closeList();
        html += "<ul>";
        listTag = "ul";
      }
      html += `<li>${trimmed.replace(/^[-•*]\s+/, "")}</li>`;
    } else if (/^\d+[.、)]\s+/.test(trimmed)) {
      if (listTag !== "ol") {
        closeList();
        html += "<ol>";
        listTag = "ol";
      }
      html += `<li>${trimmed.replace(/^\d+[.、)]\s+/, "")}</li>`;
    } else if (!trimmed) {
      closeList();
    } else {
      closeList();
      html += `<p>${line}</p>`;
    }
  });
  closeList();
  tableHtmls.forEach((t, i) => {
    html = html.replace("__TABLE_" + i + "__", t);
  });
  return html
    .replace(/\*\*([^*]+)\*\*/g, "<strong>$1</strong>")
    .replace(/\*([^*]+)\*/g, "<em>$1</em>")
    .replace(/`([^`]+)`/g, "<code>$1</code>");
}

function refreshQuestions() {
  const current = quickQuestions.value.map((question) => QUESTION_POOL.indexOf(question));
  quickQuestions.value = current.map((index) => QUESTION_POOL[(index + 1) % QUESTION_POOL.length]);
}

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
      explanation: normalized.explanation || null,
      volunteerIndex: item?.volunteerIndex ?? normalized.volunteerIndex ?? null,
      adjust: item?.adjust !== false
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
    const fallback = plans.value.find((item) => item.planName === "当前方案草稿") || plans.value[0];
    activePlanId.value = matched ? String(matched.id) : fallback ? String(fallback.id) : "";
    return true;
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "加载志愿表失败"));
    return false;
  } finally {
    plansLoading.value = false;
  }
}

/**
 * 模拟填报器使用 localStorage 保存 45 个志愿位，AI 使用云端志愿表。
 * 当本地已有志愿、云端还没有“当前方案草稿”时自动建立草稿，
 * 避免用户已有本地志愿表，却因为只有旧的已保存方案而无法让 AI 识别当前内容。
 */
async function syncLocalSheetWhenNoCloudDraft() {
  if (plans.value.some((item) => item.planName === "当前方案草稿")) return false;
  const items = sheetToPlanItems(readCurrentSheet() || []);
  if (!items.length) return false;
  try {
    const result = buildPlanResult(items, {});
    const saved = await apiFetch("/api/plans/current", {
      method: "PUT",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        planName: "当前方案草稿",
        sourceType: "score",
        sourceQuery: `本地填报器自动同步 ${items.length} 条志愿结果`,
        resultJson: JSON.stringify(result),
        aiSummary: result.summary
      })
    });
    await loadPlans(saved?.id);
    return true;
  } catch (ex) {
    console.warn("[syncLocalSheetWhenNoCloudDraft]", ex);
    return false;
  }
}

function requestPlanId() {
  if (!activePlanId.value) return null;
  const value = Number(activePlanId.value);
  return Number.isSafeInteger(value) && value > 0 ? value : null;
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
    draft.value = "";
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

/**
 * An SSE connection can fail after the backend has persisted its final messages. Replace all
 * optimistic and live messages with the server record so a partial stream never overlaps the
 * final persisted answer.
 */
async function reconcileCurrentConversation() {
  if (!activeConversationId.value) return false;
  try {
    const detail = await apiFetch(`/api/agent/conversations/${activeConversationId.value}`, {
      headers: getAuthHeaders(),
      timeoutMs: 5000
    });
    messages.value = Array.isArray(detail.messages) ? detail.messages : [];
    return true;
  } catch {
    return false;
  }
}

async function scrollMessagesToBottom() {
  await nextTick();
  if (messageListRef.value) messageListRef.value.scrollTop = messageListRef.value.scrollHeight;
}

const abortRef = ref(null);
const stopRequested = ref(false);
const streamingStarted = ref(false);
const expandedAnswerIds = ref(new Set());
const answerFeedback = ref({});
let liveMessage = null;

function sleep(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

function ensureLiveMessage() {
  if (!liveMessage) {
    messages.value.push({
      id: `stream-${Date.now()}`,
      role: "assistant",
      messageType: "text",
      content: "",
      createdAt: new Date().toISOString(),
      streaming: true
    });
    liveMessage = messages.value[messages.value.length - 1];
  }
  return liveMessage;
}

function finalizeLiveMessage(content) {
  if (!liveMessage) return;
  if (content != null) liveMessage.content = content;
  liveMessage.streaming = false;
  liveMessage = null;
}

function stopGeneration() {
  stopRequested.value = true;
  if (abortRef.value) abortRef.value.abort();
}

async function sendMessage(content = draft.value) {
  const text = String(content || "").trim();
  if (!text || sending.value) return;
  if (!activeConversationId.value) {
    await createConversation(text.slice(0, 12));
    if (!activeConversationId.value) return;
  }
  const turnStartIndex = messages.value.length;
  const optimistic = { id: `temp-${Date.now()}`, role: "user", messageType: "text", content: text, createdAt: new Date().toISOString() };
  messages.value.push(optimistic);
  draft.value = "";
  sending.value = true;
  stopRequested.value = false;
  streamingStarted.value = false;
  liveMessage = null;
  await scrollMessagesToBottom();
  let usedWriteTool = false;
  try {
    const streamResult = await runStreamTurn(text);
    if (streamResult.streamed) {
      usedWriteTool = streamResult.usedWriteTool;
    } else if (streamResult.allowLegacy) {
      usedWriteTool = await runLegacyTurn(text);
    }
  } catch (ex) {
    if (ex?.name === "AbortError" || stopRequested.value) {
      finalizeLiveMessage();
    } else {
      const reconciled = await reconcileCurrentConversation();
      if (!reconciled) {
        messages.value.splice(turnStartIndex);
      }
      liveMessage = null;
      streamingStarted.value = false;
      if (reconciled) {
        ElMessage.warning("流式连接已中断，已从服务器同步本次对话记录");
      } else {
        ElMessage.error(resolveErrorMessage(ex, "发送消息失败"));
      }
    }
  } finally {
    finalizeLiveMessage();
    sending.value = false;
    abortRef.value = null;
    if (usedWriteTool) {
      await loadPlans(activePlanId.value);
      await loadActivePlan();
    }
    await loadConversations();
    await scrollMessagesToBottom();
  }
}

async function runStreamTurn(text) {
  const controller = new AbortController();
  abortRef.value = controller;
  let response;
  try {
    response = await fetch(`/api/agent/conversations/${activeConversationId.value}/messages/stream`, {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json", Accept: "text/event-stream" }),
      body: JSON.stringify({ content: text, planId: requestPlanId() }),
      signal: controller.signal
    });
  } catch (ex) {
    if (ex?.name === "AbortError") throw ex;
    const connectionError = new Error(
      "AI 对话服务连接失败：请确认后端服务已启动，并检查前端的 VITE_API_PROXY_TARGET 是否指向实际后端地址。为避免重复调用工具，本次消息未自动重试。"
    );
    throw connectionError;
  }
  if (!response.ok) {
    // The legacy endpoint is only a compatibility fallback for backends that genuinely
    // do not implement streaming. Retrying after a network failure can create a second
    // tool call even when the original POST has already been persisted by the server.
    if (response.status === 404 || response.status === 405) {
      return { streamed: false, allowLegacy: true, usedWriteTool: false };
    }
    const isJson = response.headers.get("content-type")?.includes("application/json");
    const data = isJson ? await response.json() : null;
    throw createHttpError(response, data, UI_TEXT.common.requestFailed);
  }
  const contentType = response.headers.get("content-type") || "";
  if (!contentType.includes("text/event-stream")) {
    return { streamed: false, allowLegacy: true, usedWriteTool: false };
  }

  let usedWriteTool = false;
  const WRITE_TOOLS = ["addPlanItem", "removePlanItem", "savePlan"];
  const pushEventMessage = (message) => {
    messages.value.push({
      id: `evt-${Date.now()}-${messages.value.length}`,
      createdAt: new Date().toISOString(),
      ...message
    });
    if (WRITE_TOOLS.includes(message.toolName)) usedWriteTool = true;
  };

  const handleEvent = async (eventName, dataText) => {
    let data = {};
    try {
      data = dataText ? JSON.parse(dataText) : {};
    } catch {
      data = {};
    }
    if (eventName === "tool_call") {
      pushEventMessage({ role: "assistant", messageType: "tool_call", toolName: data.toolName, content: data.content || "" });
      streamingStarted.value = true;
      await scrollMessagesToBottom();
    } else if (eventName === "tool_result") {
      pushEventMessage({ role: "assistant", messageType: "tool_result", toolName: data.toolName, content: data.content || "", payload: data.payload });
      streamingStarted.value = true;
      await scrollMessagesToBottom();
    } else if (eventName === "delta") {
      const target = ensureLiveMessage();
      target.content += data.text || "";
      streamingStarted.value = true;
      await scrollMessagesToBottom();
    } else if (eventName === "message") {
      const message = data.message;
      if (!message || message.role === "user") return;
      if (message.messageType === "text") {
        const target = ensureLiveMessage();
        target.content = message.content || target.content;
        if (WRITE_TOOLS.includes(message.toolName)) usedWriteTool = true;
      } else {
        pushEventMessage(message);
      }
      await scrollMessagesToBottom();
    } else if (eventName === "error") {
      ElMessage.warning(data.message || "AI 服务波动，已切换备用回答");
    }
  };

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = "";
  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    let separatorMatch;
    while ((separatorMatch = /\r?\n\r?\n/.exec(buffer)) !== null) {
      const separatorIndex = separatorMatch.index;
      const block = buffer.slice(0, separatorIndex);
      buffer = buffer.slice(separatorIndex + separatorMatch[0].length);
      let eventName = "message";
      let dataText = "";
      block.split(/\r?\n/).forEach((line) => {
        if (line.startsWith("event:")) eventName = line.slice(6).trim();
        else if (line.startsWith("data:")) dataText += line.slice(5).trim();
      });
      await handleEvent(eventName, dataText);
    }
  }
  return { streamed: true, usedWriteTool };
}

function revealTextProgressively(fullText) {
  return new Promise((resolve) => {
    const target = ensureLiveMessage();
    const step = () => {
      if (stopRequested.value) {
        target.content = fullText;
        resolve();
        return;
      }
      const remaining = fullText.length - target.content.length;
      if (remaining <= 0) {
        resolve();
        return;
      }
      const size = remaining > 400 ? 6 + Math.floor(Math.random() * 5) : 2 + Math.floor(Math.random() * 4);
      target.content = fullText.slice(0, target.content.length + size);
      scrollMessagesToBottom();
      setTimeout(step, remaining > 400 ? 14 : 22);
    };
    step();
  });
}

async function runLegacyTurn(text) {
  const turn = await apiFetch(`/api/agent/conversations/${activeConversationId.value}/messages`, {
    method: "POST",
    headers: getAuthHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({ content: text, planId: requestPlanId() }),
    timeoutMs: 25000
  });
  const generated = Array.isArray(turn.generatedMessages) ? turn.generatedMessages : [];
  let usedWriteTool = false;
  for (const message of generated) {
    if (["addPlanItem", "removePlanItem", "savePlan"].includes(message.toolName)) usedWriteTool = true;
    if (stopRequested.value) {
      messages.value.push(message);
      continue;
    }
    if (["tool_call", "tool_result"].includes(message.messageType)) {
      await sleep(420);
      messages.value.push(message);
      streamingStarted.value = true;
      await scrollMessagesToBottom();
    } else {
      await sleep(280);
      streamingStarted.value = true;
      await revealTextProgressively(message.content || "");
    }
  }
  finalizeLiveMessage();
  return usedWriteTool;
}

function recommendationCards(message) {
  return Array.isArray(message?.payload?.topItems) ? message.payload.topItems : [];
}
function hasProfilePayload(message) { return message?.toolName === "getUserProfile" && !!message?.payload; }
function hasPlanPayload(message) { return message?.toolName === "getCurrentPlan" && !!message?.payload; }
function hasErrorPayload(message) { return message?.messageType === "tool_result" && !!message?.payload?.errorCategory; }

function toolChipText(tool) {
  if (tool.messageType === "tool_call") return `正在调用工具 · ${tool.toolName || "agent"}`;
  if (hasErrorPayload(tool)) return `${TOOL_LABELS[tool.toolName] || tool.toolName || "工具"} 执行失败`;
  return TOOL_LABELS[tool.toolName] || tool.toolName || "工具已执行";
}

function formatStrategyLabel(value) {
  const text = String(value || "").toUpperCase();
  if (text.includes("RUSH") || text.includes("冲")) return "冲刺";
  if (text.includes("GUARANTEE") || text.includes("保")) return "保底";
  return "稳妥";
}

function strategyTagType(value) {
  const label = formatStrategyLabel(value);
  if (label === "冲刺") return "warning";
  if (label === "保底") return "info";
  return "success";
}

function answerHtml(message) {
  const html = renderMarkdown(message.content || "");
  return message.streaming ? `${html}<span class="gk-cursor"></span>` : html;
}

function isLongAnswer(message) {
  return (message.content || "").length > 420;
}

function isCollapsedAnswer(message) {
  return isLongAnswer(message) && !expandedAnswerIds.value.has(message.id);
}

function expandAnswer(message) {
  const next = new Set(expandedAnswerIds.value);
  if (next.has(message.id)) next.delete(message.id);
  else next.add(message.id);
  expandedAnswerIds.value = next;
}

async function copyAnswer(message) {
  try {
    await navigator.clipboard.writeText(message.content || "");
    ElMessage.success("已复制回答内容");
  } catch {
    ElMessage.warning("复制失败，请手动选择文本复制");
  }
}

function regenerateAnswer(message) {
  const index = messages.value.findIndex((item) => item.id === message.id);
  for (let i = index - 1; i >= 0; i -= 1) {
    if (messages.value[i].role === "user") {
      sendMessage(messages.value[i].content);
      return;
    }
  }
}

function setAnswerFeedback(message, value) {
  answerFeedback.value = {
    ...answerFeedback.value,
    [message.id]: answerFeedback.value[message.id] === value ? "" : value
  };
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
watch(() => messages.value.length, () => scrollMessagesToBottom());
onMounted(async () => {
  const [plansLoaded] = await Promise.all([loadPlans(), loadConversations()]);
  if (plansLoaded) await syncLocalSheetWhenNoCloudDraft();
  if (!conversations.value.length) await createConversation();
  await loadActivePlan();
  const presetQuestion = Array.isArray(route.query.q) ? route.query.q[0] : route.query.q;
  if (typeof presetQuestion === "string" && presetQuestion.trim() && !draft.value.trim()) {
    draft.value = presetQuestion.trim();
  }
});
</script>

<template>
  <section class="xz-chat">
    <aside class="xz-side" :class="{ 'is-collapsed': sideCollapsed }">
      <div class="xz-side__head">
        <div class="xz-side__logo">
          <XiaoZhiAvatar size="md" />
          <span v-if="!sideCollapsed" class="xz-side__name">智愿AI报考平台</span>
        </div>
        <button v-if="!sideCollapsed" type="button" class="xz-side__fold" title="收起侧栏" @click="sideCollapsed = true">
          <el-icon><Fold /></el-icon>
        </button>
      </div>

      <button
        type="button"
        class="xz-side__new"
        :class="{ 'is-mini': sideCollapsed }"
        :disabled="creating"
        :title="sideCollapsed ? '新对话' : ''"
        @click="createConversation()"
      >
        <el-icon><Plus /></el-icon>
        <span v-if="!sideCollapsed">新对话</span>
      </button>

      <nav class="xz-side__quick" aria-label="快速提问">
        <div v-if="!sideCollapsed" class="xz-side__label">快速提问</div>
        <button
          v-for="prompt in QUICK_PROMPTS"
          :key="prompt.label"
          type="button"
          class="xz-side__item"
          :title="sideCollapsed ? prompt.label : ''"
          @click="sendMessage(prompt.label)"
        >
          <el-icon><component :is="prompt.icon" /></el-icon>
          <span v-if="!sideCollapsed">{{ prompt.label }}</span>
        </button>
      </nav>

      <div v-if="!sideCollapsed" class="xz-side__history">
        <div class="xz-side__label">历史对话</div>
        <div class="xz-side__history-list" v-loading="conversationsLoading">
          <button
            v-for="item in conversations"
            :key="item.id"
            type="button"
            :class="['xz-side__hist-item', { 'is-active': item.id === activeConversationId }]"
            @click="openConversation(item.id)"
          >
            <span class="xz-side__hist-title">{{ item.title || "未命名会话" }}</span>
            <span class="xz-side__hist-meta">{{ item.messageCount || 0 }} 条 · {{ formatDateTime(item.updatedAt || item.createdAt) }}</span>
          </button>
          <p v-if="!conversations.length && !conversationsLoading" class="xz-side__hist-empty">暂无历史对话</p>
        </div>
      </div>

      <div class="xz-side__user">
        <span class="xz-side__user-avatar">{{ userAvatarText }}</span>
        <template v-if="!sideCollapsed">
          <div class="xz-side__user-info">
            <strong>{{ props.user?.username || "志愿考生" }}</strong>
            <span>AI 对话工作区</span>
          </div>
          <button type="button" class="xz-side__logout" @click="workspace?.logout?.()">
            <el-icon><SwitchButton /></el-icon>退出
          </button>
        </template>
      </div>

      <button v-if="sideCollapsed" type="button" class="xz-side__unfold" title="展开侧栏" @click="sideCollapsed = false">
        <el-icon><Expand /></el-icon>
      </button>
    </aside>

    <main class="xz-main">
      <div v-if="!messages.length" class="xz-hero">
        <div class="xz-hero__inner">
          <div class="xz-hero__logo">
            <XiaoZhiAvatar size="hero" float />
            <div class="xz-hero__title">
              <em>Hello</em>
              <strong>我是你的<i>志愿AI咨询师</i>～</strong>
              <span>✨ 高考志愿，就问小智</span>
            </div>
          </div>

          <div class="xz-plan-context" :class="{ 'is-unbound': !activePlan }">
            <span class="xz-plan-context__dot" aria-hidden="true"></span>
            <span>{{ planContextText }}</span>
            <button type="button" @click="planDrawerVisible = true">
              {{ activePlan ? "查看志愿表" : "选择或新建" }}
            </button>
          </div>

          <div class="xz-ask">
            <textarea
              v-model="draft"
              rows="3"
              class="xz-ask__area"
              maxlength="1000"
              placeholder="请输入您想咨询的问题，Shift+Enter换行"
              @keydown.enter.exact.prevent="sendMessage()"
            ></textarea>
            <div class="xz-ask__row">
              <button
                v-for="pill in NAV_PILLS"
                :key="pill.label"
                type="button"
                class="xz-ask__pill"
                @click="router.push(pill.to)"
              >
                <el-icon><component :is="pill.icon" /></el-icon>{{ pill.label }}
              </button>
              <button
                class="xz-ask__send"
                :class="{ 'is-stop': sending }"
                type="button"
                :disabled="!sending && !canSend"
                :title="sending ? '停止生成' : '发送'"
                @click="sending ? stopGeneration() : sendMessage()"
              >
                <el-icon><VideoPause v-if="sending" /><Promotion v-else /></el-icon>
              </button>
            </div>
          </div>

          <div class="xz-panel xz-qs">
            <div class="xz-panel__head">
              <span class="xz-panel__title">你可以这样问：<i>HAVE A TRY</i></span>
              <button type="button" class="xz-panel__more" @click="refreshQuestions">
                <el-icon><Refresh /></el-icon>换一批
              </button>
            </div>
            <ul class="xz-qs__list">
              <li v-for="question in quickQuestions" :key="question" @click="sendMessage(question)">{{ question }}</li>
            </ul>
          </div>

          <div class="xz-panel xz-tools">
            <div class="xz-panel__head">
              <span class="xz-panel__title">快捷工具</span>
              <button type="button" class="xz-panel__more" @click="router.push('/')">
                更多<el-icon><ArrowRight /></el-icon>
              </button>
            </div>
            <div class="xz-tools__list">
              <button
                v-for="tool in TOOL_CARDS"
                :key="tool.label"
                type="button"
                class="xz-tool"
                :class="`xz-tool--${tool.theme}`"
                @click="tool.ask ? sendMessage(tool.ask) : router.push(tool.to)"
              >
                <span class="xz-tool__cover"><el-icon><component :is="tool.icon" /></el-icon></span>
                <span class="xz-tool__name">{{ tool.label }}</span>
                <span class="xz-tool__desc">{{ tool.desc }}</span>
              </button>
            </div>
          </div>

          <p class="xz-tip">内容由AI生成，仅供参考使用，不作为决策建议～</p>
        </div>
      </div>

      <template v-else>
        <header class="xz-topbar">
          <div class="xz-topbar__title">
            <XiaoZhiAvatar size="sm" />
            <strong>{{ conversationTitle }}</strong>
            <span class="xz-topbar__plan">{{ activePlan?.planName || "未绑定方案 · 可直接咨询" }}</span>
          </div>
          <div class="xz-topbar__actions">
            <el-select v-model="activePlanId" class="xz-plan-select" placeholder="选择志愿表" size="small" :loading="plansLoading">
              <el-option v-for="plan in plans" :key="plan.id" :label="plan.planName" :value="String(plan.id)" />
            </el-select>
            <button type="button" class="xz-topbar__plans" @click="planDrawerVisible = true">
              <el-icon><Collection /></el-icon>志愿表<span class="xz-topbar__badge">{{ activePlanItems.length }}</span>
            </button>
          </div>
        </header>

        <div ref="messageListRef" class="xz-stream" v-loading="conversationLoading">
          <div class="xz-stream__inner">
            <template v-for="(group, gi) in groups" :key="`g-${gi}`">
              <div class="xz-msg">
                <div v-if="group.user" class="xz-msg__q">{{ group.user.content }}</div>
                <div v-if="group.tools.length || group.assistant.length || (sending && gi === groups.length - 1)" class="xz-msg__a">
                  <div class="xz-msg__card">
                    <div v-if="group.tools.length" class="gk-tools">
                      <span
                        v-for="(tool, ti) in group.tools"
                        :key="`t-${gi}-${ti}`"
                        :class="[
                          'gk-tool-chip',
                          { 'gk-tool-chip--call': tool.messageType === 'tool_call', 'gk-tool-chip--error': hasErrorPayload(tool) }
                        ]"
                      >{{ toolChipText(tool) }}</span>
                    </div>
                    <div v-for="(tool, ti) in group.tools" :key="`p-${gi}-${ti}`">
                      <div v-if="hasProfilePayload(tool)" class="gk-payload gk-payload--profile">
                        <span>分数<strong>{{ tool.payload.score ?? "-" }}</strong></span>
                        <span>科类<strong>{{ tool.payload.subjectType || "-" }}</strong></span>
                        <span>省份<strong>{{ tool.payload.examProvince || "-" }}</strong></span>
                      </div>
                      <div v-if="hasPlanPayload(tool)" class="gk-payload gk-payload--plan">
                        <strong>{{ tool.payload.planName || "当前志愿表" }}</strong>
                        <span>共 {{ tool.payload.itemCount ?? 0 }} 条志愿</span>
                      </div>
                      <div v-if="recommendationCards(tool).length" class="gk-cards">
                        <article v-for="(item, idx) in recommendationCards(tool)" :key="`c-${gi}-${ti}-${idx}`" class="gk-card">
                          <div class="gk-card__head">
                            <div>
                              <strong>{{ item.label || item.universityName }}</strong>
                              <span>{{ item.universityProvince || "-" }} · {{ item.majorName || "院校志愿" }}</span>
                            </div>
                            <el-tag size="small" :type="strategyTagType(item.strategyLabel || item.strategy)" effect="light">
                              {{ formatStrategyLabel(item.strategyLabel || item.strategy) }}
                            </el-tag>
                          </div>
                          <div class="gk-card__stats">
                            <span>录取概率 <strong>{{ item.admissionProbability == null ? "待测" : item.admissionProbability + "%" }}</strong></span>
                            <span>最低位次 <strong>{{ item.minRank ?? "-" }}</strong></span>
                            <span>位次差 <strong>{{ item.rankGap ?? "-" }}</strong></span>
                          </div>
                          <div class="gk-card__foot">
                            <span>{{ Array.isArray(item.matchReasons) ? item.matchReasons.slice(0, 2).join("；") : "" }}</span>
                            <button type="button" class="gk-card__add" @click="openAddDialog(item)">加入志愿表</button>
                          </div>
                        </article>
                      </div>
                      <div v-if="hasErrorPayload(tool)" class="gk-error">{{ tool.payload.errorMessage || tool.payload.errorCode }}</div>
                    </div>
                    <div v-for="(msg, mi) in group.assistant" :key="`m-${gi}-${mi}`" class="xz-answer">
                      <div :class="['gk-answer__body', { 'is-collapsed': isCollapsedAnswer(msg) }]">
                        <div v-if="msg.content" class="gk-answer__content" v-html="answerHtml(msg)"></div>
                      </div>
                      <button v-if="isLongAnswer(msg) && !msg.streaming" type="button" class="xz-answer__more" @click="expandAnswer(msg)">
                        {{ isCollapsedAnswer(msg) ? "查看更多" : "收起" }}
                      </button>
                      <div v-if="!msg.streaming && msg.content" class="xz-actions">
                        <button type="button" title="复制" @click="copyAnswer(msg)"><el-icon><CopyDocument /></el-icon></button>
                        <button type="button" title="重新生成" @click="regenerateAnswer(msg)"><el-icon><RefreshRight /></el-icon></button>
                        <button type="button" title="赞同" :class="{ 'is-active': answerFeedback[msg.id] === 'up' }" @click="setAnswerFeedback(msg, 'up')"><el-icon><CaretTop /></el-icon></button>
                        <button type="button" title="反对" :class="{ 'is-active': answerFeedback[msg.id] === 'down' }" @click="setAnswerFeedback(msg, 'down')"><el-icon><CaretBottom /></el-icon></button>
                      </div>
                    </div>
                    <div v-if="sending && gi === groups.length - 1 && !streamingStarted" class="gk-typing"><i></i><i></i><i></i></div>
                  </div>
                </div>
              </div>
            </template>
          </div>
        </div>

        <div class="xz-askbar">
          <div class="xz-ask">
            <textarea
              v-model="draft"
              rows="2"
              class="xz-ask__area"
              maxlength="1000"
              placeholder="请输入您想咨询的问题，Shift+Enter换行"
              @keydown.enter.exact.prevent="sendMessage()"
            ></textarea>
            <div class="xz-ask__row">
              <button
                v-for="pill in NAV_PILLS"
                :key="pill.label"
                type="button"
                class="xz-ask__pill"
                @click="router.push(pill.to)"
              >
                <el-icon><component :is="pill.icon" /></el-icon>{{ pill.label }}
              </button>
              <button
                class="xz-ask__send"
                :class="{ 'is-stop': sending }"
                type="button"
                :disabled="!sending && !canSend"
                :title="sending ? '停止生成' : '发送'"
                @click="sending ? stopGeneration() : sendMessage()"
              >
                <el-icon><VideoPause v-if="sending" /><Promotion v-else /></el-icon>
              </button>
            </div>
          </div>
          <p class="xz-tip">内容由AI生成，仅供参考使用，不作为决策建议～</p>
        </div>
      </template>
    </main>

    <el-drawer v-model="planDrawerVisible" title="当前志愿表" size="400px" direction="rtl">
      <p class="gk-drawer-tip">AI 的读取、加入和移除操作均作用于当前选择的志愿表。</p>
      <div class="gk-drawer-list">
        <article v-for="item in activePlanItems" :key="item.planKey" class="gk-card">
          <div class="gk-card__head">
            <div>
              <strong>{{ item.universityName }}</strong>
              <span>{{ item.majorName || "院校志愿" }} · {{ item.universityProvince || "-" }}</span>
            </div>
            <el-tag size="small" :type="strategyTagType(item.strategy)" effect="light">{{ formatStrategyLabel(item.strategy) }}</el-tag>
          </div>
          <div class="gk-card__stats">
            <span>录取概率 <strong>{{ item.admissionProbability == null ? "待测" : item.admissionProbability + "%" }}</strong></span>
            <span>最低位次 <strong>{{ item.minRank ?? "-" }}</strong></span>
          </div>
        </article>
        <el-empty v-if="!activePlanItems.length" description="暂无志愿" :image-size="80" />
      </div>
      <div class="gk-drawer-actions">
        <el-button class="gk-btn-outline" @click="createPlanDialogVisible = true">新建志愿表</el-button>
        <el-button class="gk-btn-primary" @click="planDrawerVisible = false; emit('jump-to-plans')">查看完整志愿表</el-button>
      </div>
    </el-drawer>

    <el-dialog v-model="addDialogVisible" title="加入志愿表" width="460px" destroy-on-close>
      <p class="gk-dialog-tip">请选择目标志愿表，再确认写入。</p>
      <el-select v-model="addTargetPlanId" class="gk-dialog-select" placeholder="请选择志愿表">
        <el-option v-for="plan in plans" :key="plan.id" :label="plan.planName" :value="String(plan.id)" />
        <el-option label="新建志愿表" value="new" />
      </el-select>
      <el-input v-if="addTargetPlanId === 'new'" v-model.trim="addNewPlanName" maxlength="30" placeholder="输入志愿表名称" />
      <div v-if="pendingAddItem" class="gk-dialog-target">
        <strong>{{ pendingAddItem.universityName }}</strong><span>{{ pendingAddItem.majorName || "院校志愿" }}</span>
      </div>
      <template #footer>
        <el-button @click="addDialogVisible = false">取消</el-button>
        <el-button class="gk-btn-primary" :loading="addSubmitting" @click="confirmAddItem">确认加入</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="createPlanDialogVisible" title="新建志愿表" width="420px" destroy-on-close>
      <el-input v-model.trim="createPlanName" maxlength="30" placeholder="例如：2026湖南志愿方案-A" />
      <template #footer>
        <el-button @click="createPlanDialogVisible = false">取消</el-button>
        <el-button class="gk-btn-primary" :loading="creating" @click="createEmptyPlan">创建</el-button>
      </template>
    </el-dialog>
  </section>
</template>
