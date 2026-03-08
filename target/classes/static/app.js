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

  createApp({
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
        return `用户: ${user.username || "-"} | 分数: ${user.score ?? "-"} | 科类: ${user.subjectType || "-"} | 生源地: ${user.examProvince || "-"}`;
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
          grouped.rush = data.rush || [];
          grouped.safe = data.safe || [];
          grouped.guarantee = data.guarantee || [];
          resultSummary.value = data.summary || "";
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
          (data.recommendations || []).forEach((item) => {
            buckets[normalizeStrategy(item.strategy)].push(item);
          });
          grouped.rush = buckets.rush;
          grouped.safe = buckets.safe;
          grouped.guarantee = buckets.guarantee;
          resultSummary.value = data.summary || "";
          aiSummary.value = data.aiSummary || "";
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
          <h1>志愿推荐系统</h1>
          <p>请先登录，首次登录可补充分数、科类和生源地。</p>
          <div class="field">
            <label>用户名</label>
            <input v-model.trim="loginForm.username" placeholder="请输入用户名" />
          </div>
          <div class="field">
            <label>密码</label>
            <input v-model="loginForm.password" type="password" placeholder="请输入密码" />
          </div>
          <div class="grid-2">
            <div class="field">
              <label>分数(可选)</label>
              <input v-model="loginForm.score" type="number" min="0" max="750" placeholder="如 620" />
            </div>
            <div class="field">
              <label>科类(可选)</label>
              <select v-model="loginForm.subjectType">
                <option value="">请选择</option>
                <option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
              </select>
            </div>
          </div>
          <div class="field">
            <label>生源地(可选)</label>
            <select v-model="loginForm.examProvince">
              <option value="">请选择</option>
              <option v-for="p in provinces" :key="p" :value="p">{{ p }}</option>
            </select>
          </div>
          <button class="btn-primary btn-block" :disabled="loading" @click="login">{{ loading ? '登录中...' : '登录' }}</button>
          <p class="error" v-if="error">{{ error }}</p>
        </div>
      </div>

      <div v-else class="layout">
        <aside class="sidebar">
          <h2>查询模式</h2>
          <button class="menu-btn" :class="{ active: activeMode === 'text' }" @click="activeMode = 'text'">文本查询</button>
          <button class="menu-btn" :class="{ active: activeMode === 'score' }" @click="activeMode = 'score'">分数查询</button>
        </aside>

        <main class="main">
          <header class="topbar">
            <div class="title">高考志愿推荐中心</div>
            <div class="user-info">
              <span>{{ userText }}</span>
              <button class="btn-secondary" @click="logout">退出登录</button>
            </div>
          </header>

          <section class="content">
            <div class="panel">
              <h3 v-if="activeMode === 'text'">文本查询</h3>
              <h3 v-else>分数查询</h3>

              <template v-if="activeMode === 'text'">
                <div class="field">
                  <label>需求描述</label>
                  <textarea v-model.trim="textForm.requirementText" placeholder="例如：我是江苏考生，620分，偏好计算机，想去华东地区，学校层次希望冲稳保各给几所。"></textarea>
                </div>
                <button class="btn-primary btn-block" :disabled="loading" @click="queryByText">{{ loading ? '查询中...' : '开始文本查询' }}</button>
              </template>

              <template v-else>
                <div class="field">
                  <label>分数</label>
                  <input type="number" min="0" max="750" v-model="scoreForm.score" />
                </div>
                <div class="field">
                  <label>省份</label>
                  <select v-model="scoreForm.province">
                    <option value="">请选择</option>
                    <option v-for="p in provinces" :key="p" :value="p">{{ p }}</option>
                  </select>
                </div>
                <div class="field">
                  <label>科类</label>
                  <select v-model="scoreForm.subjectType">
                    <option value="">请选择</option>
                    <option v-for="opt in SUBJECT_OPTIONS" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                  </select>
                </div>
                <button class="btn-primary btn-block" :disabled="loading" @click="queryByScore">{{ loading ? '查询中...' : '开始分数查询' }}</button>
              </template>

              <p class="error" v-if="error">{{ error }}</p>
            </div>

            <div class="panel">
              <h3>学校推荐结果</h3>
              <div v-if="activeMode === 'text' && aiSummary" class="summary-box">{{ aiSummary }}</div>
              <div v-else-if="resultSummary" class="summary-box">{{ resultSummary }}</div>

              <div class="cards">
                <div class="school-col rush">
                  <div class="school-head">冲刺学校</div>
                  <div class="school-body">
                    <div v-if="!grouped.rush.length" class="meta">暂无数据</div>
                    <div class="item" v-for="(item, idx) in grouped.rush" :key="'r' + idx">
                      <h4>{{ item.universityName }}</h4>
                      <div class="meta">录取线: {{ item.cutoffScore ?? '-' }}</div>
                      <div class="meta">分差: {{ item.scoreGap ?? '-' }}</div>
                      <div class="meta">概率: {{ item.admissionProbability ?? '-' }}%</div>
                      <div class="meta">{{ item.explanation || '' }}</div>
                    </div>
                  </div>
                </div>

                <div class="school-col safe">
                  <div class="school-head">稳妥学校</div>
                  <div class="school-body">
                    <div v-if="!grouped.safe.length" class="meta">暂无数据</div>
                    <div class="item" v-for="(item, idx) in grouped.safe" :key="'s' + idx">
                      <h4>{{ item.universityName }}</h4>
                      <div class="meta">录取线: {{ item.cutoffScore ?? '-' }}</div>
                      <div class="meta">分差: {{ item.scoreGap ?? '-' }}</div>
                      <div class="meta">概率: {{ item.admissionProbability ?? '-' }}%</div>
                      <div class="meta">{{ item.explanation || '' }}</div>
                    </div>
                  </div>
                </div>

                <div class="school-col guarantee">
                  <div class="school-head">保底学校</div>
                  <div class="school-body">
                    <div v-if="!grouped.guarantee.length" class="meta">暂无数据</div>
                    <div class="item" v-for="(item, idx) in grouped.guarantee" :key="'g' + idx">
                      <h4>{{ item.universityName }}</h4>
                      <div class="meta">录取线: {{ item.cutoffScore ?? '-' }}</div>
                      <div class="meta">分差: {{ item.scoreGap ?? '-' }}</div>
                      <div class="meta">概率: {{ item.admissionProbability ?? '-' }}%</div>
                      <div class="meta">{{ item.explanation || '' }}</div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </section>
        </main>
      </div>
    `
  }).mount("#app");
})();
