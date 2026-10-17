<script setup>
import { admissionLevel, admissionDisplayText, admissionResearchRoute } from "../utils/recommendation";
import { ArrowDown } from "@element-plus/icons-vue";
import { computed, onMounted, ref, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSchoolLogo from "../components/GkSchoolLogo.vue";
import { profile, rank, subjectType } from "../utils/examProfile";
import { isExtremelyLowProbability, probabilityDisplayValue } from "../utils/recommendation";
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

/* 就业方向映射：按专业名关键词精确匹配（优先），未命中退回学科门类默认值。
   关键词顺序即优先级，先具体后宽泛。 */
const EMPLOY_KEYWORD_MAP = [
  { keys: ["土木", "建筑", "城乡规划", "道路桥梁", "给排水", "智能建造"], jobs: ["结构设计", "施工管理", "造价咨询", "工程监理", "BIM 工程师"] },
  { keys: ["计算机", "软件", "智能科学", "数据科学", "大数据", "网络工程", "信息安全", "物联网", "数字媒体技术"], jobs: ["软件开发", "算法工程师", "系统架构", "数据分析", "网络安全"] },
  { keys: ["人工智能", "机器人", "智能装备", "智能制造"], jobs: ["算法工程师", "机器人系统集成", "智能产线规划", "自动化调试", "AI 产品"] },
  { keys: ["电子", "微电子", "集成电路", "芯片", "光电", "半导体"], jobs: ["硬件工程师", "芯片设计", "嵌入式开发", "IC 验证", "半导体工艺"] },
  { keys: ["通信", "信息工程", "广播电视"], jobs: ["通信工程师", "网络优化", "射频工程师", "运营商网络岗", "物联网开发"] },
  { keys: ["自动化", "电气", "轨道交通信号", "能源与动力", "能源工程", "电力"], jobs: ["电气工程师", "电力系统运行", "自动化调试", "新能源开发", "设备管理"] },
  { keys: ["机械", "车辆", "过程装备", "材料成型"], jobs: ["机械设计", "工艺工程师", "汽车工程", "设备运维", "质量工程师"] },
  { keys: ["水利", "水文", "港口", "航运", "船舶", "交通", "运输"], jobs: ["水利工程师", "航运管理", "交通规划", "港口物流", "项目管理"] },
  { keys: ["测绘", "地质", "采矿", "石油", "矿业", "冶金"], jobs: ["测绘工程师", "地质勘察", "矿业工程", "油气田开发", "安全工程师"] },
  { keys: ["环境", "安全工程", "环保", "资源循环"], jobs: ["环保工程师", "安全评价", "环境监测", "水处理", "EHS 管理"] },
  { keys: ["化学", "化工", "制药", "材料", "高分子", "纺织", "轻工", "食品"], jobs: ["研发工程师", "工艺工程师", "质量检验", "生产管理", "技术支持"] },
  { keys: ["生物", "生物医学", "生物工程"], jobs: ["生物研发", "医药代表", "实验技术员", "医疗器械", "质检分析"] },
  { keys: ["农学", "园艺", "植物", "植物生产", "种子", "植保"], jobs: ["农业技术", "种业研发", "园林园艺", "农资管理", "现代农业创业"] },
  { keys: ["动物", "畜牧", "兽医", "水产"], jobs: ["宠物医师", "养殖技术", "动物防疫", "饲料研发", "水产技术"] },
  { keys: ["临床", "医学影像", "麻醉", "口腔", "中医", "儿科", "精神医学"], jobs: ["临床医师", "医学影像技师", "公共卫生", "医学研究", "医院管理"] },
  { keys: ["护理", "助产"], jobs: ["临床护理", "护理管理", "社区护理", "老年照护", "医美机构"] },
  { keys: ["药学", "药物", "药事"], jobs: ["医药研发", "药品注册", "药房执业", "医药代表", "质量控制"] },
  { keys: ["公共卫生", "预防", "食品卫生", "健康管理"], jobs: ["疾控中心", "卫生监督", "健康管理机构", "食品检验", "卫生统计"] },
  { keys: ["法医", "公安技术", "侦查", "治安"], jobs: ["公安机关", "司法鉴定", "安全防范", "检察辅助", "应急管理"] },
  { keys: ["法学", "法律", "知识产权", "监狱", "政治学", "社会工作", "思政"], jobs: ["法律实务", "法务合规", "公务员", "知识产权代理", "社会工作"] },
  { keys: ["教育", "师范", "体育", "运动"], jobs: ["学校教师", "教育培训", "教育管理", "体育教练", "课程研发"] },
  { keys: ["中国语言", "汉语言", "新闻", "传播", "广告", "编辑", "网络与新媒体"], jobs: ["内容运营", "编辑记者", "文案策划", "新媒体运营", "品牌公关"] },
  { keys: ["外语", "英语", "翻译", "日语", "法语", "德语", "商务英语"], jobs: ["翻译", "外贸业务", "涉外文秘", "跨境运营", "语言培训"] },
  { keys: ["历史", "考古", "文物", "哲学", "逻辑", "宗教"], jobs: ["文博考古", "教育教学", "出版编辑", "文化研究", "公务员"] },
  { keys: ["数学", "物理", "化学", "生物科学", "天文", "地理", "地质学", "统计", "心理学"], jobs: ["科研院所", "数据分析师", "中学教师", "考研深造", "量化分析"] },
  { keys: ["经济", "金融", "财政", "税收", "保险", "投资", "精算"], jobs: ["金融分析", "银行从业", "证券投资", "保险精算", "财政税务"] },
  { keys: ["管理", "工商", "会计", "审计", "财务", "人力资源", "市场营销", "电子商务", "物流", "旅游", "酒店", "会展"], jobs: ["企业管理", "市场营销", "财务会计", "人力资源", "供应链管理"] },
  { keys: ["图书", "档案", "信息管理", "情报"], jobs: ["图书档案", "信息管理", "数据分析", "行政文秘", "知识管理"] },
  { keys: ["音乐", "舞蹈", "表演", "戏剧", "影视", "播音", "美术", "设计", "艺术", "书法", "动画", "漫画", "陶瓷", "工艺"], jobs: ["艺术设计", "教育培训", "影视传媒", "自由创作", "文化机构"] },
  { keys: ["建筑学", "风景园林", "城乡"], jobs: ["建筑设计", "城市规划", "景观设计", "室内设计", "地产开发"] }
];

const EMPLOY_MAP = {
  工学: ["工程技术人员", "研发工程师", "生产管理", "质量工程", "技术支持"],
  理学: ["科研院所", "数据分析", "教师", "考研深造", "交叉学科"],
  医学: ["临床医师", "医学科研", "公共卫生", "医院管理", "医药研发"],
  文学: ["编辑记者", "文案策划", "教师", "翻译", "内容运营"],
  法学: ["律师", "法务专员", "公务员", "合规审查"],
  经济学: ["经济分析", "金融顾问", "数据分析师", "银行从业"],
  管理学: ["企业管理", "市场营销", "人力资源", "财务分析"],
  教育学: ["学校教师", "教育管理", "课程研发", "教育咨询"],
  历史学: ["文博考古", "历史教师", "档案管理", "文化研究"],
  农学: ["农业技术", "种业研发", "园林园艺", "现代农业", "农资管理"],
  艺术学: ["艺术设计", "教育培训", "影视传媒", "自由创作", "文化机构"],
  军事学: ["军队院校任职", "国防科研", "军工单位", "应急管理", "公务员"],
  专科: ["技术技能岗", "产线技术员", "企业运营", "销售与客服", "自主创业"]
};

function employTagsOf(major) {
  if (!major) return ["国企央企", "公务员", "科研院所", "自主创业"];
  const name = major.name || "";
  for (const group of EMPLOY_KEYWORD_MAP) {
    if (group.keys.some((k) => name.includes(k))) {
      return group.jobs;
    }
  }
  return EMPLOY_MAP[major.category] || ["国企央企", "公务员", "科研院所", "自主创业"];
}

const SORTS = ["默认排序", "分数排序"];
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

const majors = ref([]);
const major = ref(null);
const offeringSchools = ref([]);
const loading = ref(true);

/* 院校线参考的年份标签：取实际数据行里的最新年份，不再写死（L-05 同族：绑定值要能回答数据从哪来） */
const cutoffYearLabel = computed(() => {
  const years = [...new Set(offeringSchools.value.map((s) => s.admissionYear).filter(Boolean))];
  return years.length ? `${Math.max(...years)} 本科批` : "最新年份";
});

async function load(id) {
  loading.value = true;
  if (!majors.value.length) {
    try {
      const res = await fetch("/api/majors");
      const data = await res.json();
      majors.value = data.majors || [];
    } catch (e) {
      console.error("加载专业目录失败", e);
    }
  }
  // 优先按 id 匹配；id 为非数字（如 URL 编码的专业名）时按 name 兜底
  const isNumericId = /^\d+$/.test(String(id));
  major.value = isNumericId
    ? (majors.value.find((m) => String(m.id) === String(id)) || null)
    : (majors.value.find((m) => decodeURIComponent(String(id)) === m.name) || null);
  // 默认列表已过滤冷门专业；未命中（直达冷门专业链接）时拉全量目录兜底
  if (!major.value) {
    try {
      const res = await fetch("/api/majors?all=true");
      const data = await res.json();
      majors.value = data.majors || [];
      major.value = isNumericId
        ? (majors.value.find((m) => String(m.id) === String(id)) || null)
        : (majors.value.find((m) => decodeURIComponent(String(id)) === m.name) || null);
    } catch (e) {
      console.error("加载全量专业目录失败", e);
    }
  }
  offeringSchools.value = [];
  if (major.value) {
    try {
      const params = new URLSearchParams();
      if (profile.province) params.set("province", profile.province);
      if (subjectType.value) params.set("subjectType", subjectType.value);
      if (profile.score) params.set("score", profile.score);
      if (rank.value) params.set("userRank", rank.value);
      const qs = params.toString();
      const res = await fetch(`/api/majors/${major.value.id}/schools${qs ? `?${qs}` : ""}`);
      const list = (await res.json()) || [];
      // 校徽组件按 school.id/school.name 取图，补别名
      offeringSchools.value = list.map((s) => ({ ...s, id: s.universityId, name: s.universityName }));
    } catch (e) {
      console.error("加载开设院校失败", e);
      offeringSchools.value = [];
    }
  }
  loading.value = false;
}

watch(
  () => [route.params.code, route.query.tab, profile.province, subjectType.value, profile.score, rank.value],
  async ([code, tab]) => {
    const t = TABS.find((x) => x.key === tab);
    activeTab.value = t ? t.key : "intro";
    page.value = 1;
    provinceFilter.value = "不限";
    typeFilter.value = "不限";
    natureFilter.value = "不限";
    featureFilter.value = "不限";
    sortKey.value = "默认排序";
    scope.value = "全部科类";
    await load(code);
  },
  { immediate: true }
);

/* ---------- 派生数据 ---------- */

const popularity = computed(() =>
  major.value ? (major.value.openSchoolCount || 0).toLocaleString("en-US") : "0"
);

const offeringCount = computed(() => offeringSchools.value.length || major.value?.openSchoolCount || 0);

const cutoffDataNote = computed(() => {
  if (!offeringSchools.value.length) {
    return "当前省份和科类暂无录取数据，未使用其他省份数据兜底。";
  }
  if (offeringSchools.value.some((school) => school.dataKind === "SIMULATED")) {
    return "以上为已入库的比赛验证数据，仅用于功能验证；志愿填报请以省考试院公布为准。";
  }
  return "以上为后端已入库录取数据；志愿填报请以省考试院公布为准。";
});

const provinces = computed(() => ["不限", ...Array.from(new Set(offeringSchools.value.map((s) => s.province)))]);
const types = computed(() => ["不限", ...Array.from(new Set(offeringSchools.value.map((s) => (s.schoolType || "").replace("类", ""))))]);

const filteredSchools = computed(() => {
  let list = offeringSchools.value.filter((s) => {
    if (provinceFilter.value !== "不限" && s.province !== provinceFilter.value) return false;
    if (typeFilter.value !== "不限" && !(s.schoolType || "").includes(typeFilter.value)) return false;
    if (natureFilter.value !== "不限" && s.nature !== natureFilter.value) return false;
    if (featureFilter.value !== "不限") {
      if (featureFilter.value === "985" && !s.is985) return false;
      if (featureFilter.value === "双一流" && !s.is985 && !s.is211) return false;
    }
    return true;
  });
  if (sortKey.value === "分数排序") {
    list = [...list].sort((a, b) => (b.cutoffScore ?? -1) - (a.cutoffScore ?? -1));
  } else {
    list = [...list].sort((a, b) => a.universityId - b.universityId);
  }
  return list;
});

const pagedSchools = computed(() =>
  filteredSchools.value.slice((page.value - 1) * pageSize, page.value * pageSize)
);

function probOf(school) {
  const detail = school.probability;
  const p = probabilityDisplayValue(detail);
  if (isExtremelyLowProbability(detail)) {
    return { p: 0, label: "概率极低", cls: "unknown" };
  }
  const key = String(detail?.strategy || "").toUpperCase();
  const tag = {
    RUSH: { key: "冲", full: detail?.strategyLabel || "冲刺" },
    SAFE: { key: "稳", full: detail?.strategyLabel || "稳妥" },
    GUARANTEE: { key: "保", full: detail?.strategyLabel || "保底" }
  }[key] || { key: "unknown", full: "待测" };
  return {
    p,
    label: p == null ? "待测" : tag.full,
    cls: tag.key
  };
}

const isHot = (school) => Boolean(school.is985 || school.is211);

function schoolTags(school) {
  const tags = [school.schoolType || "本科"];
  if (school.nature) tags.push(school.nature);
  if (school.is985) tags.push("985");
  if (school.is211 && !school.is985) tags.push("211");
  return tags;
}

/* ---------- 交互 ---------- */
function askProb(school) {
  router.push({
    path: "/agent",
    query: { q: `我在${profile.province || "湖南"}${subjectType.value || "物理"}类${profile.score || ""}分，被${school.universityName}${major.value?.name || ""}专业录取的概率有多大？` }
  });
}

function askPredict() {
  router.push({ path: "/agent", query: { q: `我今年高考，帮我预测${major.value?.name || ""}专业在各院校的录取概率` } });
}

function goMajor(m) {
  router.push({ path: `/majors/${m.id}` });
}

const relatedMajors = computed(() => {
  if (!major.value) return [];
  // 同门类下按开设院校数降序 = 热门优先，避免推荐极冷门专业
  return majors.value
    .filter((m) => m.id !== major.value.id && m.category === major.value.category)
    .sort((a, b) => (b.openSchoolCount || 0) - (a.openSchoolCount || 0))
    .slice(0, 6);
});

const employTags = computed(() => employTagsOf(major.value));

const scoreBands = computed(() =>
  filteredSchools.value.slice(0, 8).map((s) => ({ ...s, prob: probOf(s) }))
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
                <span class="gkd-head__pop">开设院校：{{ popularity }} 所</span>
              </div>
              <p class="gkd-head__path">
                <span v-for="seg in ['本科(普通)', major.category]" :key="seg">{{ seg }}</span>
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
                      <el-dropdown-item v-for="f in ['不限', '985', '双一流']" :key="f" :command="f" :class="{ 'is-active': featureFilter === f }" class="gks-drop__opt">{{ f }}</el-dropdown-item>
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
                  <span>{{ cutoffYearLabel }} {{ scope }}</span>
                  <el-icon><ArrowDown /></el-icon>
                </button>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item v-for="sc in SUBJECT_SCOPES" :key="sc" :command="sc" :class="{ 'is-active': scope === sc }" class="gks-drop__opt">{{ cutoffYearLabel }} {{ sc }}</el-dropdown-item>
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
                    {{ school.universityName }}
                    <img v-if="isHot(school)" :src="hotIcon" alt="hot" class="gkd-item__hot" />
                  </p>
                  <p class="gkd-item__tags">
                    <i v-for="tag in schoolTags(school)" :key="tag">{{ tag }}</i>
                  </p>
                </div>
                <button type="button" class="gkd-prob" @click="router.push(admissionResearchRoute(school.universityId || school.id, school.universityName, major?.name))">AI 录取分析</button>
                <button type="button" class="gkd-prob" @click="askProb(school)">
                  <span class="gkd-prob__cap">机会等级</span>
                  <span class="gkd-prob__row">
                    <em :class="`is-${probOf(school).cls}`">{{ probOf(school).p == null ? "待测" : admissionLevel(probOf(school).p) }}</em>
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
              <div class="gkd-facts__item"><dt>学科门类</dt><dd>{{ major.category }}</dd></div>
              <div class="gkd-facts__item"><dt>授予学位</dt><dd>{{ major.degreeType || "—" }}</dd></div>
              <div class="gkd-facts__item"><dt>选科要求</dt><dd>{{ major.subjectRequirement || "不限" }}</dd></div>
              <div class="gkd-facts__item"><dt>开设院校</dt><dd>{{ major.openSchoolCount || 0 }} 所</dd></div>
              <div class="gkd-facts__item"><dt>专业代码</dt><dd>{{ major.id }}</dd></div>
            </dl>
            <h4 class="gkd-sub">专业介绍</h4>
            <p class="gkd-text">
              {{ major.description || `${major.name}专业属于${major.category}门类，毕业后授予${major.degreeType || "相应"}学位。` }}
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
            <h4 class="gkd-sub">{{ major.name }} · 院校线参考（{{ cutoffYearLabel }}）</h4>
            <table class="gkd-table">
              <thead>
                <tr><th>院校</th><th>最低分</th><th>最低位次</th><th>机会等级</th></tr>
              </thead>
              <tbody>
                <tr v-for="row in scoreBands" :key="row.universityId">
                  <td>
                    <span class="gkd-table__school">
                      <GkSchoolLogo :school="row" />
                      {{ row.universityName }}
                    </span>
                  </td>
                  <td>{{ row.cutoffScore == null ? "—" : row.cutoffScore }}</td>
                  <td>{{ row.minRank == null ? "—" : row.minRank }}</td>
                  <td><span class="gkd-badge" :class="`is-${row.prob.cls}`">{{ row.prob.p == null ? "待测" : admissionLevel(row.prob.p) }}</span></td>
                </tr>
              </tbody>
            </table>
            <p class="gkd-note">{{ cutoffDataNote }}</p>
            <button type="button" class="gkd-cta" @click="askPredict">输入分数，问小智精准预测 &gt;</button>
          </div>

          <!-- 相关就业 -->
          <div v-else class="gkd-panel">
            <div class="gkd-stats">
              <div class="gkd-stats__item">
                <em>{{ offeringCount }}</em>
                <span>全国开设院校数</span>
              </div>
              <div class="gkd-stats__item">
                <em>{{ major.category }}</em>
                <span>学科门类</span>
              </div>
              <div class="gkd-stats__item">
                <em>{{ major.subjectRequirement || "不限" }}</em>
                <span>选科要求</span>
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
            <button v-for="m in relatedMajors" :key="m.id" type="button" class="gkd-related__item" @click="goMajor(m)">
              {{ m.name }}
            </button>
          </div>
        </aside>
      </div>

      <div v-else class="gk-page__content gkd-empty">
        <p>未找到该专业，去 <a href="#/majors" class="gkd-link">查专业</a> 看看全部专业</p>
      </div>
    </main>
  </div>
</template>
