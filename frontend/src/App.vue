<script setup>
import { computed, onMounted, reactive, ref } from "vue";
import HistoryView from "./components/HistoryView.vue";
import RecommendationResult from "./components/RecommendationResult.vue";
import {
  SUBJECT_OPTIONS,
  buildGroupedFromResult,
  clearStoredAuth,
  formatDateTime,
  groupByStrategy,
  queryTypeLabel,
  readStoredAuth,
  saveStoredAuth
} from "./utils/recommendation";

const auth = ref(readStoredAuth());
const activePage = ref("recommend");
const activeMode = ref("text");
const loading = ref(false);
const error = ref("");
const resultSummary = ref("");
const aiSummary = ref("");
const grouped = reactive({ rush: [], safe: [], guarantee: [] });
const provinces = ref([]);

const historyLoading = ref(false);
const historyRecords = ref([]);
const historyDialogVisible = ref(false);
const historyDetailLoading = ref(false);
const historyDetail = ref(null);
const historyResultJson = ref("");
const historyGrouped = reactive({ rush: [], safe: [], guarantee: [] });
const historySummary = ref("");
const historyAiSummary = ref("");

const loginForm = reactive({
  username: "",
  password: "",
  score: "",
  subjectType: "",
  examProvince: ""
});

const scoreForm = reactive({
  score: "",
  province: "",
  subjectType: ""
});

const textForm = reactive({
  requirementText: ""
});

const userText = computed(() => {
  if (!auth.value) return "";
  const user = auth.value.user || {};
  return `用户：${user.username || "-"} | 分数：${user.score ?? "-"} | 科类：${user.subjectType || "-"} | 省份：${user.examProvince || "-"}`;
});

const historyHasResult = computed(
  () => historyGrouped.rush.length + historyGrouped.safe.length + historyGrouped.guarantee.length > 0
);

async function apiFetch(url, options) {
  const response = await fetch(url, options);
  const isJson = response.headers.get("content-type")?.includes("application/json");
  const data = isJson ? await response.json() : null;
  if (!response.ok) {
    throw new Error(data?.message || "请求失败");
  }
  return data;
}

function getAuthHeaders(extraHeaders) {
  const token = auth.value?.token;
  if (!token) {
    throw new Error("请先登录");
  }
  return {
    ...(extraHeaders || {}),
    Authorization: `Bearer ${token}`
  };
}

function resetResults() {
  grouped.rush = [];
  grouped.safe = [];
  grouped.guarantee = [];
  resultSummary.value = "";
  aiSummary.value = "";
}

function fillScoreFromUser() {
  if (!auth.value?.user) return;
  const user = auth.value.user;
  scoreForm.score = user.score ?? "";
  scoreForm.subjectType = user.subjectType || "";
  scoreForm.province = user.examProvince || "";
}

function resetHistoryDialog() {
  historyDetail.value = null;
  historyResultJson.value = "";
  historyGrouped.rush = [];
  historyGrouped.safe = [];
  historyGrouped.guarantee = [];
  historySummary.value = "";
  historyAiSummary.value = "";
}

async function loadMetaOptions() {
  try {
    const data = await apiFetch("/api/meta/options", { method: "GET" });
    provinces.value = Array.isArray(data?.provinces) ? data.provinces : [];
  } catch {
    provinces.value = [];
  }
}

async function login() {
  error.value = "";
  loading.value = true;
  try {
    const payload = {
      username: loginForm.username,
      password: loginForm.password
    };
    if (loginForm.score !== "") payload.score = Number(loginForm.score);
    if (loginForm.subjectType) payload.subjectType = loginForm.subjectType;
    if (loginForm.examProvince) payload.examProvince = loginForm.examProvince;

    const data = await apiFetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    auth.value = { token: data.token, user: data };
    saveStoredAuth(auth.value);
    activePage.value = "recommend";
    fillScoreFromUser();
  } catch (ex) {
    error.value = ex.message;
  } finally {
    loading.value = false;
  }
}

async function logout() {
  const token = auth.value?.token;
  if (token) {
    try {
      await apiFetch("/api/auth/logout", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` }
      });
    } catch {
    }
  }
  auth.value = null;
  clearStoredAuth();
  resetResults();
  historyRecords.value = [];
  historyDialogVisible.value = false;
}

async function queryByScore() {
  error.value = "";
  loading.value = true;
  resetResults();
  try {
    const data = await apiFetch("/api/recommendations", {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        score: Number(scoreForm.score),
        province: scoreForm.province,
        subjectType: scoreForm.subjectType || null
      })
    });
    grouped.rush = Array.isArray(data?.rush) ? data.rush : [];
    grouped.safe = Array.isArray(data?.safe) ? data.safe : [];
    grouped.guarantee = Array.isArray(data?.guarantee) ? data.guarantee : [];
    resultSummary.value = data?.summary || "";
  } catch (ex) {
    error.value = ex.message;
  } finally {
    loading.value = false;
  }
}

async function queryByText() {
  error.value = "";
  loading.value = true;
  resetResults();
  try {
    const data = await apiFetch("/api/recommendations/free-text", {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({ requirementText: textForm.requirementText })
    });
    const groupedData = groupByStrategy(data?.recommendations || []);
    grouped.rush = groupedData.rush;
    grouped.safe = groupedData.safe;
    grouped.guarantee = groupedData.guarantee;
    resultSummary.value = data?.summary || "";
    aiSummary.value = data?.aiSummary || "";
  } catch (ex) {
    error.value = ex.message;
  } finally {
    loading.value = false;
  }
}

async function loadHistory() {
  historyLoading.value = true;
  try {
    historyRecords.value = await apiFetch("/api/history", {
      method: "GET",
      headers: getAuthHeaders()
    });
  } catch (ex) {
    error.value = ex.message;
    historyRecords.value = [];
  } finally {
    historyLoading.value = false;
  }
}

async function openHistoryResult(row) {
  historyDialogVisible.value = true;
  historyDetailLoading.value = true;
  resetHistoryDialog();
  try {
    const detail = await apiFetch(`/api/history/${row.id}`, {
      method: "GET",
      headers: getAuthHeaders()
    });
    historyDetail.value = detail;
    historyResultJson.value = detail?.resultJson || "";

    let parsed = null;
    try {
      parsed = detail?.resultJson ? JSON.parse(detail.resultJson) : null;
    } catch {
      parsed = null;
    }

    const groupedData = buildGroupedFromResult(parsed || {});
    historyGrouped.rush = groupedData.rush;
    historyGrouped.safe = groupedData.safe;
    historyGrouped.guarantee = groupedData.guarantee;
    historySummary.value = parsed?.summary || "";
    historyAiSummary.value = parsed?.aiSummary || "";
  } catch (ex) {
    error.value = ex.message;
    resetHistoryDialog();
  } finally {
    historyDetailLoading.value = false;
  }
}

async function switchPage(page) {
  activePage.value = page;
  if (page === "history") {
    await loadHistory();
  }
}

onMounted(() => {
  loadMetaOptions();
  fillScoreFromUser();
});
</script>

<template>
  <div v-if="!auth" class="app-shell">
    <el-container class="auth-container">
      <el-main class="auth-main">
        <el-card class="auth-card" shadow="never">
          <div class="auth-head">
            <h1>高考志愿推荐系统</h1>
            <p>AI 助手为你生成冲刺、稳妥、保底三档院校建议</p>
          </div>

          <el-form label-position="top" :model="loginForm">
            <el-row :gutter="12">
              <el-col :span="24">
                <el-form-item label="用户名">
                  <el-input v-model.trim="loginForm.username" placeholder="请输入用户名" />
                </el-form-item>
              </el-col>
              <el-col :span="24">
                <el-form-item label="密码">
                  <el-input
                    v-model="loginForm.password"
                    type="password"
                    show-password
                    placeholder="请输入密码"
                  />
                </el-form-item>
              </el-col>
            </el-row>

            <el-row :gutter="12">
              <el-col :xs="24" :sm="12">
                <el-form-item label="分数（可选）">
                  <el-input v-model="loginForm.score" type="number" placeholder="例如 620" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :sm="12">
                <el-form-item label="科类（可选）">
                  <el-select v-model="loginForm.subjectType" placeholder="请选择" style="width: 100%;">
                    <el-option
                      v-for="opt in SUBJECT_OPTIONS"
                      :key="opt.value"
                      :label="opt.label"
                      :value="opt.value"
                    />
                  </el-select>
                </el-form-item>
              </el-col>
            </el-row>

            <el-form-item label="省份（可选）">
              <el-select v-model="loginForm.examProvince" placeholder="请选择" style="width: 100%;">
                <el-option v-for="province in provinces" :key="province" :label="province" :value="province" />
              </el-select>
            </el-form-item>

            <el-button type="primary" class="auth-submit" :loading="loading" @click="login">登录</el-button>
            <div v-if="error" class="error">{{ error }}</div>
          </el-form>
        </el-card>
      </el-main>
    </el-container>
  </div>

  <div v-else class="app-shell">
    <el-container class="dashboard">
      <el-header class="app-header">
        <div>
          <h2>高考志愿推荐中心</h2>
          <p>结合分数与意向文本，智能生成志愿建议</p>
        </div>
        <el-space alignment="center" :size="12" wrap>
          <el-button-group>
            <el-button :type="activePage === 'recommend' ? 'primary' : 'default'" @click="switchPage('recommend')">
              推荐查询
            </el-button>
            <el-button :type="activePage === 'history' ? 'primary' : 'default'" @click="switchPage('history')">
              历史记录
            </el-button>
          </el-button-group>
          <span class="user-text">{{ userText }}</span>
          <el-button type="info" plain @click="logout">退出登录</el-button>
        </el-space>
      </el-header>

      <el-main v-if="activePage === 'recommend'" class="app-main">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="8">
            <el-card class="query-card" shadow="never">
              <template #header>
                <div class="panel-title-row">
                  <span>查询条件</span>
                  <el-tag size="small" type="primary" effect="plain">分数查询 / 文本查询</el-tag>
                </div>
              </template>

              <el-tabs v-model="activeMode">
                <el-tab-pane label="文本查询" name="text">
                  <el-form label-position="top" :model="textForm">
                    <el-form-item label="需求描述">
                      <el-input
                        v-model.trim="textForm.requirementText"
                        type="textarea"
                        :rows="7"
                        placeholder="例如：我是江苏考生，620分，偏好计算机，想去华东地区，请给出冲刺/稳妥/保底院校建议。"
                      />
                    </el-form-item>
                    <el-button type="primary" class="query-submit" :loading="loading" @click="queryByText">
                      开始推荐
                    </el-button>
                  </el-form>
                </el-tab-pane>

                <el-tab-pane label="分数查询" name="score">
                  <el-form label-position="top" :model="scoreForm">
                    <el-form-item label="分数">
                      <el-input v-model="scoreForm.score" type="number" placeholder="请输入高考分数" />
                    </el-form-item>
                    <el-form-item label="省份">
                      <el-select v-model="scoreForm.province" placeholder="请选择" style="width: 100%;">
                        <el-option
                          v-for="province in provinces"
                          :key="province"
                          :label="province"
                          :value="province"
                        />
                      </el-select>
                    </el-form-item>
                    <el-form-item label="科类">
                      <el-select v-model="scoreForm.subjectType" placeholder="请选择" style="width: 100%;">
                        <el-option
                          v-for="opt in SUBJECT_OPTIONS"
                          :key="opt.value"
                          :label="opt.label"
                          :value="opt.value"
                        />
                      </el-select>
                    </el-form-item>
                    <el-button type="primary" class="query-submit" :loading="loading" @click="queryByScore">
                      开始推荐
                    </el-button>
                  </el-form>
                </el-tab-pane>
              </el-tabs>

              <div v-if="error" class="error">{{ error }}</div>
            </el-card>
          </el-col>

          <el-col :xs="24" :lg="16">
            <RecommendationResult
              :loading="loading"
              :grouped="grouped"
              :summary="resultSummary"
              :ai-summary="aiSummary"
            />
          </el-col>
        </el-row>
      </el-main>

      <el-main v-else class="app-main">
        <HistoryView :records="historyRecords" :loading="historyLoading" @refresh="loadHistory" @view="openHistoryResult" />
      </el-main>
    </el-container>

    <el-dialog
      v-model="historyDialogVisible"
      title="历史结果"
      width="80%"
      top="4vh"
      destroy-on-close
    >
      <el-skeleton :loading="historyDetailLoading" animated>
        <template #template>
          <el-skeleton-item variant="h1" style="width: 50%;" />
          <el-skeleton-item variant="text" style="margin-top: 8px;" />
          <el-skeleton-item variant="text" />
        </template>

        <template #default>
          <div v-if="historyDetail" class="history-detail-meta">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="查询时间">
                {{ formatDateTime(historyDetail.createdAt) }}
              </el-descriptions-item>
              <el-descriptions-item label="查询类型">
                {{ queryTypeLabel(historyDetail.queryType) }}
              </el-descriptions-item>
              <el-descriptions-item label="查询内容">
                {{ historyDetail.queryContent }}
              </el-descriptions-item>
            </el-descriptions>
          </div>

          <RecommendationResult
            v-if="historyHasResult"
            :loading="false"
            :grouped="historyGrouped"
            :summary="historySummary"
            :ai-summary="historyAiSummary"
          />

          <el-card v-else shadow="never" class="history-raw-card">
            <template #header>原始结果</template>
            <pre class="history-raw">{{ historyResultJson || "暂无可展示结果" }}</pre>
          </el-card>
        </template>
      </el-skeleton>
    </el-dialog>
  </div>
</template>
