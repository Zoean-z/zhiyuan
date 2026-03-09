(function () {
  const { createApp, ref, reactive, computed, onMounted } = Vue;

  const SUBJECT_OPTIONS = [
    { value: "PHYSICS", label: "Physics" },
    { value: "HISTORY", label: "History" }
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
      const message = data?.message || "Request failed";
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
      universityName: pickValue(item, ["universityName", "schoolName", "name"]) || "Unknown University",
      cutoffScore: pickValue(item, ["cutoffScore", "cutoff", "lastYearCutoff"]),
      scoreGap: pickValue(item, ["scoreGap", "gap", "difference"]),
      admissionProbability: pickValue(item, ["admissionProbability", "probability", "chance"]),
      strategy
    };
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
          ? "Rush"
          : this.model.strategy === "guarantee"
            ? "Guarantee"
            : "Safe";
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
          <div class="meta-row"><span>Cutoff</span><strong>{{ model.cutoffScore ?? '-' }}</strong></div>
          <div class="meta-row"><span>Score Gap</span><strong>{{ model.scoreGap ?? '-' }}</strong></div>
          <div class="meta-row"><span>Probability</span><strong>{{ probabilityText }}</strong></div>
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
            <span>AI Summary</span>
            <el-tag size="small" type="info" effect="plain">Recommendation Advice</el-tag>
          </div>
        </template>
        <div v-if="displaySummary" class="summary-text">{{ displaySummary }}</div>
        <el-empty v-else description="No AI summary available yet." :image-size="90" />
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
          <h2>Recommendation Results</h2>
          <p>Review AI-generated colleges by strategy level and compare score fit quickly.</p>
        </el-card>

        <el-card class="result-main" shadow="never">
          <template #header>
            <div class="panel-title-row">
              <span>University Recommendations</span>
              <el-tag size="small" type="primary" effect="plain">Rush / Safe / Guarantee</el-tag>
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
                <el-tab-pane :label="'Rush (' + rushList.length + ')'" name="rush">
                  <div v-if="rushList.length" class="cards-grid">
                    <UniversityCard v-for="(item, idx) in rushList" :key="'rush-'+idx" :item="item" strategy="rush" />
                  </div>
                  <el-empty v-else description="No rush universities yet." :image-size="90" />
                </el-tab-pane>

                <el-tab-pane :label="'Safe (' + safeList.length + ')'" name="safe">
                  <div v-if="safeList.length" class="cards-grid">
                    <UniversityCard v-for="(item, idx) in safeList" :key="'safe-'+idx" :item="item" strategy="safe" />
                  </div>
                  <el-empty v-else description="No safe universities yet." :image-size="90" />
                </el-tab-pane>

                <el-tab-pane :label="'Guarantee (' + guaranteeList.length + ')'" name="guarantee">
                  <div v-if="guaranteeList.length" class="cards-grid">
                    <UniversityCard v-for="(item, idx) in guaranteeList" :key="'guarantee-'+idx" :item="item" strategy="guarantee" />
                  </div>
                  <el-empty v-else description="No guarantee universities yet." :image-size="90" />
                </el-tab-pane>
              </el-tabs>

              <el-empty v-if="!hasAnyData" description="No recommendation data. Submit a query to generate results." :image-size="100" />
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
        return `User: ${user.username || "-"} | Score: ${user.score ?? "-"} | Subject: ${user.subjectType || "-"} | Province: ${user.examProvince || "-"}`;
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
          grouped.rush = buckets.rush;
          grouped.safe = buckets.safe;
          grouped.guarantee = buckets.guarantee;
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
      <div v-if="!auth" class="auth-wrap">
        <div class="card">
          <h1>AI College Recommendation</h1>
          <p>Login first. You can optionally provide score and province information at login.</p>

          <div class="field">
            <label>Username</label>
            <input v-model.trim="loginForm.username" placeholder="Enter username" />
          </div>

          <div class="field">
            <label>Password</label>
            <input v-model="loginForm.password" type="password" placeholder="Enter password" />
          </div>

          <div class="grid-2">
            <div class="field">
              <label>Score (optional)</label>
              <input v-model="loginForm.score" type="number" min="0" max="750" placeholder="e.g. 620" />
            </div>
            <div class="field">
              <label>Subject (optional)</label>
              <select v-model="loginForm.subjectType">
                <option value="">Select</option>
                <option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
          </div>

          <div class="field">
            <label>Province (optional)</label>
            <select v-model="loginForm.examProvince">
              <option value="">Select</option>
              <option v-for="p in provinces" :key="p" :value="p">{{ p }}</option>
            </select>
          </div>

          <button class="btn-primary btn-block" :disabled="loading" @click="login">{{ loading ? 'Logging in...' : 'Login' }}</button>
          <p class="error" v-if="error">{{ error }}</p>
        </div>
      </div>

      <div v-else class="layout">
        <aside class="sidebar">
          <h2>Query Mode</h2>
          <button class="menu-btn" :class="{ active: activeMode === 'text' }" @click="activeMode = 'text'">Free Text</button>
          <button class="menu-btn" :class="{ active: activeMode === 'score' }" @click="activeMode = 'score'">By Score</button>
        </aside>

        <main class="main">
          <header class="topbar">
            <div class="title">AI College Planner</div>
            <div class="user-info">
              <span>{{ userText }}</span>
              <button class="btn-secondary" @click="logout">Logout</button>
            </div>
          </header>

          <section class="content">
            <div class="panel">
              <h3 v-if="activeMode === 'text'">Text Query</h3>
              <h3 v-else>Score Query</h3>

              <template v-if="activeMode === 'text'">
                <div class="field">
                  <label>Requirement</label>
                  <textarea
                    v-model.trim="textForm.requirementText"
                    placeholder="Example: Jiangsu student, score 620, computer major preference, East China, provide rush/safe/guarantee schools."
                  ></textarea>
                </div>
                <button class="btn-primary btn-block" :disabled="loading" @click="queryByText">{{ loading ? 'Loading...' : 'Generate Recommendations' }}</button>
              </template>

              <template v-else>
                <div class="field">
                  <label>Score</label>
                  <input type="number" min="0" max="750" v-model="scoreForm.score" />
                </div>
                <div class="field">
                  <label>Province</label>
                  <select v-model="scoreForm.province">
                    <option value="">Select</option>
                    <option v-for="p in provinces" :key="p" :value="p">{{ p }}</option>
                  </select>
                </div>
                <div class="field">
                  <label>Subject</label>
                  <select v-model="scoreForm.subjectType">
                    <option value="">Select</option>
                    <option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                  </select>
                </div>
                <button class="btn-primary btn-block" :disabled="loading" @click="queryByScore">{{ loading ? 'Loading...' : 'Generate Recommendations' }}</button>
              </template>

              <p class="error" v-if="error">{{ error }}</p>
            </div>

            <RecommendationResult
              :loading="loading"
              :grouped="grouped"
              :summary="resultSummary"
              :ai-summary="aiSummary"
            />
          </section>
        </main>
      </div>
    `
  });

  app.use(ElementPlus);
  app.mount("#app");
})();
