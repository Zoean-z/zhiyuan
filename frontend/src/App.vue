<script setup>
import { ElMessage, ElMessageBox } from "element-plus";
import { computed, onMounted, reactive, ref, watch } from "vue";
import ApplicationPlanView from "./components/ApplicationPlanView.vue";
import CurrentPlanPanel from "./components/CurrentPlanPanel.vue";
import HistoryView from "./components/HistoryView.vue";
import RecognizedConditionsPanel from "./components/RecognizedConditionsPanel.vue";
import RecommendationResult from "./components/RecommendationResult.vue";
import {
  RECOMMENDATION_MODE_OPTIONS,
  SUBJECT_OPTIONS,
  buildPlanItemKey,
  buildGroupedFromResult,
  clearStoredAuth,
  formatDateTime,
  groupByStrategy,
  normalizeItem,
  queryTypeLabel,
  readStoredAuth,
  recommendationModeLabel,
  saveStoredAuth,
  sourceTypeLabel,
  subjectTypeLabel
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
const latestResult = ref(null);
const latestSourceType = ref("");
const latestSourceQuery = ref("");
const latestRankMeta = ref(null);

const historyLoading = ref(false);
const historyRecords = ref([]);
const historyDialogVisible = ref(false);
const historyDetailLoading = ref(false);
const historyDetail = ref(null);
const historyResultJson = ref("");
const historyGrouped = reactive({ rush: [], safe: [], guarantee: [] });
const historySummary = ref("");
const historyAiSummary = ref("");
const historyRecommendationMode = ref("");

const planLoading = ref(false);
const planRecords = ref([]);
const planDialogVisible = ref(false);
const planDetailLoading = ref(false);
const planDetail = ref(null);
const planResultJson = ref("");
const planGrouped = reactive({ rush: [], safe: [], guarantee: [] });
const planSummary = ref("");
const planAiSummary = ref("");
const planRecommendationMode = ref("");

const saveDialogVisible = ref(false);
const saveSubmitting = ref(false);
const saveForm = reactive({ planName: "" });
const majorSuggestionLoading = ref(false);
const majorSuggestions = ref([]);
const currentPlanItems = ref([]);

const loginForm = reactive({ username: "", password: "", score: "", subjectType: "", examProvince: "" });
const scoreForm = reactive({ score: "", province: "", subjectType: "", recommendationMode: "SCHOOL_FIRST", majorKeyword: "" });
const textForm = reactive({ requirementText: "" });

const userText = computed(() => {
  if (!auth.value) return "";
  const user = auth.value.user || {};
  return `用户：${user.username || "-"} | 分数：${user.score ?? "-"} | 科类：${subjectTypeLabel(user.subjectType)} | 省份：${user.examProvince || "-"}`;
});

const historyHasResult = computed(() => historyGrouped.rush.length + historyGrouped.safe.length + historyGrouped.guarantee.length > 0);
const planHasResult = computed(() => planGrouped.rush.length + planGrouped.safe.length + planGrouped.guarantee.length > 0);
const canSavePlan = computed(() => currentPlanItems.value.length > 0);
const selectedPlanKeys = computed(() => currentPlanItems.value.map((item) => item.planKey));
const textParsedRequirement = computed(() => latestSourceType.value === "text" ? latestResult.value?.parsed || null : null);

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
  latestResult.value = null;
  latestSourceType.value = "";
  latestSourceQuery.value = "";
  latestRankMeta.value = null;
}

function addCurrentPlanItem(item, strategy) {
  const normalized = normalizeItem(item, strategy);
  const planKey = buildPlanItemKey(item, strategy);
  if (currentPlanItems.value.some((entry) => entry.planKey === planKey)) {
    ElMessage.warning("该条结果已加入当前方案");
    return;
  }
  currentPlanItems.value = [...currentPlanItems.value, { ...normalized, planKey }];
  ElMessage.success("已加入当前方案");
}

function removeCurrentPlanItem(item) {
  currentPlanItems.value = currentPlanItems.value.filter((entry) => entry.planKey !== item.planKey);
  ElMessage.success("已从当前方案移除");
}

function clearCurrentPlan() {
  if (!currentPlanItems.value.length) {
    return;
  }
  currentPlanItems.value = [];
  ElMessage.success("当前方案已清空");
}

function buildPlanPayload() {
  const groups = { rush: [], safe: [], guarantee: [] };
  currentPlanItems.value.forEach((item) => {
    groups[item.strategy || "safe"].push({
      recommendationMode: item.recommendationMode,
      universityName: item.universityName,
      majorName: item.majorName || null,
      universityProvince: item.universityProvince || null,
      universityTier: item.universityTier || null,
      universityTags: item.universityTags || null,
      cutoffScore: item.cutoffScore,
      scoreGap: item.scoreGap,
      userRank: item.userRank,
      minRank: item.minRank,
      rankGap: item.rankGap,
      recommendationBasis: item.recommendationBasis,
      strategy: String(item.strategy || "safe").toUpperCase()
    });
  });
  return {
    recommendationMode: currentPlanItems.value[0]?.recommendationMode || latestResult.value?.recommendationMode || scoreForm.recommendationMode,
    rush: groups.rush,
    safe: groups.safe,
    guarantee: groups.guarantee,
    summary: resultSummary.value || `当前方案共选择 ${currentPlanItems.value.length} 条志愿结果。`,
    aiSummary: aiSummary.value || ""
  };
}

function fillScoreFromUser() {
  if (!auth.value?.user) return;
  const user = auth.value.user;
  scoreForm.score = user.score ?? "";
  scoreForm.subjectType = user.subjectType || "";
  scoreForm.province = user.examProvince || "";
}

function resetMajorSuggestions() {
  majorSuggestions.value = [];
}

function resetHistoryDialog() {
  historyDetail.value = null;
  historyResultJson.value = "";
  historyGrouped.rush = [];
  historyGrouped.safe = [];
  historyGrouped.guarantee = [];
  historySummary.value = "";
  historyAiSummary.value = "";
  historyRecommendationMode.value = "";
}

function resetPlanDialog() {
  planDetail.value = null;
  planResultJson.value = "";
  planGrouped.rush = [];
  planGrouped.safe = [];
  planGrouped.guarantee = [];
  planSummary.value = "";
  planAiSummary.value = "";
  planRecommendationMode.value = "";
}

function buildScoreSourceQuery() {
  const parts = [
    `模式：${recommendationModeLabel(scoreForm.recommendationMode)}`,
    `分数：${scoreForm.score || "-"}`,
    `省份：${scoreForm.province || "-"}`,
    `科类：${subjectTypeLabel(scoreForm.subjectType)}`
  ];
  if (scoreForm.recommendationMode === "MAJOR_FIRST") {
    parts.push(`专业：${scoreForm.majorKeyword || "-"}`);
  }
  return parts.join("，");
}

async function loadMetaOptions() {
  try {
    const data = await apiFetch("/api/meta/options", { method: "GET" });
    provinces.value = Array.isArray(data?.provinces) ? data.provinces : [];
  } catch {
    provinces.value = [];
  }
}

async function loadMajorSuggestions(query) {
  const keyword = String(query || "").trim();
  if (scoreForm.recommendationMode !== "MAJOR_FIRST" || !keyword) {
    resetMajorSuggestions();
    return;
  }

  majorSuggestionLoading.value = true;
  try {
    const params = new URLSearchParams({ keyword });
    if (scoreForm.province) {
      params.set("province", scoreForm.province);
    }
    if (scoreForm.subjectType) {
      params.set("subjectType", scoreForm.subjectType);
    }
    const data = await apiFetch(`/api/meta/major-options?${params.toString()}`, { method: "GET" });
    majorSuggestions.value = Array.isArray(data) ? data : [];
  } catch {
    majorSuggestions.value = [];
  } finally {
    majorSuggestionLoading.value = false;
  }
}

async function login() {
  error.value = "";
  loading.value = true;
  try {
    const payload = { username: loginForm.username, password: loginForm.password };
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
      await apiFetch("/api/auth/logout", { method: "POST", headers: { Authorization: `Bearer ${token}` } });
    } catch {
    }
  }
  auth.value = null;
  clearStoredAuth();
  resetResults();
  currentPlanItems.value = [];
  historyRecords.value = [];
  planRecords.value = [];
  historyDialogVisible.value = false;
  planDialogVisible.value = false;
  saveDialogVisible.value = false;
}

async function queryByScore() {
  error.value = "";
  if (scoreForm.recommendationMode === "MAJOR_FIRST" && !scoreForm.majorKeyword.trim()) {
    error.value = "专业优先模式下请输入专业";
    ElMessage.warning("专业优先模式下请输入专业");
    return;
  }
  loading.value = true;
  resetResults();
  try {
    const data = await apiFetch("/api/recommendations", {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        score: Number(scoreForm.score),
        province: scoreForm.province,
        subjectType: scoreForm.subjectType || null,
        recommendationMode: scoreForm.recommendationMode,
        majorKeyword: scoreForm.recommendationMode === "MAJOR_FIRST" ? scoreForm.majorKeyword.trim() : null
      })
    });
    grouped.rush = Array.isArray(data?.rush) ? data.rush : [];
    grouped.safe = Array.isArray(data?.safe) ? data.safe : [];
    grouped.guarantee = Array.isArray(data?.guarantee) ? data.guarantee : [];
    resultSummary.value = data?.summary || "";
    aiSummary.value = data?.aiSummary || "";
    latestResult.value = data;
    latestSourceType.value = "score";
    latestSourceQuery.value = buildScoreSourceQuery();
    latestRankMeta.value = {
      score: Number(scoreForm.score),
      province: scoreForm.province || "",
      subjectTypeLabel: subjectTypeLabel(scoreForm.subjectType),
      userRank: data?.userRank ?? null
    };
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
    latestResult.value = data;
    latestSourceType.value = "text";
    latestSourceQuery.value = textForm.requirementText.trim();
    const firstRanked = groupedData.rush[0] || groupedData.safe[0] || groupedData.guarantee[0] || null;
    latestRankMeta.value = {
      score: data?.parsed?.score ?? null,
      province: data?.parsed?.candidateProvince || "",
      subjectTypeLabel: subjectTypeLabel(data?.parsed?.subjectType),
      userRank: firstRanked?.userRank ?? null
    };
  } catch (ex) {
    error.value = ex.message;
  } finally {
    loading.value = false;
  }
}

async function loadHistory() {
  historyLoading.value = true;
  try {
    historyRecords.value = await apiFetch("/api/history", { method: "GET", headers: getAuthHeaders() });
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
    const detail = await apiFetch(`/api/history/${row.id}`, { method: "GET", headers: getAuthHeaders() });
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
    historyRecommendationMode.value =
      parsed?.recommendationMode
      || groupedData.rush[0]?.recommendationMode
      || groupedData.safe[0]?.recommendationMode
      || groupedData.guarantee[0]?.recommendationMode
      || "";
  } catch (ex) {
    error.value = ex.message;
    resetHistoryDialog();
  } finally {
    historyDetailLoading.value = false;
  }
}

async function deleteHistoryRecord(row) {
  try {
    await ElMessageBox.confirm("删除后不可恢复，是否继续？", "删除历史记录", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
  } catch {
    return;
  }

  try {
    await apiFetch(`/api/history/${row.id}`, { method: "DELETE", headers: getAuthHeaders() });
    if (historyDetail.value?.id === row.id) {
      historyDialogVisible.value = false;
      resetHistoryDialog();
    }
    ElMessage.success("历史记录已删除");
    await loadHistory();
  } catch (ex) {
    error.value = ex.message;
    ElMessage.error(ex.message || "删除历史记录失败");
  }
}

async function loadPlans() {
  planLoading.value = true;
  try {
    planRecords.value = await apiFetch("/api/plans", { method: "GET", headers: getAuthHeaders() });
  } catch (ex) {
    error.value = ex.message;
    planRecords.value = [];
  } finally {
    planLoading.value = false;
  }
}

async function openPlanDetail(row) {
  planDialogVisible.value = true;
  planDetailLoading.value = true;
  resetPlanDialog();
  try {
    const detail = await apiFetch(`/api/plans/${row.id}`, { method: "GET", headers: getAuthHeaders() });
    planDetail.value = detail;
    planResultJson.value = detail?.resultJson || "";
    planAiSummary.value = detail?.aiSummary || "";

    let parsed = null;
    try {
      parsed = detail?.resultJson ? JSON.parse(detail.resultJson) : null;
    } catch {
      parsed = null;
    }

    const groupedData = buildGroupedFromResult(parsed || {});
    planGrouped.rush = groupedData.rush;
    planGrouped.safe = groupedData.safe;
    planGrouped.guarantee = groupedData.guarantee;
    planSummary.value = parsed?.summary || "";
    if (!planAiSummary.value) {
      planAiSummary.value = parsed?.aiSummary || parsed?.summary || "";
    }
    planRecommendationMode.value =
      parsed?.recommendationMode
      || groupedData.rush[0]?.recommendationMode
      || groupedData.safe[0]?.recommendationMode
      || groupedData.guarantee[0]?.recommendationMode
      || "";
  } catch (ex) {
    error.value = ex.message;
    resetPlanDialog();
  } finally {
    planDetailLoading.value = false;
  }
}

async function deletePlan(row) {
  try {
    await ElMessageBox.confirm("删除后不可恢复，是否继续？", "删除志愿方案", {
      type: "warning",
      confirmButtonText: "删除",
      cancelButtonText: "取消"
    });
  } catch {
    return;
  }

  try {
    await apiFetch(`/api/plans/${row.id}`, { method: "DELETE", headers: getAuthHeaders() });
    if (planDetail.value?.id === row.id) {
      planDialogVisible.value = false;
      resetPlanDialog();
    }
    ElMessage.success("志愿方案已删除");
    await loadPlans();
  } catch (ex) {
    error.value = ex.message;
    ElMessage.error(ex.message || "删除志愿方案失败");
  }
}

function openSavePlanDialog() {
  if (!currentPlanItems.value.length) {
    ElMessage.warning("当前方案为空，请先加入条目");
    return;
  }
  saveForm.planName = "";
  saveDialogVisible.value = true;
}

async function savePlan() {
  if (!saveForm.planName.trim()) {
    ElMessage.warning("请输入方案名称");
    return;
  }
  if (!currentPlanItems.value.length) {
    ElMessage.warning("当前方案为空，请先加入条目");
    return;
  }

  saveSubmitting.value = true;
  try {
    const payload = buildPlanPayload();
    await apiFetch("/api/plans", {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        planName: saveForm.planName.trim(),
        sourceType: latestSourceType.value || "score",
        sourceQuery: latestSourceQuery.value || `手动选择 ${currentPlanItems.value.length} 条志愿结果`,
        resultJson: JSON.stringify(payload),
        aiSummary: payload.aiSummary || payload.summary || ""
      })
    });
    saveDialogVisible.value = false;
    currentPlanItems.value = [];
    ElMessage.success("志愿方案保存成功");
    if (activePage.value === "plans") {
      await loadPlans();
    }
  } catch (ex) {
    error.value = ex.message;
    ElMessage.error(ex.message || "保存志愿方案失败");
  } finally {
    saveSubmitting.value = false;
  }
}

async function switchPage(page) {
  activePage.value = page;
  if (page === "history") {
    await loadHistory();
  }
  if (page === "plans") {
    await loadPlans();
  }
}

onMounted(() => {
  loadMetaOptions();
  fillScoreFromUser();
});

watch(() => scoreForm.recommendationMode, (mode) => {
  if (mode !== "MAJOR_FIRST") {
    scoreForm.majorKeyword = "";
  }
  resetMajorSuggestions();
});

watch(() => scoreForm.province, () => {
  resetMajorSuggestions();
});

watch(() => scoreForm.subjectType, () => {
  resetMajorSuggestions();
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
                  <el-input v-model="loginForm.password" type="password" show-password placeholder="请输入密码" />
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
                    <el-option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
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
            <el-button :type="activePage === 'recommend' ? 'primary' : 'default'" @click="switchPage('recommend')">推荐查询</el-button>
            <el-button :type="activePage === 'history' ? 'primary' : 'default'" @click="switchPage('history')">历史记录</el-button>
            <el-button :type="activePage === 'plans' ? 'primary' : 'default'" @click="switchPage('plans')">志愿方案</el-button>
          </el-button-group>
          <span class="user-text">{{ userText }}</span>
          <el-button type="info" plain @click="logout">退出登录</el-button>
        </el-space>
      </el-header>

      <el-main v-if="activePage === 'recommend'" class="app-main">
        <el-row :gutter="16">
          <el-col :xs="24" :lg="8">
            <div class="recommend-side">
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
                        <el-input v-model.trim="textForm.requirementText" type="textarea" :rows="7" placeholder="例如：我是江苏考生，620分，偏好计算机，想去华东地区，请给出冲刺/稳妥/保底院校建议。" />
                      </el-form-item>
                      <el-button type="primary" class="query-submit" :loading="loading" @click="queryByText">开始推荐</el-button>
                    </el-form>
                  </el-tab-pane>

                  <el-tab-pane label="分数查询" name="score">
                    <el-form label-position="top" :model="scoreForm">
                      <el-form-item label="推荐模式">
                        <el-radio-group v-model="scoreForm.recommendationMode">
                          <el-radio-button v-for="opt in RECOMMENDATION_MODE_OPTIONS" :key="opt.value" :value="opt.value">
                            {{ opt.label }}
                          </el-radio-button>
                        </el-radio-group>
                      </el-form-item>
                      <el-form-item label="分数">
                        <el-input v-model="scoreForm.score" type="number" placeholder="请输入高考分数" />
                      </el-form-item>
                      <el-form-item label="省份">
                        <el-select v-model="scoreForm.province" placeholder="请选择" style="width: 100%;">
                          <el-option v-for="province in provinces" :key="province" :label="province" :value="province" />
                        </el-select>
                      </el-form-item>
                      <el-form-item label="科类">
                        <el-select v-model="scoreForm.subjectType" placeholder="请选择" style="width: 100%;">
                          <el-option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                        </el-select>
                      </el-form-item>
                      <el-form-item v-if="scoreForm.recommendationMode === 'MAJOR_FIRST'" label="专业">
                        <el-select
                          v-model="scoreForm.majorKeyword"
                          filterable
                          remote
                          clearable
                          allow-create
                          default-first-option
                          reserve-keyword
                          :remote-method="loadMajorSuggestions"
                          :loading="majorSuggestionLoading"
                          placeholder="输入专业关键词，例如：计算机、法学、软件工程"
                          style="width: 100%;"
                        >
                          <el-option v-for="item in majorSuggestions" :key="item" :label="item" :value="item" />
                        </el-select>
                      </el-form-item>
                      <el-button type="primary" class="query-submit" :loading="loading" @click="queryByScore">开始推荐</el-button>
                    </el-form>
                  </el-tab-pane>
                </el-tabs>

                <div v-if="error" class="error">{{ error }}</div>
              </el-card>

              <CurrentPlanPanel :items="currentPlanItems" :save-disabled="!canSavePlan" :clearing-disabled="!currentPlanItems.length" @remove="removeCurrentPlanItem" @clear="clearCurrentPlan" @save="openSavePlanDialog" />
            </div>
          </el-col>

          <el-col :xs="24" :lg="16">
            <div class="recommend-result-stack">
              <RecognizedConditionsPanel v-if="textParsedRequirement" :parsed="textParsedRequirement" />
              <RecommendationResult :loading="loading" :grouped="grouped" :summary="resultSummary" :ai-summary="aiSummary" :recommendation-mode="latestResult?.recommendationMode || latestResult?.parsed?.recommendationMode || scoreForm.recommendationMode" :rank-meta="latestRankMeta" :show-add-action="true" :selected-plan-keys="selectedPlanKeys" @add-item="addCurrentPlanItem" />
            </div>
          </el-col>
        </el-row>
      </el-main>

      <el-main v-else-if="activePage === 'history'" class="app-main">
        <HistoryView :records="historyRecords" :loading="historyLoading" @refresh="loadHistory" @view="openHistoryResult" @delete="deleteHistoryRecord" />
      </el-main>

      <el-main v-else class="app-main">
        <ApplicationPlanView :records="planRecords" :loading="planLoading" @refresh="loadPlans" @view="openPlanDetail" @delete="deletePlan" />
      </el-main>
    </el-container>

    <el-dialog v-model="saveDialogVisible" title="保存志愿方案" width="420px" destroy-on-close>
      <el-form label-position="top" :model="saveForm">
        <el-form-item label="方案名称" required>
          <el-input v-model.trim="saveForm.planName" maxlength="50" placeholder="请输入方案名称" show-word-limit />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="saveDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saveSubmitting" @click="savePlan">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="historyDialogVisible" title="历史结果" width="80%" top="4vh" destroy-on-close>
      <el-skeleton :loading="historyDetailLoading" animated>
        <template #template>
          <el-skeleton-item variant="h1" style="width: 50%;" />
          <el-skeleton-item variant="text" style="margin-top: 8px;" />
          <el-skeleton-item variant="text" />
        </template>
        <template #default>
          <div v-if="historyDetail" class="history-detail-meta">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="查询时间">{{ formatDateTime(historyDetail.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="查询类型">{{ queryTypeLabel(historyDetail.queryType) }}</el-descriptions-item>
              <el-descriptions-item label="查询内容">{{ historyDetail.queryContent }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <RecommendationResult v-if="historyHasResult" :loading="false" :grouped="historyGrouped" :summary="historySummary" :ai-summary="historyAiSummary" :recommendation-mode="historyRecommendationMode" />
          <el-card v-else shadow="never" class="history-raw-card">
            <template #header>原始结果</template>
            <pre class="history-raw">{{ historyResultJson || "暂无可展示结果" }}</pre>
          </el-card>
        </template>
      </el-skeleton>
    </el-dialog>

    <el-dialog v-model="planDialogVisible" title="方案详情" width="80%" top="4vh" destroy-on-close>
      <el-skeleton :loading="planDetailLoading" animated>
        <template #template>
          <el-skeleton-item variant="h1" style="width: 50%;" />
          <el-skeleton-item variant="text" style="margin-top: 8px;" />
          <el-skeleton-item variant="text" />
        </template>
        <template #default>
          <div v-if="planDetail" class="history-detail-meta">
            <el-descriptions :column="1" border>
              <el-descriptions-item label="方案名称">{{ planDetail.planName }}</el-descriptions-item>
              <el-descriptions-item label="来源类型">{{ sourceTypeLabel(planDetail.sourceType) }}</el-descriptions-item>
              <el-descriptions-item label="创建时间">{{ formatDateTime(planDetail.createdAt) }}</el-descriptions-item>
              <el-descriptions-item label="来源内容">{{ planDetail.sourceQuery }}</el-descriptions-item>
            </el-descriptions>
          </div>
          <RecommendationResult v-if="planHasResult" :loading="false" :grouped="planGrouped" :summary="planSummary" :ai-summary="planAiSummary" :recommendation-mode="planRecommendationMode" />
          <el-card v-else shadow="never" class="history-raw-card">
            <template #header>原始结果</template>
            <pre class="history-raw">{{ planResultJson || "暂无可展示结果" }}</pre>
          </el-card>
        </template>
      </el-skeleton>
    </el-dialog>
  </div>
</template>
