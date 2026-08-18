<script setup>
import { ArrowDown } from "@element-plus/icons-vue";
import { computed, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import { MAJORS, SCHOOLS } from "../utils/exploreData";
import { majorCutoff, probDetailOf, schoolCutoff } from "../utils/volunteerCore";
import { profile, subjectType } from "../utils/examProfile";
import { strategyOf } from "../utils/scoreModel";
import hotIcon from "../assets/gk_hot.png";

const route = useRoute();
const router = useRouter();

/* ---------- 基础数据 ---------- */
const TABS = [
  { key: "intro", label: "专业概况" },
  { key: "schools", label: "开设院校" },
  { key: "predict", label: "录取预测" },
  { key: "jobs", label: "相关就业" }
];

const SUB_TYPE = {
  计算机类: "理工", 电子信息类: "理工", 机械类: "理工", 自动化类: "理工", 土木类: "理工",
  管理科学与工程类: "理工", 生物医学工程类: "理工",
  数学类: "综合", 物理类: "综合", 化学类: "综合", 生物科学类: "综合", 心理学类: "综合",
  中国语言文学类: "综合", 外国语言文学类: "综合", 新闻传播学类: "综合",
  工商管理类: "综合", 电子商务类: "综合", 经济学类: "综合", 金融学类: "综合", 财政学类: "综合",
  法学类: "综合", 政治学类: "综合", 社会学类: "综合", 哲学类: "综合", 历史学类: "综合",
  临床医学类: "医药", 口腔医学类: "医药", 护理学类: "医药", 中医学类: "医药",
  教育学类: "师范", 体育学类: "体育", 设计学类: "艺术", 戏剧与影视学类: "艺术",
  动物医学类: "农林", 植物生产类: "农林"
};

const EMPLOY_MAP = {
  计算机类: ["前端开发", "后端开发", "算法工程师", "测试工程师", "运维工程师"],
  电子信息类: ["硬件工程师", "嵌入式开发", "芯片设计", "通信工程师"],
  机械类: ["机械设计", "智能制造", "汽车工程", "设备工程师"],
  自动化类: ["自动控制", "机器人工程", "电气工程师", "工业互联网"],
  土木类: ["结构设计", "工程造价", "施工管理", "市政规划"],
  临床医学类: ["临床医师", "医学科研", "公共卫生", "医院管理"],
  口腔医学类: ["口腔医师", "口腔诊所", "正畸方向", "医学教研"],
  护理学类: ["临床护理", "护理管理", "社区护理", "国际护理"],
  中医学类: ["中医师", "中医科研", "康复理疗", "中药研发"],
  工商管理类: ["企业管理", "市场营销", "人力资源", "财务分析"],
  经济学类: ["经济分析", "政策研究", "数据分析师", "金融顾问"],
  金融学类: ["银行从业", "证券投资", "风控合规", "基金管理"],
  法学类: ["律师", "法务专员", "公务员", "合规审查"],
  中国语言文学类: ["编辑记者", "文案策划", "语文教师", "内容运营"],
  教育学类: ["学校教师", "教育管理", "课程研发", "教育咨询"]
};

const SORTS = ["默认排序", "分数排序", "软科排序", "学科排序", "毕业生满意度排序"];
const SUBJECT_SCOPES = ["全部科类", "物理类", "历史类"];

const provinceFilter = ref("不限");
const typeFilter = ref("不限");
const natureFilter = ref("不限");
const featureFilter = ref("不限");
const sortKey = ref("默认排序");
const scope = ref("全部科类");
const page = ref(1);
const pageSize = 10;
const followed = ref(false);
const activeTab = ref("intro");

const major = computed(() => MAJORS.find((m) => m.code === String(route.params.code || "")) || null);

watch(
  () => [route.params.code, route.query.tab],
  ([, tab]) => {
    const t = TABS.find((x) => x.key === tab);
    activeTab.value = t ? t.key : "intro";
    page.value = 1;
    provinceFilter.value = "不限";
    typeFilter.value = "不限";
    natureFilter.value = "不限";
    featureFilter.value = "不限";
    sortKey.value = "默认排序";
    scope.value = "全部科类";
  },
  { immediate: true }
);

/* ---------- 派生数据（不再伪造专业录取口径） ---------- */

const popularity = computed(() =>
  major.value ? (5240000 - major.value.hot * 91837).toLocaleString("en-US") : "0"
);

/* 【修复】不再伪造“某专业在某校的最低分/开设情况”。
   专业详情页暂只展示统一院校线参考，避免用 hash 派生出看似真实的专业录取数据。 */
const baseScoreOf = (school) =>
  schoolCutoff(school, { province: profile.province, subjectType: subjectType.value }).score;

function subjectScoreOf(school) {
  return baseScoreOf(school);
}

function ratingOf(school) {
  return 92 + ((school.id * 7) % 8);
}

function satisfactionOf(school) {
  return 86 + ((school.id * 5) % 10);
}

const offeringSchools = computed(() => {
  if (!major.value) return [];
  return SCHOOLS.map((school) => ({
    ...school,
    subjectScore: subjectScoreOf(school),
    rating: ratingOf(school),
    satisfaction: satisfactionOf(school)
  }));
});

const provinces = computed(() => ["不限", ...Array.from(new Set(offeringSchools.value.map((s) => s.province)))]);
const types = computed(() => ["不限", ...Array.from(new Set(offeringSchools.value.map((s) => s.type.replace("类", ""))))]);

const FEATURE_FIELD = { 985: "is985", 211: "is211", 双一流: "isDoubleFirstClass" };

const filteredSchools = computed(() => {
  let list = offeringSchools.value.filter((s) => {
    if (provinceFilter.value !== "不限" && s.province !== provinceFilter.value) return false;
    if (typeFilter.value !== "不限" && !s.type.startsWith(typeFilter.value)) return false;
    if (natureFilter.value !== "不限" && s.nature !== natureFilter.value) return false;
    if (featureFilter.value !== "不限" && !s[FEATURE_FIELD[featureFilter.value]]) return false;
    return true;
  });
  if (sortKey.value === "分数排序") list = [...list].sort((a, b) => b.subjectScore - a.subjectScore);
  else if (sortKey.value === "软科排序") list = [...list].sort((a, b) => a.id - b.id);
  else if (sortKey.value === "学科排序") list = [...list].sort((a, b) => b.rating - a.rating);
  else if (sortKey.value === "毕业生满意度排序") list = [...list].sort((a, b) => b.satisfaction - a.satisfaction);
  else list = [...list].sort((a, b) => a.id - b.id);
  return list;
});

const pagedSchools = computed(() =>
  filteredSchools.value.slice((page.value - 1) * pageSize, page.value * pageSize)
);

function probOf(school) {
  const detail = probDetailOf(school, profile.score, { province: profile.province, subjectType: subjectType.value });
  const tag = strategyOf(detail.probability);
  return {
    p: detail.probability,
    label: detail.probability == null ? "待测" : tag.full,
    cls: tag.key
  };
}

const isHot = (school) => school.id <= 8;

function schoolTags(school) {
  const tags = ["本科", school.type, school.nature];
  if (school.is985) tags.push("985");
  if (school.is211) tags.push("211");
  if (school.isDoubleFirstClass) tags.push("双一流");
  return tags;
}

/* ---------- 交互 ---------- */
function askProb(school) {
  router.push({ path: "/agent", query: { q: `我在湖南物理类560分，被${school.name}${major.value?.name || ""}专业录取的概率有多大？` } });
}

function askPredict() {
  router.push({ path: "/agent", query: { q: `我今年高考，帮我预测${major.value?.name || ""}专业在各院校的录取概率` } });
}

function goMajor(m) {
  router.push({ path: `/majors/${m.code}` });
}

const relatedMajors = computed(() => {
  if (!major.value) return [];
  return MAJORS.filter((m) => m.code !== major.value.code && (m.sub === major.value.sub || m.category === major.value.category)).slice(0, 6);
});

const employTags = computed(() => EMPLOY_MAP[major.value?.sub] || ["国企央企", "公务员", "科研院所", "自主创业"]);

const scoreBands = computed(() =>
  filteredSchools.value.slice(0, 8).map((s) => ({
    ...s,
    prob: probOf(s),
    rankNo: schoolCutoff(s, { province: profile.province, subjectType: subjectType.value })?.minRank ?? null
  }))
);
</script>

<template>
  <div class="gk-page">
    <GkHeader active="查专业" />

    <main class="gk-home__container gk-page__main">
      <div v-if="major" class="gk-page__body gkd-body">
        <section class="gkd-main">
          <!-- 专业信息头 -->
          <div class="gkd-head">
            <div class="gkd-head__left">
              <div class="gkd-head__name">
                <h3>{{ major.name }}</h3>
                <span class="gkd-head__pop">人气值：{{ popularity }}</span>
              </div>
              <p class="gkd-head__path">
                <span v-for="seg in [major.level ? '专科(高职)' : '本科(普通)', major.category, major.sub]" :key="seg">{{ seg }}</span>
              </p>
            </div>
            <button type="button" class="gkd-head__follow" :class="{ 'is-on': followed }" @click="followed = !followed">
              {{ followed ? "已关注" : "+ 关注" }}
            </button>
          </div>

          <!-- 选项卡 -->
          <nav class="gkd-tabs">
            <button
              v-for="t in TABS"
              :key="t.key"
              type="button"
              class="gkd-tabs__item"
              :class="{ 'is-active': activeTab === t.key }"
              @click="activeTab = t.key"
            >
              {{ t.label }}
            </button>
          </nav>

          <!-- 开设院校 -->
          <div v-if="activeTab === 'schools'" class="gkd-panel">
            <div class="gkd-bar">
              <div class="gkd-bar__filters">
                <el-dropdown trigger="click" popper-class="gks-drop" @command="(v) => { provinceFilter = v; page = 1; }">
                  <button type="button" class="gkd-fsel" :class="{ 'is-set': provinceFilter !== '不限' }">
                    <span>{{ provinceFilter === "不限" ? "院校所属" : provinceFilter }}</span>
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="p in provinces" :key="p" :command="p" :class="{ 'is-active': provinceFilter === p }" class="gks-drop__opt">{{ p }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <el-dropdown trigger="click" popper-class="gks-drop" @command="(v) => { typeFilter = v; page = 1; }">
                  <button type="button" class="gkd-fsel" :class="{ 'is-set': typeFilter !== '不限' }">
                    <span>{{ typeFilter === "不限" ? "院校类型" : typeFilter + "类" }}</span>
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="t in types" :key="t" :command="t" :class="{ 'is-active': typeFilter === t }" class="gks-drop__opt">{{ t === "不限" ? "不限" : t + "类" }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <el-dropdown trigger="click" popper-class="gks-drop" @command="(v) => { natureFilter = v; page = 1; }">
                  <button type="button" class="gkd-fsel" :class="{ 'is-set': natureFilter !== '不限' }">
                    <span>{{ natureFilter === "不限" ? "办学类型" : natureFilter }}</span>
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="n in ['不限', '公办', '民办', '中外合作办学']" :key="n" :command="n" :class="{ 'is-active': natureFilter === n }" class="gks-drop__opt">{{ n }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>

                <el-dropdown trigger="click" popper-class="gks-drop" @command="(v) => { featureFilter = v; page = 1; }">
                  <button type="button" class="gkd-fsel" :class="{ 'is-set': featureFilter !== '不限' }">
                    <span>{{ featureFilter === "不限" ? "院校特色" : featureFilter }}</span>
                    <el-icon><ArrowDown /></el-icon>
                  </button>
                  <template #dropdown>
                    <el-dropdown-menu>
                      <el-dropdown-item v-for="f in ['不限', '985', '211', '双一流']" :key="f" :command="f" :class="{ 'is-active': featureFilter === f }" class="gks-drop__opt">{{ f }}</el-dropdown-item>
                    </el-dropdown-menu>
                  </template>
                </el-dropdown>
              </div>
            </div>

            <div class="gkd-sort">
              <div class="gkd-sort__list">
                <button
                  v-for="s in SORTS"
                  :key="s"
                  type="button"
                  class="gkd-sort__item"
                  :class="{ 'is-active': sortKey === s }"
                  @click="sortKey = s; page = 1;"
                >
                  {{ s }}
                </button>
              </div>
              <el-dropdown trigger="click" popper-class="gks-drop" @command="(v) => (scope = v)">
                <button type="button" class="gkd-fsel">
                  <span>2025 本科批 {{ scope }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="sc in SUBJECT_SCOPES" :key="sc" :command="sc" :class="{ 'is-active': scope === sc }" class="gks-drop__opt">2025 本科批 {{ sc }}</el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>
            </div>

            <p class="gkd-count">院校 <b>{{ filteredSchools.length }}</b> 所</p>

            <ul class="gkd-list">
              <li v-for="school in pagedSchools" :key="school.id" class="gkd-item">
                <GkSchoolLogo :school="school" />
                <div class="gkd-item__info">
                  <p class="gkd-item__name">
                    {{ school.name }}
                    <img v-if="isHot(school)" :src="hotIcon" alt="hot" class="gkd-item__hot" />
                  </p>
                  <p class="gkd-item__tags">
                    <i v-for="tag in schoolTags(school)" :key="tag">{{ tag }}</i>
                  </p>
                </div>
                <button type="button" class="gkd-prob" @click="askProb(school)">
                  <span class="gkd-prob__cap">录取概率</span>
                  <span class="gkd-prob__row">
                    <b :class="`is-${probOf(school).cls}`">{{ probOf(school).label }}</b>
                    <em :class="`is-${probOf(school).cls}`">{{ probOf(school).p == null ? "待测" : `${probOf(school).p}%` }}</em>
                  </span>
                </button>
              </li>
              <li v-if="!filteredSchools.length" class="gk-school__empty">没有符合筛选条件的院校，试试放宽条件</li>
            </ul>

            <div v-if="filteredSchools.length > pageSize" class="gkd-pager">
              <el-pagination
                layout="prev, pager, next"
                :total="filteredSchools.length"
                :page-size="pageSize"
                :current-page="page"
                background
                @current-change="(p) => (page = p)"
              />
            </div>
          </div>

          <!-- 专业概况 -->
          <div v-else-if="activeTab === 'intro'" class="gkd-panel">
            <dl class="gkd-facts">
              <div class="gkd-facts__item"><dt>修业年限</dt><dd>{{ major.duration }}</dd></div>
              <div class="gkd-facts__item"><dt>授予学位</dt><dd>{{ major.degree }}</dd></div>
              <div class="gkd-facts__item"><dt>男女比例</dt><dd>{{ major.gender }}</dd></div>
              <div class="gkd-facts__item"><dt>平均年薪</dt><dd>{{ major.salary }}</dd></div>
              <div class="gkd-facts__item"><dt>开设院校</dt><dd>{{ major.schoolCount }} 所</dd></div>
              <div class="gkd-facts__item"><dt>专业代码</dt><dd>{{ major.code }}</dd></div>
            </dl>
            <h4 class="gkd-sub">专业介绍</h4>
            <p class="gkd-text">
              {{ major.name }}专业属于{{ major.category }}门类下的{{ major.sub }}，修业年限{{ major.duration }}，毕业后授予{{ major.degree }}学位。
              该专业主要培养掌握{{ major.sub.replace("类", "") }}基础理论与专业技能、能够解决实际问题的复合型人才，
              全国目前共有 {{ major.schoolCount }} 所院校开设。从历年报考热度看，该专业人气值约 {{ popularity }}，
              在{{ major.category }}门类中热度排名靠前{{ major.hot <= 10 ? "（TOP10）" : "" }}。
            </p>
            <h4 class="gkd-sub">就业方向</h4>
            <p class="gkd-text">毕业生主要面向以下岗位与行业，供参考：</p>
            <div class="gkd-chips">
              <i v-for="t in employTags" :key="t">{{ t }}</i>
            </div>
            <button type="button" class="gkd-cta" @click="askPredict">让小智分析就业前景 &gt;</button>
          </div>

          <!-- 录取预测 -->
          <div v-else-if="activeTab === 'predict'" class="gkd-panel">
            <h4 class="gkd-sub">{{ major.name }} · 院校线参考（2025 本科批）</h4>
            <table class="gkd-table">
              <thead>
                <tr><th>院校</th><th>最低分</th><th>最低位次</th><th>录取概率</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in scoreBands" :key="row.id">
                  <td>
                    <span class="gkd-table__school">
                      <GkSchoolLogo :school="row" />
                      {{ row.name }}
                    </span>
                  </td>
                  <td>{{ row.subjectScore }}</td>
                  <td>{{ row.rankNo == null ? "—" : row.rankNo }}</td>
                  <td><span class="gkd-badge" :class="`is-${row.prob.cls}`">{{ row.prob.p == null ? "待测" : `${row.prob.label} ${row.prob.p}%` }}</span></td>
                </tr>
              </tbody>
            </table>
            <p class="gkd-note">以上为基于公开数据的参考推算，志愿填报请以省考试院公布为准。</p>
            <button type="button" class="gkd-cta" @click="askPredict">输入分数，问小智精准预测 &gt;</button>
          </div>

          <!-- 相关就业 -->
          <div v-else class="gkd-panel">
            <div class="gkd-stats">
              <div class="gkd-stats__item">
                <em>{{ major.salary.replace("¥", "") }}</em>
                <span>毕业平均年薪</span>
              </div>
              <div class="gkd-stats__item">
                <em>{{ major.gender.split(":")[0] }}% / {{ major.gender.split(":")[1] }}%</em>
                <span>男生 / 女生比例</span>
              </div>
              <div class="gkd-stats__item">
                <em>{{ major.schoolCount }}</em>
                <span>全国开设院校数</span>
              </div>
            </div>
            <h4 class="gkd-sub">主要就业去向</h4>
            <div class="gkd-chips">
              <i v-for="t in employTags" :key="t">{{ t }}</i>
            </div>
            <button type="button" class="gkd-cta" @click="askPredict">问问小智：{{ major.name }}好就业吗 &gt;</button>
          </div>
        </section>

        <aside class="gkd-side">
          <div class="gkd-related">
            <p class="gkd-related__title">相关专业推荐</p>
            <button v-for="m in relatedMajors" :key="m.code" type="button" class="gkd-related__item" @click="goMajor(m)">
              {{ m.name }}
            </button>
          </div>
          <GkSidePanel />
        </aside>
      </div>

      <div v-else class="gk-page__content gkd-empty">
        <p>未找到该专业，去 <a href="#/majors" class="gkd-link">查专业</a> 看看全部专业</p>
      </div>
    </main>
  </div>
</template>
