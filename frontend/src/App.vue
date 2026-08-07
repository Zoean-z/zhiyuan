<script setup>
import { ElMessage, ElMessageBox } from "element-plus";
import { ChatDotRound, Clock, Document, Reading, Search, UserFilled } from "@element-plus/icons-vue";
import { computed, onMounted, provide, reactive, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import admissionJourneyImage from "./assets/admission-journey.png";
import {
  buildPlanItemKey,
  buildGroupedFromResult,
  clearStoredAuth,
  groupByStrategy,
  isUserProfileComplete,
  normalizeItem,
  readStoredAuth,
  recommendationModeLabel,
  saveStoredAuth,
  subjectTypeLabel
} from "./utils/recommendation";
import { UI_TEXT, createHttpError, normalizeUserError } from "./utils/ui";

const router = useRouter();
const currentRoute = useRoute();
const auth = ref(readStoredAuth());
const authMode = ref("login");
const activeMode = ref("text");
const loading = ref(false);
const error = ref("");
const resultSummary = ref("");
const aiSummary = ref("");
const finalAdvice = ref("");
const resultTips = ref([]);
const grouped = reactive({ rush: [], safe: [], guarantee: [] });
const provinces = ref([]);
const latestResult = ref(null);
const latestSourceType = ref("");
const latestSourceQuery = ref("");
const latestRankMeta = ref(null);
const latestQueryContext = ref(null);

const historyLoading = ref(false);
const historyRecords = ref([]);
const historyDialogVisible = ref(false);
const historyDetailLoading = ref(false);
const historyDetail = ref(null);
const historyResultJson = ref("");
const historyGrouped = reactive({ rush: [], safe: [], guarantee: [] });
const historySummary = ref("");
const historyAiSummary = ref("");
const historyFinalAdvice = ref("");
const historyTips = ref([]);
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
const planFinalAdvice = ref("");
const planTips = ref([]);
const planRecommendationMode = ref("");
const planTargetDialogVisible = ref(false);
const planTargetSubmitting = ref(false);
const planTargetId = ref("");
const planTargetNewName = ref("");
const pendingPlanItems = ref([]);
let historyLoadVersion = 0;
let planLoadVersion = 0;

const saveDialogVisible = ref(false);
const saveSubmitting = ref(false);
const saveForm = reactive({ planName: "" });
const majorSuggestionLoading = ref(false);
const majorSuggestions = ref([]);
let majorSuggestionRequestVersion = 0;
const currentPlanItems = ref([]);
const schoolDetailVisible = ref(false);
const schoolDetailLoading = ref(false);
const schoolDetail = ref(null);
const schoolDetailMajors = ref([]);
const schoolDetailSourceItem = ref(null);

const loginForm = reactive({ username: "", password: "" });
const profileForm = reactive({ score: "", subjectType: "", examProvince: "", confirmed: false });
const scoreForm = reactive({ score: "", province: "", subjectType: "", recommendationMode: "SCHOOL_FIRST", majorKeyword: "" });
const textForm = reactive({ requirementText: "" });

const username = computed(() => auth.value?.user?.username || "用户");
const userMeta = computed(() => {
  const user = auth.value?.user || {};
  return [user.examProvince, subjectTypeLabel(user.subjectType), user.score == null ? "" : `${user.score}分`]
    .filter(Boolean)
    .join(" · ");
});
const pageTitle = computed(() => currentRoute.meta.title || "推荐查询");

const historyHasResult = computed(() => historyGrouped.rush.length + historyGrouped.safe.length + historyGrouped.guarantee.length > 0);
const planHasResult = computed(() => planGrouped.rush.length + planGrouped.safe.length + planGrouped.guarantee.length > 0);
const canSavePlan = computed(() => currentPlanItems.value.length > 0);
const selectedPlanKeys = computed(() => currentPlanItems.value.map((item) => item.planKey));
const textParsedRequirement = computed(() => latestSourceType.value === "text" ? latestResult.value?.parsed || null : null);

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

function applyError(ex, fallbackMessage, options = {}) {
  const { notify = false } = options;
  const message = resolveErrorMessage(ex, fallbackMessage);
  error.value = message;
  if (notify) {
    ElMessage.error(message);
  }
  return message;
}

function validateLoginForm() {
  if (!loginForm.username.trim()) {
    return UI_TEXT.form.usernameRequired;
  }
  if (!loginForm.password) {
    return UI_TEXT.form.passwordRequired;
  }
  return "";
}

function validateProfileForm() {
  const score = Number(profileForm.score);
  if (!profileForm.examProvince) return UI_TEXT.form.provinceRequired;
  if (!profileForm.subjectType) return UI_TEXT.form.subjectTypeRequired;
  if (profileForm.score === "" || Number.isNaN(score) || score < 0 || score > 750) {
    return UI_TEXT.form.scoreRequired;
  }
  if (!profileForm.confirmed) return "请确认报考信息填写无误";
  return "";
}

function validateScoreForm() {
  if (!scoreForm.recommendationMode) {
    return UI_TEXT.form.recommendationModeRequired;
  }
  if (scoreForm.score === "" || Number.isNaN(Number(scoreForm.score))) {
    return UI_TEXT.form.scoreRequired;
  }
  if (!scoreForm.province) {
    return UI_TEXT.form.provinceRequired;
  }
  if (!scoreForm.subjectType) {
    return UI_TEXT.form.subjectTypeRequired;
  }
  if (scoreForm.recommendationMode === "MAJOR_FIRST" && !scoreForm.majorKeyword.trim()) {
    return UI_TEXT.form.majorRequired;
  }
  return "";
}

function validateTextForm() {
  return textForm.requirementText.trim() ? "" : UI_TEXT.form.requirementTextRequired;
}

function getAuthHeaders(extraHeaders) {
  const token = auth.value?.token;
  if (!token) {
    throw new Error(UI_TEXT.common.loginRequired);
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
  finalAdvice.value = "";
  resultTips.value = [];
  latestResult.value = null;
  latestSourceType.value = "";
  latestSourceQuery.value = "";
  latestRankMeta.value = null;
  latestQueryContext.value = null;
  resetSchoolDetail();
}

function resetSchoolDetail() {
  schoolDetailVisible.value = false;
  schoolDetailLoading.value = false;
  schoolDetail.value = null;
  schoolDetailMajors.value = [];
  schoolDetailSourceItem.value = null;
}

async function addCurrentPlanItem(item, strategy) {
  await openPlanTargetDialog([{ ...item, strategy }]);
}

function appendCurrentPlanItems(items) {
  const nextItems = [...currentPlanItems.value];
  let addedCount = 0;

  (items || []).forEach((item) => {
    const normalized = normalizeItem(item, item?.strategy);
    const planKey = buildPlanItemKey({ ...item, ...normalized }, normalized.strategy);
    if (nextItems.some((entry) => entry.planKey === planKey)) {
      return;
    }
    nextItems.push({ ...normalized, planKey });
    addedCount += 1;
  });

  if (addedCount > 0) {
    currentPlanItems.value = nextItems;
  }

  return addedCount;
}

function buildSelectedMajorPlanItem(major) {
  const sourceItem = schoolDetailSourceItem.value ? normalizeItem(schoolDetailSourceItem.value) : null;
  const queryContext = latestQueryContext.value || {};
  if (!sourceItem) {
    return null;
  }

  return {
    recommendationMode: sourceItem.recommendationMode || "SCHOOL_FIRST",
    universityId: sourceItem.universityId,
    universityName: sourceItem.universityName,
    majorName: major?.majorName || "",
    universityProvince: sourceItem.universityProvince,
    universityTier: sourceItem.universityTier,
    is985: sourceItem.is985,
    is211: sourceItem.is211,
    isDoubleFirstClass: sourceItem.isDoubleFirstClass,
    schoolTags: Array.isArray(sourceItem.schoolTags) ? sourceItem.schoolTags : [],
    universityTags: sourceItem.universityTags || null,
    cutoffScore: major?.cutoffScore ?? null,
    scoreGap: queryContext.score == null || major?.cutoffScore == null ? null : queryContext.score - major.cutoffScore,
    userRank: sourceItem.userRank ?? null,
    minRank: major?.minRank ?? null,
    rankGap: sourceItem.userRank == null || major?.minRank == null ? null : major.minRank - sourceItem.userRank,
    recommendationBasis: sourceItem.recommendationBasis,
    admissionProbability: sourceItem.admissionProbability,
    strategy: sourceItem.strategy,
    strategyLabel: sourceItem.strategyLabel || null,
    riskScore: sourceItem.riskScore ?? null,
    matchReasons: Array.isArray(sourceItem.matchReasons) ? sourceItem.matchReasons : [],
    explanation: sourceItem.explanation || null
  };
}

async function addSelectedMajorsToPlan(majors) {
  if (!Array.isArray(majors) || !majors.length) {
    ElMessage.warning(UI_TEXT.form.selectMajorRequired);
    return;
  }

  const items = majors.map((major) => buildSelectedMajorPlanItem(major)).filter(Boolean);
  await openPlanTargetDialog(items);
}

async function removeCurrentPlanItem(item) {
  currentPlanItems.value = currentPlanItems.value.filter((entry) => entry.planKey !== item.planKey);
  try {
    await upsertCurrentPlanDraft();
    ElMessage.success(UI_TEXT.success.removeFromPlan);
  } catch (ex) {
    ElMessage.error(applyError(ex, UI_TEXT.failure.savePlan));
    await loadCurrentPlanDraft();
  }
}

async function clearCurrentPlan() {
  if (!currentPlanItems.value.length) {
    return;
  }
  currentPlanItems.value = [];
  try {
    await deleteCurrentPlanDraft();
    ElMessage.success(UI_TEXT.success.clearCurrentPlan);
  } catch (ex) {
    ElMessage.error(applyError(ex, UI_TEXT.failure.savePlan));
    await loadCurrentPlanDraft();
  }
}

function buildPlanPayload() {
  const groups = { rush: [], safe: [], guarantee: [] };
  currentPlanItems.value.forEach((item) => {
    groups[item.strategy || "safe"].push({
      recommendationMode: item.recommendationMode,
      universityId: item.universityId ?? null,
      universityName: item.universityName,
      majorName: item.majorName || null,
      universityProvince: item.universityProvince || null,
      universityTier: item.universityTier || null,
      is985: item.is985 === true,
      is211: item.is211 === true,
      isDoubleFirstClass: item.isDoubleFirstClass === true,
      schoolTags: Array.isArray(item.schoolTags) ? item.schoolTags : [],
      universityTags: item.universityTags || null,
      cutoffScore: item.cutoffScore,
      scoreGap: item.scoreGap,
      userRank: item.userRank,
      minRank: item.minRank,
      rankGap: item.rankGap,
      recommendationBasis: item.recommendationBasis,
      admissionProbability: item.admissionProbability ?? null,
      strategy: String(item.strategy || "safe").toUpperCase(),
      strategyLabel: item.strategyLabel || null,
      riskScore: item.riskScore ?? null,
      matchReasons: Array.isArray(item.matchReasons) ? item.matchReasons : [],
      explanation: item.explanation || null
    });
  });
  return {
    recommendationMode: currentPlanItems.value[0]?.recommendationMode || latestResult.value?.recommendationMode || scoreForm.recommendationMode,
    rush: groups.rush,
    safe: groups.safe,
    guarantee: groups.guarantee,
    summary: resultSummary.value || `当前方案共选择 ${currentPlanItems.value.length} 条志愿结果。`,
    aiSummary: aiSummary.value || "",
    finalAdvice: finalAdvice.value || "",
    tips: resultTips.value
  };
}

function buildPlanItemsFromResult(resultObj) {
  const groupedData = buildGroupedFromResult(resultObj || {});
  const items = [];
  [["rush", groupedData.rush], ["safe", groupedData.safe], ["guarantee", groupedData.guarantee]].forEach(([strategy, list]) => {
    (list || []).forEach((item) => {
      const normalized = normalizeItem(item, strategy);
      items.push({
        ...normalized,
        strategy: normalized.strategy || strategy,
        planKey: buildPlanItemKey(item, normalized.strategy || strategy)
      });
    });
  });
  return items;
}

function buildPlanPayloadForItems(items, base = {}) {
  const previousItems = currentPlanItems.value;
  currentPlanItems.value = items;
  const payload = buildPlanPayload();
  currentPlanItems.value = previousItems;
  return {
    ...payload,
    recommendationMode: base.recommendationMode || payload.recommendationMode,
    summary: `当前方案共选择 ${items.length} 条志愿结果。`,
    aiSummary: base.aiSummary || payload.aiSummary || "",
    finalAdvice: base.finalAdvice || payload.finalAdvice || "",
    tips: Array.isArray(base.tips) ? base.tips : payload.tips
  };
}

async function openPlanTargetDialog(items) {
  pendingPlanItems.value = (items || []).map((item) => normalizeItem(item, item?.strategy));
  if (!pendingPlanItems.value.length) return;
  await loadPlans();
  const preferred = planRecords.value.find((record) => record.planName !== "当前方案草稿") || planRecords.value[0];
  planTargetId.value = preferred ? String(preferred.id) : "new";
  planTargetNewName.value = `2026${auth.value?.user?.examProvince || ""}志愿方案`;
  planTargetDialogVisible.value = true;
}

async function confirmAddToPlan() {
  if (!planTargetId.value) {
    ElMessage.warning("请选择目标志愿表");
    return;
  }
  if (planTargetId.value === "new" && !planTargetNewName.value.trim()) {
    ElMessage.warning("请输入新志愿表名称");
    return;
  }

  planTargetSubmitting.value = true;
  try {
    let detail = null;
    let existingItems = [];
    let parsed = {};
    if (planTargetId.value !== "new") {
      detail = await apiFetch(`/api/plans/${planTargetId.value}`, { method: "GET", headers: getAuthHeaders() });
      try {
        parsed = detail?.resultJson ? JSON.parse(detail.resultJson) : {};
      } catch {
        parsed = {};
      }
      existingItems = buildPlanItemsFromResult(parsed);
    }

    const merged = [...existingItems];
    let addedCount = 0;
    pendingPlanItems.value.forEach((item) => {
      const normalized = normalizeItem(item, item?.strategy);
      const planKey = buildPlanItemKey(normalized, normalized.strategy);
      if (merged.some((entry) => buildPlanItemKey(entry, entry.strategy) === planKey)) return;
      merged.push({ ...normalized, planKey });
      addedCount += 1;
    });
    if (!addedCount) {
      ElMessage.warning("所选结果已在该志愿表中");
      return;
    }

    const payload = buildPlanPayloadForItems(merged, parsed);
    const planName = detail?.planName || planTargetNewName.value.trim();
    const requestBody = {
      planName,
      sourceType: detail?.sourceType || latestSourceType.value || "score",
      sourceQuery: detail?.sourceQuery || latestSourceQuery.value || `手动加入 ${addedCount} 条志愿结果`,
      resultJson: JSON.stringify(payload),
      aiSummary: detail?.aiSummary || payload.aiSummary || payload.summary
    };
    const saved = await apiFetch(planTargetId.value === "new" ? "/api/plans" : `/api/plans/${planTargetId.value}`, {
      method: planTargetId.value === "new" ? "POST" : "PUT",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify(requestBody)
    });
    currentPlanItems.value = merged;
    planTargetId.value = String(saved.id);
    planTargetDialogVisible.value = false;
    pendingPlanItems.value = [];
    resetSchoolDetail();
    await loadPlans();
    ElMessage.success(`已加入《${saved.planName}》，共新增 ${addedCount} 条`);
  } catch (ex) {
    ElMessage.error(applyError(ex, "加入志愿表失败"));
  } finally {
    planTargetSubmitting.value = false;
  }
}

async function loadCurrentPlanDraft() {
  if (!auth.value?.token) {
    currentPlanItems.value = [];
    return;
  }
  try {
    const detail = await apiFetch("/api/plans/current", { method: "GET", headers: getAuthHeaders() });
    let parsed = null;
    try {
      parsed = detail?.resultJson ? JSON.parse(detail.resultJson) : null;
    } catch {
      parsed = null;
    }
    const groupedData = buildGroupedFromResult(parsed || {});
    const nextItems = [];
    [["rush", groupedData.rush], ["safe", groupedData.safe], ["guarantee", groupedData.guarantee]].forEach(([strategy, list]) => {
      (list || []).forEach((item) => {
        const normalized = normalizeItem(item, strategy);
        nextItems.push({
          ...normalized,
          strategy: normalized.strategy || strategy,
          planKey: buildPlanItemKey(item, normalized.strategy || strategy)
        });
      });
    });
    currentPlanItems.value = nextItems;
    latestSourceType.value = detail?.sourceType || latestSourceType.value;
    latestSourceQuery.value = detail?.sourceQuery || latestSourceQuery.value;
  } catch (ex) {
    if (ex?.status === 404) {
      currentPlanItems.value = [];
      latestSourceType.value = "";
      latestSourceQuery.value = "";
      return;
    }
    console.error("[loadCurrentPlanDraft]", ex);
  }
}

async function upsertCurrentPlanDraft() {
  if (!auth.value?.token) {
    return;
  }
  if (!currentPlanItems.value.length) {
    await deleteCurrentPlanDraft();
    return;
  }
  const payload = buildPlanPayload();
  await apiFetch("/api/plans/current", {
    method: "PUT",
    headers: getAuthHeaders({ "Content-Type": "application/json" }),
    body: JSON.stringify({
      planName: "当前方案草稿",
      sourceType: latestSourceType.value || "score",
      sourceQuery: latestSourceQuery.value || `手动选择 ${currentPlanItems.value.length} 条志愿结果`,
      resultJson: JSON.stringify(payload),
      aiSummary: payload.aiSummary || payload.summary || ""
    })
  });
}

async function deleteCurrentPlanDraft() {
  if (!auth.value?.token) {
    return;
  }
  try {
    await apiFetch("/api/plans/current", { method: "DELETE", headers: getAuthHeaders() });
  } catch (ex) {
    if (ex?.status !== 404) {
      throw ex;
    }
  }
}

function fillScoreFromUser() {
  if (!auth.value?.user) return;
  const user = auth.value.user;
  scoreForm.score = user.score ?? "";
  scoreForm.subjectType = user.subjectType || "";
  scoreForm.province = user.examProvince || "";
}

function resetMajorSuggestions() {
  majorSuggestionRequestVersion += 1;
  majorSuggestions.value = [];
  majorSuggestionLoading.value = false;
}

function resetHistoryDialog() {
  historyDetail.value = null;
  historyResultJson.value = "";
  historyGrouped.rush = [];
  historyGrouped.safe = [];
  historyGrouped.guarantee = [];
  historySummary.value = "";
  historyAiSummary.value = "";
  historyFinalAdvice.value = "";
  historyTips.value = [];
  historyRecommendationMode.value = "";
}

function fillProfileFromUser() {
  const user = auth.value?.user || {};
  profileForm.score = user.score ?? "";
  profileForm.subjectType = user.subjectType || "";
  profileForm.examProvince = user.examProvince || "";
}

function resetPlanDialog() {
  planDetail.value = null;
  planResultJson.value = "";
  planGrouped.rush = [];
  planGrouped.safe = [];
  planGrouped.guarantee = [];
  planSummary.value = "";
  planAiSummary.value = "";
  planFinalAdvice.value = "";
  planTips.value = [];
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
  } catch (ex) {
    console.error("[loadMetaOptions]", ex);
    provinces.value = [];
  }
}

async function loadMajorSuggestions(query) {
  const keyword = String(query || "").trim();
  if (scoreForm.recommendationMode !== "MAJOR_FIRST" || !keyword) {
    resetMajorSuggestions();
    return [];
  }

  const requestVersion = ++majorSuggestionRequestVersion;
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
    const suggestions = Array.isArray(data) ? data.filter(Boolean) : [];
    if (requestVersion === majorSuggestionRequestVersion) {
      majorSuggestions.value = suggestions;
    }
    return requestVersion === majorSuggestionRequestVersion ? suggestions : [];
  } catch (ex) {
    console.error("[loadMajorSuggestions]", ex);
    if (requestVersion === majorSuggestionRequestVersion) {
      majorSuggestions.value = [];
    }
    return [];
  } finally {
    if (requestVersion === majorSuggestionRequestVersion) {
      majorSuggestionLoading.value = false;
    }
  }
}

async function openSchoolDetail(item, strategy) {
  const normalized = normalizeItem(item, strategy);
  const queryContext = latestQueryContext.value || {};
  if (!normalized.universityId) {
    ElMessage.warning(UI_TEXT.common.operationFailed);
    return;
  }
  if (!queryContext.province || !queryContext.subjectType) {
    ElMessage.warning(UI_TEXT.common.operationFailed);
    return;
  }

  schoolDetailVisible.value = true;
  schoolDetailLoading.value = true;
  schoolDetailSourceItem.value = { ...normalized, strategy };
  schoolDetail.value = normalized;
  schoolDetailMajors.value = [];

  try {
    const params = new URLSearchParams({
      province: queryContext.province,
      subjectType: queryContext.subjectType
    });
    const data = await apiFetch(
      `/api/recommendations/schools/${normalized.universityId}/majors?${params.toString()}`,
      { method: "GET", headers: getAuthHeaders() }
    );
    schoolDetail.value = normalizeItem({ ...data, strategy });
    schoolDetailMajors.value = Array.isArray(data?.majors) ? data.majors : [];
  } catch (ex) {
    ElMessage.error(applyError(ex, UI_TEXT.failure.loadSchoolDetail));
    resetSchoolDetail();
  } finally {
    schoolDetailLoading.value = false;
  }
}

async function login() {
  error.value = "";
  const validationMessage = validateLoginForm();
  if (validationMessage) {
    error.value = validationMessage;
    ElMessage.warning(validationMessage);
    return;
  }
  loading.value = true;
  try {
    const data = await apiFetch("/api/auth/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ username: loginForm.username, password: loginForm.password })
    });

    auth.value = { token: data.token, user: data };
    saveStoredAuth(auth.value);
    fillScoreFromUser();
    fillProfileFromUser();
    await router.replace(resolvePostAuthTarget(data));
  } catch (ex) {
    applyError(ex, UI_TEXT.failure.login);
  } finally {
    loading.value = false;
  }
}

async function register() {
  error.value = "";
  const validationMessage = validateLoginForm();
  if (validationMessage) {
    error.value = validationMessage;
    ElMessage.warning(validationMessage);
    return;
  }
  loading.value = true;
  try {
    const payload = {
      username: loginForm.username,
      password: loginForm.password
    };

    const data = await apiFetch("/api/auth/register", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload)
    });

    auth.value = { token: data.token, user: data };
    saveStoredAuth(auth.value);
    authMode.value = "login";
    fillScoreFromUser();
    fillProfileFromUser();
    await router.replace(resolvePostAuthTarget(data));
    ElMessage.success(UI_TEXT.success.register);
  } catch (ex) {
    applyError(ex, UI_TEXT.failure.register);
  } finally {
    loading.value = false;
  }
}

async function completeProfile() {
  error.value = "";
  const validationMessage = validateProfileForm();
  if (validationMessage) {
    error.value = validationMessage;
    ElMessage.warning(validationMessage);
    return;
  }
  loading.value = true;
  try {
    const data = await apiFetch("/api/auth/profile", {
      method: "POST",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        score: Number(profileForm.score),
        subjectType: profileForm.subjectType,
        examProvince: profileForm.examProvince
      })
    });
    auth.value = { token: data.token, user: data };
    saveStoredAuth(auth.value);
    fillScoreFromUser();
    fillProfileFromUser();
    profileForm.confirmed = false;
    await router.replace(resolvePostAuthTarget(data));
    ElMessage.success("报考信息已保存");
  } catch (ex) {
    applyError(ex, "报考信息保存失败");
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
  latestSourceType.value = "";
  latestSourceQuery.value = "";
  historyRecords.value = [];
  planRecords.value = [];
  historyDialogVisible.value = false;
  planDialogVisible.value = false;
  saveDialogVisible.value = false;
  await router.replace({ name: "login" });
}

async function queryByScore() {
  error.value = "";
  const validationMessage = validateScoreForm();
  if (validationMessage) {
    error.value = validationMessage;
    ElMessage.warning(validationMessage);
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
    finalAdvice.value = data?.finalAdvice || "";
    resultTips.value = Array.isArray(data?.tips) ? data.tips : [];
    latestResult.value = data;
    latestSourceType.value = "score";
    latestSourceQuery.value = buildScoreSourceQuery();
    latestRankMeta.value = {
      score: Number(scoreForm.score),
      province: scoreForm.province || "",
      subjectTypeLabel: subjectTypeLabel(scoreForm.subjectType),
      userRank: data?.userRank ?? null
    };
    latestQueryContext.value = {
      score: Number(scoreForm.score),
      province: scoreForm.province || "",
      subjectType: scoreForm.subjectType || "",
      userRank: data?.userRank ?? null
    };
  } catch (ex) {
    applyError(ex, UI_TEXT.failure.queryRecommendation);
  } finally {
    loading.value = false;
  }
}

async function queryByText() {
  error.value = "";
  const validationMessage = validateTextForm();
  if (validationMessage) {
    error.value = validationMessage;
    ElMessage.warning(validationMessage);
    return;
  }
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
    finalAdvice.value = data?.finalAdvice || "";
    resultTips.value = Array.isArray(data?.tips) ? data.tips : [];
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
    latestQueryContext.value = {
      score: data?.parsed?.score ?? null,
      province: data?.parsed?.candidateProvince || "",
      subjectType: data?.parsed?.subjectType || "",
      userRank: firstRanked?.userRank ?? null
    };
  } catch (ex) {
    applyError(ex, UI_TEXT.failure.queryFreeText);
  } finally {
    loading.value = false;
  }
}

async function loadHistory() {
  const loadVersion = ++historyLoadVersion;
  historyLoading.value = true;
  try {
    const records = await apiFetch("/api/history", { method: "GET", headers: getAuthHeaders() });
    if (loadVersion === historyLoadVersion) {
      historyRecords.value = records;
    }
  } catch (ex) {
    if (loadVersion !== historyLoadVersion) return;
    applyError(ex, UI_TEXT.failure.loadHistory, { notify: true });
    historyRecords.value = [];
  } finally {
    if (loadVersion === historyLoadVersion) {
      historyLoading.value = false;
    }
  }
}

function invalidateHistoryLoad() {
  historyLoadVersion += 1;
  historyLoading.value = false;
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
    historyFinalAdvice.value = parsed?.finalAdvice || "";
    historyTips.value = Array.isArray(parsed?.tips) ? parsed.tips : [];
    historyRecommendationMode.value =
      parsed?.recommendationMode
      || groupedData.rush[0]?.recommendationMode
      || groupedData.safe[0]?.recommendationMode
      || groupedData.guarantee[0]?.recommendationMode
      || "";
  } catch (ex) {
    applyError(ex, UI_TEXT.failure.loadHistoryDetail, { notify: true });
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
    ElMessage.success(UI_TEXT.success.deleteHistory);
    await loadHistory();
  } catch (ex) {
    ElMessage.error(applyError(ex, UI_TEXT.failure.deleteHistory));
  }
}

async function loadPlans() {
  const loadVersion = ++planLoadVersion;
  planLoading.value = true;
  try {
    const records = await apiFetch("/api/plans", { method: "GET", headers: getAuthHeaders() });
    if (loadVersion === planLoadVersion) {
      planRecords.value = records;
    }
  } catch (ex) {
    if (loadVersion !== planLoadVersion) return;
    applyError(ex, UI_TEXT.failure.loadPlans, { notify: true });
    planRecords.value = [];
  } finally {
    if (loadVersion === planLoadVersion) {
      planLoading.value = false;
    }
  }
}

function invalidatePlanLoad() {
  planLoadVersion += 1;
  planLoading.value = false;
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
    planTips.value = Array.isArray(parsed?.tips) ? parsed.tips : [];
    planFinalAdvice.value = parsed?.finalAdvice || "";
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
    applyError(ex, UI_TEXT.failure.loadPlanDetail, { notify: true });
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
    if (row?.planName === "当前方案草稿") {
      currentPlanItems.value = [];
    }
    if (planDetail.value?.id === row.id) {
      planDialogVisible.value = false;
      resetPlanDialog();
    }
    ElMessage.success(UI_TEXT.success.deletePlan);
    await loadPlans();
  } catch (ex) {
    ElMessage.error(applyError(ex, UI_TEXT.failure.deletePlan));
  }
}

async function updatePlanDetailItems(items) {
  if (!planDetail.value?.id) return false;
  let parsed = {};
  try {
    parsed = planDetail.value.resultJson ? JSON.parse(planDetail.value.resultJson) : {};
  } catch {
    parsed = {};
  }
  const normalizedItems = (items || []).map((item) => normalizeItem(item, item?.strategy));
  const payload = buildPlanPayloadForItems(normalizedItems, parsed);
  try {
    const updated = await apiFetch(`/api/plans/${planDetail.value.id}`, {
      method: "PUT",
      headers: getAuthHeaders({ "Content-Type": "application/json" }),
      body: JSON.stringify({
        planName: planDetail.value.planName,
        sourceType: planDetail.value.sourceType,
        sourceQuery: planDetail.value.sourceQuery,
        resultJson: JSON.stringify(payload),
        aiSummary: planDetail.value.aiSummary || payload.aiSummary || payload.summary
      })
    });
    await openPlanDetail(updated);
    await loadPlans();
    ElMessage.success(`《${updated.planName}》已更新`);
    return true;
  } catch (ex) {
    ElMessage.error(applyError(ex, "更新志愿表失败"));
    return false;
  }
}

function openSavePlanDialog() {
  if (!currentPlanItems.value.length) {
    ElMessage.warning(UI_TEXT.form.currentPlanEmpty);
    return;
  }
  saveForm.planName = "";
  saveDialogVisible.value = true;
}

async function savePlan() {
  if (!saveForm.planName.trim()) {
    ElMessage.warning(UI_TEXT.form.planNameRequired);
    return;
  }
  if (!currentPlanItems.value.length) {
    ElMessage.warning(UI_TEXT.form.currentPlanEmpty);
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
    await deleteCurrentPlanDraft();
    saveDialogVisible.value = false;
    currentPlanItems.value = [];
    ElMessage.success(UI_TEXT.success.savePlan);
    if (currentRoute.name === "plans") {
      await loadPlans();
    }
  } catch (ex) {
    ElMessage.error(applyError(ex, UI_TEXT.failure.savePlan));
  } finally {
    saveSubmitting.value = false;
  }
}

function resolvePostAuthTarget(user = auth.value?.user) {
  const redirect = currentRoute.query.redirect;
  if (!isUserProfileComplete(user)) {
    return {
      name: "profile-setup",
      query: typeof redirect === "string" && redirect.startsWith("/") ? { redirect } : {}
    };
  }
  return typeof redirect === "string" && redirect.startsWith("/")
    ? redirect
    : { name: "recommend" };
}

function navigateTo(name) {
  if (currentRoute.name !== name) {
    router.push({ name });
  }
}

provide("workspace", {
  activeMode,
  addCurrentPlanItem,
  addSelectedMajorsToPlan,
  aiSummary,
  auth,
  authMode,
  canSavePlan,
  completeProfile,
  clearCurrentPlan,
  currentPlanItems,
  deleteHistoryRecord,
  deletePlan,
  error,
  finalAdvice,
  grouped,
  historyAiSummary,
  historyDetail,
  historyDetailLoading,
  historyDialogVisible,
  historyFinalAdvice,
  historyGrouped,
  historyHasResult,
  invalidateHistoryLoad,
  invalidatePlanLoad,
  historyLoading,
  historyRecommendationMode,
  historyRecords,
  historyResultJson,
  historySummary,
  historyTips,
  latestRankMeta,
  latestResult,
  latestSourceType,
  loadCurrentPlanDraft,
  loadHistory,
  loadMajorSuggestions,
  loadPlans,
  loading,
  login,
  loginForm,
  majorSuggestionLoading,
  majorSuggestions,
  navigateTo,
  openHistoryResult,
  openPlanDetail,
  openSavePlanDialog,
  openSchoolDetail,
  planAiSummary,
  planDetail,
  planDetailLoading,
  planDialogVisible,
  planFinalAdvice,
  planGrouped,
  planHasResult,
  planLoading,
  planRecommendationMode,
  planRecords,
  planResultJson,
  planSummary,
  planTips,
  updatePlanDetailItems,
  profileForm,
  provinces,
  queryByScore,
  queryByText,
  register,
  removeCurrentPlanItem,
  resetHistoryDialog,
  resetPlanDialog,
  resultSummary,
  resultTips,
  saveDialogVisible,
  saveForm,
  savePlan,
  saveSubmitting,
  schoolDetail,
  schoolDetailLoading,
  schoolDetailMajors,
  schoolDetailVisible,
  scoreForm,
  selectedPlanKeys,
  textForm,
  textParsedRequirement
});

onMounted(async () => {
  loadMetaOptions();
  fillScoreFromUser();
  fillProfileFromUser();
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
  <RouterView v-if="currentRoute.meta.standalone || !currentRoute.meta.requiresAuth" />

  <div v-else class="app-shell app-layout" :class="{ 'app-layout--agent': currentRoute.name === 'agent' }">
    <aside class="app-sidebar">
      <div class="app-brand">
        <span class="app-brand__mark"><el-icon><Reading /></el-icon></span>
        <span>智愿AI报考平台</span>
      </div>

      <nav class="app-nav" aria-label="主导航">
        <button class="app-nav__item" :class="{ 'is-active': currentRoute.name === 'recommend' }" @click="navigateTo('recommend')">
          <el-icon><Search /></el-icon><span>推荐查询</span>
        </button>
        <button class="app-nav__item" :class="{ 'is-active': currentRoute.name === 'agent' }" @click="navigateTo('agent')">
          <el-icon><ChatDotRound /></el-icon><span>AI 对话</span>
        </button>
        <button class="app-nav__item" :class="{ 'is-active': currentRoute.name === 'history' }" @click="navigateTo('history')">
          <el-icon><Clock /></el-icon><span>历史记录</span>
        </button>
        <button class="app-nav__item" :class="{ 'is-active': currentRoute.name === 'plans' }" @click="navigateTo('plans')">
          <el-icon><Document /></el-icon><span>志愿方案</span>
        </button>
      </nav>

      <div class="app-sidebar__art" aria-hidden="true">
        <img :src="admissionJourneyImage" alt="" />
      </div>
    </aside>

    <section class="app-content">
      <header class="app-header">
        <h1>{{ pageTitle }}</h1>
        <div class="app-user">
          <span class="app-user__meta">{{ userMeta }}</span>
          <span class="app-user__avatar"><el-icon><UserFilled /></el-icon></span>
          <strong>{{ username }}</strong>
          <span class="app-user__divider" />
          <el-button link @click="logout">退出</el-button>
        </div>
      </header>

      <div class="app-route-view">
        <RouterView v-slot="{ Component, route: matchedRoute }">
          <KeepAlive include="RecommendationView">
            <component :is="Component" :key="matchedRoute.name" />
          </KeepAlive>
        </RouterView>
      </div>
    </section>
  </div>

  <el-dialog
    v-model="planTargetDialogVisible"
    title="加入志愿表"
    width="500px"
    destroy-on-close
    class="plan-target-dialog"
  >
    <div class="plan-target-dialog__body">
      <p>请选择本次要写入的志愿表</p>
      <el-radio-group v-model="planTargetId" class="plan-target-options">
        <el-radio v-for="record in planRecords" :key="record.id" :value="String(record.id)" border>
          <span>{{ record.planName }}</span>
        </el-radio>
        <el-radio value="new" border>新建志愿表</el-radio>
      </el-radio-group>
      <el-input
        v-if="planTargetId === 'new'"
        v-model.trim="planTargetNewName"
        maxlength="30"
        placeholder="输入志愿表名称"
      />
      <div class="plan-target-dialog__summary">
        将加入 {{ pendingPlanItems.length }} 条结果
      </div>
    </div>
    <template #footer>
      <el-button @click="planTargetDialogVisible = false">取消</el-button>
      <el-button type="primary" :loading="planTargetSubmitting" @click="confirmAddToPlan">确认加入</el-button>
    </template>
  </el-dialog>
</template>
