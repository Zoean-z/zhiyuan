<script setup>
import { ElMessage, ElNotification } from "element-plus";
import { computed, onMounted, ref } from "vue";
import CurrentPlanPanel from "./CurrentPlanPanel.vue";
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
const currentPlanItems = ref([]);
const currentPlanName = ref("");
const currentPlanUpdatedAt = ref("");
const planNotice = ref(null);

const quickPrompts = [
  "帮我看看我的画像信息",
  "看看我当前的志愿方案",
  "帮我推荐学校",
  "帮我推荐计算机专业"
];

const conversationTitle = computed(() => {
  return conversations.value.find((item) => item.id === activeConversationId.value)?.title || "新的对话";
});

const planMetaText = computed(() => {
  if (!currentPlanName.value) {
    return "当前未加载方案快览";
  }
  return currentPlanUpdatedAt.value
    ? `${currentPlanName.value} · 更新于 ${currentPlanUpdatedAt.value}`
    : currentPlanName.value;
});

function formatStrategyLabel(value) {
  const text = String(value || "").toUpperCase();
  if (text.includes("RUSH") || text.includes("冲")) return "冲刺";
  if (text.includes("GUARANTEE") || text.includes("保")) return "保底";
  return "稳妥";
}

function recommendationCards(message) {
  const items = message?.payload?.topItems;
  return Array.isArray(items) ? items : [];
}

function hasRecommendationCards(message) {
  return recommendationCards(message).length > 0;
}

function hasProfilePayload(message) {
  return message?.toolName === "getUserProfile" && !!message?.payload;
}

function hasPlanPayload(message) {
  return message?.toolName === "getCurrentPlan" && !!message?.payload;
}

function hasErrorPayload(message) {
  return message?.messageType === "tool_result" && !!message?.payload?.errorCategory;
}

function getAuthHeaders(extraHeaders) {
  return {
    ...(extraHeaders || {}),
    Authorization: `Bearer ${props.token}`
  };
}

async function apiFetch(url, options = {}) {
  const { timeoutMs = 15000, ...fetchOptions } = options;
  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), timeoutMs);
  try {
    const response = await fetch(url, { ...fetchOptions, signal: controller.signal });
    const isJson = response.headers.get("content-type")?.includes("application/json");
    const data = isJson ? await response.json() : null;
    if (!response.ok) {
      throw createHttpError(response, data, UI_TEXT.common.requestFailed);
    }
    return data;
  } catch (ex) {
    if (ex?.name === "AbortError") {
      throw new Error(UI_TEXT.common.timeout);
    }
    throw ex;
  } finally {
    window.clearTimeout(timeoutId);
  }
}

function resolveErrorMessage(ex, fallbackMessage = UI_TEXT.common.operationFailed) {
  return normalizeUserError(ex, fallbackMessage);
}

function buildPlanPreviewItems(resultObj) {
  const grouped = buildGroupedFromResult(resultObj || {});
  const items = [];
  [["rush", grouped.rush], ["safe", grouped.safe], ["guarantee", grouped.guarantee]].forEach(([strategy, list]) => {
    (list || []).forEach((item) => {
      const normalized = normalizeItem(item, strategy);
      items.push({
        ...normalized,
        strategy: normalized.strategy || strategy,
        planKey: buildPlanItemKey(item, strategy)
      });
    });
  });
  return items;
}

function applyPlanSnapshotFromPayload(payload) {
  if (!payload || payload.hasPlan === false) {
    currentPlanItems.value = [];
    currentPlanName.value = "";
    currentPlanUpdatedAt.value = "";
    return;
  }
  currentPlanName.value = payload.planName || "当前方案";
  currentPlanUpdatedAt.value = payload.updatedAt
    ? formatDateTime(payload.updatedAt)
    : payload.createdAt
      ? formatDateTime(payload.createdAt)
      : "";

  if (Array.isArray(payload.items)) {
    currentPlanItems.value = payload.items.map((item) => {
      const normalized = normalizeItem(item, item?.strategy);
      return {
        ...normalized,
        strategy: normalized.strategy,
        planKey: buildPlanItemKey(item, normalized.strategy)
      };
    });
  }
}

async function loadPlanPreview() {
  try {
    const detail = await apiFetch("/api/plans/current", {
      method: "GET",
      headers: getAuthHeaders()
    });
    let parsed = null;
    try {
      parsed = detail?.resultJson ? JSON.parse(detail.resultJson) : null;
    } catch {
      parsed = null;
    }
    currentPlanItems.value = buildPlanPreviewItems(parsed || {});
    currentPlanName.value = detail?.planName || "当前方案";
    currentPlanUpdatedAt.value = formatDateTime(detail?.createdAt);
  } catch (ex) {
    if (ex?.status === 404) {
      currentPlanItems.value = [];
      currentPlanName.value = "";
      currentPlanUpdatedAt.value = "";
      return;
    }
    console.error("[agent plan preview]", ex);
  }
}

async function loadConversations() {
  conversationsLoading.value = true;
  try {
    const data = await apiFetch("/api/agent/conversations", {
      method: "GET",
      headers: getAuthHeaders()
    });
    conversations.value = Array.isArray(data) ? data : [];
    if (!activeConversationId.value && conversations.value.length) {
      await openConversation(conversations.value[0].id);
      return;
    }
    if (activeConversationId.value) {
      const stillExists = conversations.value.some((item) => item.id === activeConversationId.value);
      if (!stillExists) {
        activeConversationId.value = null;
      }
    }
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
    const detail = await apiFetch(`/api/agent/conversations/${id}`, {
      method: "GET",
      headers: getAuthHeaders()
    });
    activeConversationId.value = detail.id;
    messages.value = Array.isArray(detail.messages) ? detail.messages : [];
  } catch (ex) {
    ElMessage.error(resolveErrorMessage(ex, "加载对话失败"));
  } finally {
    conversationLoading.value = false;
  }
}

function showPlanNotice(summary) {
  planNotice.value = summary;
  ElNotification({
    title: "志愿表已更新",
    message: summary,
    type: "success",
    duration: 3200
  });
}

async function syncPlanStateFromTurn(generatedMessages) {
  const toolResults = (generatedMessages || []).filter((message) => message.messageType === "tool_result");
  const planSnapshot = toolResults.find((message) => message.toolName === "getCurrentPlan");
  if (planSnapshot?.payload) {
    applyPlanSnapshotFromPayload(planSnapshot.payload);
    return;
  }

  const planMutation = toolResults.find((message) => ["addPlanItem", "removePlanItem", "savePlan"].includes(message.toolName));
  if (!planMutation) {
    return;
  }

  await loadPlanPreview();
  showPlanNotice(planMutation.content || "agent 已更新当前志愿表");
}

async function sendMessage(content = draft.value) {
  const text = String(content || "").trim();
  if (!text || sending.value) {
    return;
  }

  if (!activeConversationId.value) {
    await createConversation(text.slice(0, 12) || "新的志愿对话");
    if (!activeConversationId.value) {
      return;
    }
  }

  const optimisticUserMessage = {
    id: `temp-${Date.now()}`,
    role: "user",
    messageType: "text",
    content: text,
    toolName: null,
    payload: null,
    createdAt: new Date().toISOString()
  };

  messages.value = [...messages.value, optimisticUserMessage];
  draft.value = "";
  sending.value = true;
  try {
    const turn = await apiFetch(`/api/agent/conversations/${activeConversationId.value}/messages`, {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ content: text }),
      timeoutMs: 20000
    });
    messages.value = [...messages.value, ...(Array.isArray(turn.generatedMessages) ? turn.generatedMessages : [])];
    await syncPlanStateFromTurn(turn.generatedMessages || []);
    await loadConversations();
  } catch (ex) {
    messages.value = messages.value.filter((message) => message.id !== optimisticUserMessage.id);
    ElMessage.error(resolveErrorMessage(ex, "发送消息失败"));
  } finally {
    sending.value = false;
  }
}

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
  return "Agent";
}

onMounted(async () => {
  await loadConversations();
  await loadPlanPreview();
  if (!conversations.value.length) {
    await createConversation();
  }
});
</script>

<template>
  <section class="agent-page">
    <header class="agent-page__hero">
      <div>
        <div class="agent-page__eyebrow">受控多轮会话</div>
        <h2>AI 志愿顾问台</h2>
        <p>
          Agent 只能通过受控工具访问你的画像、推荐结果和方案数据，不会自由查库。
          现在它可以与你对话、发起推荐、修改志愿表，并把修改轨迹记录下来。
        </p>
      </div>
      <div class="agent-page__hero-side">
        <div class="agent-page__user">{{ props.user?.username || "当前用户" }}</div>
        <div class="agent-page__profile">
          {{ props.user?.score ?? "-" }} 分 · {{ props.user?.examProvince || "-" }} · {{ props.user?.subjectType || "-" }}
        </div>
      </div>
    </header>

    <div class="agent-workspace">
      <aside class="agent-rail">
        <div class="agent-rail__head">
          <div>
            <div class="agent-rail__title">对话会话</div>
            <div class="agent-rail__hint">切换或新建你的顾问会话</div>
          </div>
          <el-button type="primary" plain :loading="creating" @click="createConversation()">新建</el-button>
        </div>

        <div class="agent-quick-actions">
          <button
            v-for="prompt in quickPrompts"
            :key="prompt"
            type="button"
            class="agent-quick-actions__item"
            @click="sendMessage(prompt)"
          >
            {{ prompt }}
          </button>
        </div>

        <div class="agent-conversation-list" v-loading="conversationsLoading">
          <button
            v-for="item in conversations"
            :key="item.id"
            type="button"
            :class="['agent-conversation-item', { 'is-active': item.id === activeConversationId }]"
            @click="openConversation(item.id)"
          >
            <div class="agent-conversation-item__title">{{ item.title || "未命名会话" }}</div>
            <div class="agent-conversation-item__meta">
              <span>{{ item.messageCount || 0 }} 条消息</span>
              <span>{{ formatDateTime(item.updatedAt || item.createdAt) }}</span>
            </div>
          </button>
        </div>
      </aside>

      <main class="agent-chat-panel">
        <div class="agent-chat-panel__head">
          <div>
            <div class="agent-chat-panel__title">{{ conversationTitle }}</div>
            <div class="agent-chat-panel__hint">当前对话会记录 tool call、tool result 和志愿表变更。</div>
          </div>
          <el-button plain @click="emit('jump-to-plans')">查看志愿方案</el-button>
        </div>

        <transition name="agent-notice">
          <div v-if="planNotice" class="agent-plan-notice">
            <div class="agent-plan-notice__text">{{ planNotice }}</div>
            <div class="agent-plan-notice__actions">
              <el-button text @click="planNotice = null">关闭</el-button>
              <el-button type="primary" plain size="small" @click="emit('jump-to-plans')">快捷查看志愿表</el-button>
            </div>
          </div>
        </transition>

        <div class="agent-message-list" v-loading="conversationLoading">
          <template v-if="messages.length">
            <article
              v-for="message in messages"
              :key="message.id"
              :class="['agent-message-row', `agent-message-row--${message.role}`]"
            >
              <div :class="bubbleClass(message)">
                <div class="agent-bubble__meta">
                  <span>{{ roleLabel(message) }}</span>
                  <span>{{ formatDateTime(message.createdAt) }}</span>
                </div>
                <div class="agent-bubble__content">{{ message.content }}</div>
                <div v-if="message.toolName" class="agent-bubble__tool">
                  <el-tag size="small" effect="plain">{{ message.toolName }}</el-tag>
                </div>
                <div v-if="hasProfilePayload(message)" class="agent-bubble__payload agent-bubble__payload--profile">
                  <div class="agent-payload-grid">
                    <div class="agent-payload-item">
                      <span>用户名</span>
                      <strong>{{ message.payload.username || "-" }}</strong>
                    </div>
                    <div class="agent-payload-item">
                      <span>分数</span>
                      <strong>{{ message.payload.score ?? "-" }}</strong>
                    </div>
                    <div class="agent-payload-item">
                      <span>科类</span>
                      <strong>{{ message.payload.subjectType || "-" }}</strong>
                    </div>
                    <div class="agent-payload-item">
                      <span>省份</span>
                      <strong>{{ message.payload.examProvince || "-" }}</strong>
                    </div>
                  </div>
                </div>
                <div v-if="hasPlanPayload(message)" class="agent-bubble__payload">
                  <div class="agent-inline-summary">
                    <span>方案名称</span>
                    <strong>{{ message.payload.planName || "当前方案" }}</strong>
                  </div>
                  <div class="agent-inline-summary">
                    <span>志愿条数</span>
                    <strong>{{ message.payload.itemCount ?? 0 }}</strong>
                  </div>
                </div>
                <div v-if="hasRecommendationCards(message)" class="agent-bubble__payload agent-bubble__payload--cards">
                  <div class="agent-recommend-card"
                       v-for="(item, index) in recommendationCards(message)"
                       :key="`${message.id}-${index}`">
                    <div class="agent-recommend-card__head">
                      <div>
                        <div class="agent-recommend-card__title">{{ item.label || item.universityName || "推荐结果" }}</div>
                        <div v-if="item.universityProvince || item.recommendationMode" class="agent-recommend-card__meta">
                          {{ item.universityProvince || "-" }} · {{ item.recommendationMode === "MAJOR_FIRST" ? "专业优先" : "学校优先" }}
                        </div>
                      </div>
                      <el-tag size="small" type="success" effect="light">{{ formatStrategyLabel(item.strategyLabel || item.strategy) }}</el-tag>
                    </div>
                    <div class="agent-recommend-card__stats">
                      <span>概率 {{ item.admissionProbability ?? "-" }}%</span>
                      <span>依据 {{ item.recommendationBasis || "-" }}</span>
                      <span v-if="item.majorName">专业 {{ item.majorName }}</span>
                    </div>
                    <div v-if="Array.isArray(item.matchReasons) && item.matchReasons.length" class="agent-recommend-card__reasons">
                      {{ item.matchReasons.slice(0, 2).join("；") }}
                    </div>
                  </div>
                </div>
                <div v-if="hasErrorPayload(message)" class="agent-bubble__payload agent-bubble__payload--error">
                  <div class="agent-inline-summary">
                    <span>错误分类</span>
                    <strong>{{ message.payload.errorCategory }}</strong>
                  </div>
                  <div class="agent-inline-summary">
                    <span>错误码</span>
                    <strong>{{ message.payload.errorCode }}</strong>
                  </div>
                </div>
              </div>
            </article>
          </template>

          <div v-else class="agent-empty-state">
            <h3>开始第一轮对话</h3>
            <p>你可以直接让它推荐学校、推荐专业，或者查看当前志愿表。</p>
          </div>
        </div>

        <div class="agent-input-panel">
          <el-input
            v-model="draft"
            type="textarea"
            :rows="4"
            resize="none"
            placeholder="例如：帮我推荐浙江省内稳一点的学校，或者把第一个加入志愿单"
            @keydown.enter.exact.prevent="sendMessage()"
          />
          <div class="agent-input-panel__footer">
            <div class="agent-input-panel__tip">当前为受控 agent：只会调用我们开放的工具，不会自由执行查询。</div>
            <el-button type="primary" :loading="sending" @click="sendMessage()">发送消息</el-button>
          </div>
        </div>
      </main>

      <aside class="agent-plan-panel">
        <CurrentPlanPanel
          :items="currentPlanItems"
          :save-disabled="true"
          :clearing-disabled="true"
          :read-only="true"
          :show-jump-action="true"
          jump-label="查看志愿表"
          :meta-text="planMetaText"
          @jump="emit('jump-to-plans')"
        />
      </aside>
    </div>
  </section>
</template>
