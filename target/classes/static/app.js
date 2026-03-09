(function () {
  const { createApp, ref, reactive, computed, onMounted } = Vue;

  const SUBJECT_OPTIONS = [
    { value: "PHYSICS", label: "物理" },
    { value: "HISTORY", label: "历史" }
  ];

  function readStoredAuth() {
    try {
      return JSON.parse(localStorage.getItem("zhiyuan_auth") || "null");
    } catch (e) {
      return null;
    }
  }

  function saveStoredAuth(auth) {
    localStorage.setItem("zhiyuan_auth", JSON.stringify(auth));
  }

  function clearStoredAuth() {
    localStorage.removeItem("zhiyuan_auth");
  }

  async function apiFetch(url, options) {
    const response = await fetch(url, options);
    const isJson = response.headers.get("content-type")?.includes("application/json");
    const data = isJson ? await response.json() : null;
    if (!response.ok) {
      const message = data?.message || "请求失败";
      throw new Error(message);
    }
    return data;
  }

  function normalizeStrategy(value) {
    const v = String(value || "").toUpperCase();
    if (v.includes("RUSH") || v.includes("冲")) return "rush";
    if (v.includes("SAFE") || v.includes("稳")) return "safe";
    if (v.includes("GUARANTEE") || v.includes("保")) return "guarantee";
    return "safe";
  }

  function pickValue(obj, keys) {
    for (const key of keys) {
      if (obj && obj[key] !== undefined && obj[key] !== null && obj[key] !== "") {
        return obj[key];
      }
    }
    return null;
  }

  function normalizeItem(item, fallbackStrategy) {
    const strategy = normalizeStrategy(pickValue(item, ["strategy", "strategyType", "type"]) || fallbackStrategy);
    return {
      universityName: pickValue(item, ["universityName", "schoolName", "name"]) || "未知院校",
      cutoffScore: pickValue(item, ["cutoffScore", "cutoff", "lastYearCutoff"]),
      scoreGap: pickValue(item, ["scoreGap", "gap", "difference"]),
      admissionProbability: pickValue(item, ["admissionProbability", "probability", "chance"]),
      strategy
    };
  }

  function dedupeByUniversity(list) {
    const seen = new Set();
    const result = [];
    (list || []).forEach((item) => {
      const name = String(pickValue(item, ["universityName", "schoolName", "name"]) || "")
        .trim()
        .toLowerCase();
      if (!name || !seen.has(name)) {
        if (name) seen.add(name);
        result.push(item);
      }
    });
    return result;
  }

  function groupByStrategy(items) {
    const buckets = { rush: [], safe: [], guarantee: [] };
    (items || []).forEach((item) => {
      buckets[normalizeStrategy(item?.strategy)].push(item);
    });
    return {
      rush: dedupeByUniversity(buckets.rush),
      safe: dedupeByUniversity(buckets.safe),
      guarantee: dedupeByUniversity(buckets.guarantee)
    };
  }

  function buildGroupedFromResult(resultObj) {
    if (Array.isArray(resultObj?.rush) || Array.isArray(resultObj?.safe) || Array.isArray(resultObj?.guarantee)) {
      return {
        rush: Array.isArray(resultObj?.rush) ? resultObj.rush : [],
        safe: Array.isArray(resultObj?.safe) ? resultObj.safe : [],
        guarantee: Array.isArray(resultObj?.guarantee) ? resultObj.guarantee : []
      };
    }
    if (Array.isArray(resultObj?.recommendations)) {
      return groupByStrategy(resultObj.recommendations);
    }
    return { rush: [], safe: [], guarantee: [] };
  }

  function queryTypeLabel(type) {
    return type === "score" ? "分数查询" : type === "text" ? "文本查询" : "未知";
  }

  function queryTypeTag(type) {
    return type === "score" ? "success" : type === "text" ? "warning" : "info";
  }

  function formatDateTime(value) {
    if (!value) return "-";
    const normalized = String(value).replace(" ", "T");
    const date = new Date(normalized);
    if (Number.isNaN(date.getTime())) {
      return String(value);
    }
    return date.toLocaleString("zh-CN", { hour12: false });
  }

  const UniversityCard = {
    name: "UniversityCard",
    props: {
      item: { type: Object, required: true },
      strategy: { type: String, default: "safe" }
    },
    computed: {
      model() {
        return normalizeItem(this.item, this.strategy);
      },
      strategyLabel() {
        return this.model.strategy === "rush"
          ? "冲刺"
          : this.model.strategy === "guarantee"
            ? "保底"
            : "稳妥";
      },
      strategyType() {
        return this.model.strategy === "rush"
          ? "danger"
          : this.model.strategy === "guarantee"
            ? "success"
            : "warning";
      },
      probabilityText() {
        return this.model.admissionProbability === null ? "-" : `${this.model.admissionProbability}%`;
      }
    },
    template: `
      <el-card class="university-card" shadow="hover">
        <div class="university-card__head">
          <h4 class="university-card__name">{{ model.universityName }}</h4>
          <el-tag size="small" :type="strategyType" effect="light">{{ strategyLabel }}</el-tag>
        </div>
        <div class="university-card__meta">
          <div class="meta-row"><span>录取线</span><strong>{{ model.cutoffScore ?? '-' }}</strong></div>
          <div class="meta-row"><span>分差</span><strong>{{ model.scoreGap ?? '-' }}</strong></div>
          <div class="meta-row"><span>录取概率</span><strong>{{ probabilityText }}</strong></div>
        </div>
      </el-card>
    `
  };

  const AiSummaryPanel = {
    name: "AiSummaryPanel",
    props: {
      aiSummary: { type: String, default: "" },
      summary: { type: String, default: "" }
    },
    computed: {
      displaySummary() {
        return this.aiSummary || this.summary || "";
      }
    },
    template: `
      <el-card class="summary-panel" shadow="never">
        <template #header>
          <div class="panel-title-row">
            <span>AI 总结</span>
            <el-tag size="small" type="info" effect="plain">报考建议</el-tag>
          </div>
        </template>
        <div v-if="displaySummary" class="summary-text">{{ displaySummary }}</div>
        <el-empty v-else description="暂无 AI 总结" :image-size="90" />
      </el-card>
    `
  };

  const RecommendationResult = {
    name: "RecommendationResult",
    components: { UniversityCard, AiSummaryPanel },
    props: {
      loading: { type: Boolean, default: false },
      grouped: { type: Object, required: true },
      aiSummary: { type: String, default: "" },
      summary: { type: String, default: "" }
    },
    data() {
      return { activeTab: "rush" };
    },
    computed: {
      rushList() {
        return Array.isArray(this.grouped?.rush) ? this.grouped.rush : [];
      },
      safeList() {
        return Array.isArray(this.grouped?.safe) ? this.grouped.safe : [];
      },
      guaranteeList() {
        return Array.isArray(this.grouped?.guarantee) ? this.grouped.guarantee : [];
      },
      hasAnyData() {
        return this.rushList.length + this.safeList.length + this.guaranteeList.length > 0;
      }
    },
    template: `
      <section class="result-page">
        <el-card class="result-hero" shadow="never">
          <h2>推荐结果</h2>
          <p>按冲刺、稳妥、保底查看推荐院校，快速对比分数匹配度。</p>
        </el-card>

        <el-card class="result-main" shadow="never">
          <template #header>
            <div class="panel-title-row">
              <span>院校推荐</span>
              <el-tag size="small" type="primary" effect="plain">冲刺 / 稳妥 / 保底</el-tag>
            </div>
          </template>

          <el-skeleton :loading="loading" animated>
            <template #template>
              <div class="cards-grid">
                <el-skeleton-item variant="p" style="height: 140px; border-radius: 12px;" />
                <el-skeleton-item variant="p" style="height: 140px; border-radius: 12px;" />
                <el-skeleton-item variant="p" style="height: 140px; border-radius: 12px;" />
              </div>
            </template>

            <template #default>
              <el-tabs v-model="activeTab" class="recommend-tabs">
                <el-tab-pane :label="'冲刺 (' + rushList.length + ')'" name="rush">
                  <div v-if="rushList.length" class="cards-grid">
                    <UniversityCard v-for="(item, idx) in rushList" :key="'rush-'+idx" :item="item" strategy="rush" />
                  </div>
                  <el-empty v-else description="暂无冲刺院校" :image-size="90" />
                </el-tab-pane>

                <el-tab-pane :label="'稳妥 (' + safeList.length + ')'" name="safe">
                  <div v-if="safeList.length" class="cards-grid">
                    <UniversityCard v-for="(item, idx) in safeList" :key="'safe-'+idx" :item="item" strategy="safe" />
                  </div>
                  <el-empty v-else description="暂无稳妥院校" :image-size="90" />
                </el-tab-pane>

                <el-tab-pane :label="'保底 (' + guaranteeList.length + ')'" name="guarantee">
                  <div v-if="guaranteeList.length" class="cards-grid">
                    <UniversityCard v-for="(item, idx) in guaranteeList" :key="'guarantee-'+idx" :item="item" strategy="guarantee" />
                  </div>
                  <el-empty v-else description="暂无保底院校" :image-size="90" />
                </el-tab-pane>
              </el-tabs>

              <el-empty v-if="!hasAnyData" description="暂无推荐数据，请先发起查询。" :image-size="100" />
            </template>
          </el-skeleton>
        </el-card>

        <AiSummaryPanel :ai-summary="aiSummary" :summary="summary" />
      </section>
    `
  };

  const HistoryView = {
    name: "HistoryView",
    props: {
      records: { type: Array, default: () => [] },
      loading: { type: Boolean, default: false }
    },
    emits: ["refresh", "view"],
    methods: {
      queryTypeLabel,
      queryTypeTag,
      formatDateTime
    },
    template: `
      <el-card class="history-card" shadow="never">
        <template #header>
          <div class="panel-title-row">
            <span>历史记录</span>
            <el-button type="primary" plain size="small" @click="$emit('refresh')">刷新</el-button>
          </div>
        </template>

        <el-table v-if="records.length" :data="records" v-loading="loading" border>
          <el-table-column label="查询时间" min-width="180">
            <template #default="scope">{{ formatDateTime(scope.row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="查询类型" width="120">
            <template #default="scope">
              <el-tag :type="queryTypeTag(scope.row.queryType)" effect="light">{{ queryTypeLabel(scope.row.queryType) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="queryContent" label="查询内容" min-width="320" show-overflow-tooltip />
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="scope">
              <el-button type="primary" link @click="$emit('view', scope.row)">查看结果</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-empty v-else :image-size="100" description="暂无历史记录" />
      </el-card>
    `
  };

  const app = createApp({
    components: { RecommendationResult, HistoryView },
    setup() {
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

      const textForm = reactive({ requirementText: "" });

      const userText = computed(() => {
        if (!auth.value) return "";
        const user = auth.value.user || {};
        return `用户：${user.username || "-"} | 分数：${user.score ?? "-"} | 科类：${user.subjectType || "-"} | 省份：${user.examProvince || "-"}`;
      });

      const historyHasResult = computed(() => {
        return historyGrouped.rush.length + historyGrouped.safe.length + historyGrouped.guarantee.length > 0;
      });

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
        const u = auth.value.user;
        scoreForm.score = u.score ?? "";
        scoreForm.subjectType = u.subjectType || "";
        scoreForm.province = u.examProvince || "";
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
        } catch (e) {
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
        } catch (e) {
          error.value = e.message;
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
          } catch (e) {
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
        } catch (e) {
          error.value = e.message;
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
        } catch (e) {
          error.value = e.message;
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
        } catch (e) {
          error.value = e.message;
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
          } catch (parseError) {
            parsed = null;
          }

          const groupedData = buildGroupedFromResult(parsed || {});
          historyGrouped.rush = groupedData.rush;
          historyGrouped.safe = groupedData.safe;
          historyGrouped.guarantee = groupedData.guarantee;
          historySummary.value = parsed?.summary || "";
          historyAiSummary.value = parsed?.aiSummary || "";
        } catch (e) {
          error.value = e.message;
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

      return {
        auth,
        activePage,
        activeMode,
        loading,
        error,
        resultSummary,
        aiSummary,
        grouped,
        provinces,
        historyLoading,
        historyRecords,
        historyDialogVisible,
        historyDetailLoading,
        historyDetail,
        historyResultJson,
        historyGrouped,
        historySummary,
        historyAiSummary,
        historyHasResult,
        loginForm,
        scoreForm,
        textForm,
        userText,
        SUBJECT_OPTIONS,
        queryTypeLabel,
        formatDateTime,
        login,
        logout,
        queryByScore,
        queryByText,
        loadHistory,
        openHistoryResult,
        switchPage
      };
    },
    template: `
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
                    <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
                  </el-select>
                </el-form-item>

                <el-button type="primary" class="auth-submit" :loading="loading" @click="login">登录</el-button>
                <div class="error" v-if="error">{{ error }}</div>
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
              </el-button-group>
              <span class="user-text">{{ userText }}</span>
              <el-button type="info" plain @click="logout">退出登录</el-button>
            </el-space>
          </el-header>

          <el-main class="app-main" v-if="activePage === 'recommend'">
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
                        <el-button type="primary" class="query-submit" :loading="loading" @click="queryByText">开始推荐</el-button>
                      </el-form>
                    </el-tab-pane>

                    <el-tab-pane label="分数查询" name="score">
                      <el-form label-position="top" :model="scoreForm">
                        <el-form-item label="分数">
                          <el-input v-model="scoreForm.score" type="number" placeholder="请输入高考分数" />
                        </el-form-item>
                        <el-form-item label="省份">
                          <el-select v-model="scoreForm.province" placeholder="请选择" style="width: 100%;">
                            <el-option v-for="p in provinces" :key="p" :label="p" :value="p" />
                          </el-select>
                        </el-form-item>
                        <el-form-item label="科类">
                          <el-select v-model="scoreForm.subjectType" placeholder="请选择" style="width: 100%;">
                            <el-option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :label="opt.label" :value="opt.value" />
                          </el-select>
                        </el-form-item>
                        <el-button type="primary" class="query-submit" :loading="loading" @click="queryByScore">开始推荐</el-button>
                      </el-form>
                    </el-tab-pane>
                  </el-tabs>

                  <div class="error" v-if="error">{{ error }}</div>
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

          <el-main class="app-main" v-else>
            <HistoryView
              :records="historyRecords"
              :loading="historyLoading"
              @refresh="loadHistory"
              @view="openHistoryResult"
            />
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
                  <el-descriptions-item label="查询时间">{{ formatDateTime(historyDetail.createdAt) }}</el-descriptions-item>
                  <el-descriptions-item label="查询类型">{{ queryTypeLabel(historyDetail.queryType) }}</el-descriptions-item>
                  <el-descriptions-item label="查询内容">{{ historyDetail.queryContent }}</el-descriptions-item>
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
                <pre class="history-raw">{{ historyResultJson || '暂无可展示结果' }}</pre>
              </el-card>
            </template>
          </el-skeleton>
        </el-dialog>
      </div>
    `
  });

  app.use(ElementPlus);
  app.mount("#app");
})();
