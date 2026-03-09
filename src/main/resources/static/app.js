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
    const v = (value || "").toUpperCase();
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

  const UniversityCard = {
    name: "UniversityCard",
    props: {
      item: {
        type: Object,
        required: true
      },
      strategy: {
        type: String,
        default: "safe"
      }
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
      aiSummary: {
        type: String,
        default: ""
      },
      summary: {
        type: String,
        default: ""
      }
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
    components: {
      UniversityCard,
      AiSummaryPanel
    },
    props: {
      loading: {
        type: Boolean,
        default: false
      },
      grouped: {
        type: Object,
        required: true
      },
      aiSummary: {
        type: String,
        default: ""
      },
      summary: {
        type: String,
        default: ""
      }
    },
    data() {
      return {
        activeTab: "rush"
      };
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

  const app = createApp({
    components: {
      RecommendationResult
    },
    setup() {
      const auth = ref(readStoredAuth());
      const activeMode = ref("text");
      const loading = ref(false);
      const error = ref("");
      const resultSummary = ref("");
      const aiSummary = ref("");
      const grouped = reactive({ rush: [], safe: [], guarantee: [] });
      const provinces = ref([]);

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
      }

      async function queryByScore() {
        error.value = "";
        loading.value = true;
        resetResults();
        try {
          const data = await apiFetch("/api/recommendations", {
            method: "POST",
            headers: { "Content-Type": "application/json" },
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
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ requirementText: textForm.requirementText })
          });
          const buckets = { rush: [], safe: [], guarantee: [] };
          (data?.recommendations || []).forEach((item) => {
            buckets[normalizeStrategy(item?.strategy)].push(item);
          });
          grouped.rush = dedupeByUniversity(buckets.rush);
          grouped.safe = dedupeByUniversity(buckets.safe);
          grouped.guarantee = dedupeByUniversity(buckets.guarantee);
          resultSummary.value = data?.summary || "";
          aiSummary.value = data?.aiSummary || "";
        } catch (e) {
          error.value = e.message;
        } finally {
          loading.value = false;
        }
      }

      onMounted(() => {
        loadMetaOptions();
        fillScoreFromUser();
      });

      return {
        auth,
        activeMode,
        loading,
        error,
        resultSummary,
        aiSummary,
        grouped,
        provinces,
        loginForm,
        scoreForm,
        textForm,
        userText,
        SUBJECT_OPTIONS,
        login,
        logout,
        queryByScore,
        queryByText
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
              <span class="user-text">{{ userText }}</span>
              <el-button type="info" plain @click="logout">退出登录</el-button>
            </el-space>
          </el-header>

          <el-main class="app-main">
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
        </el-container>
      </div>
    `
  });

  app.use(ElementPlus);
  app.mount("#app");
})();
