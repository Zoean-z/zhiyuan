<script setup>
import { computed, nextTick, onMounted, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
import VolunteerSheet from "../components/VolunteerSheet.vue";
import { currentSheetCount } from "../utils/volunteerCore";
import {
  BATCHES,
  ENTRANT_TYPES,
  FIRST_SUBJECTS,
  GRADES,
  PROVINCES,
  SECOND_SUBJECTS,
  confirmProfile,
  isReady,
  percent,
  profile,
  rank,
  score,
  setFirstSubject,
  setScore,
  subjectsText,
  syncFromAuth,
  toggleSecondSubject
} from "../utils/examProfile";

/**
 * 志愿填报（重构）
 *
 * 【修复的问题】
 * 1. 「不研究几分钟看不懂这个界面的按钮都是干啥的」：
 *    原页同时存在 4 组功能重叠的按钮——表单底部 2 个 CTA、mnz-quick 4 个入口、
 *    大数据 hero 里 1 个「开始智能填报」、底部 gk-vol-entries 又 3 个入口，
 *    其中「智能填报 / 开始智能填报 / 智能志愿推荐 / 模拟填报」4 个按钮干的是同一件事。
 *    现在只保留两个主动作（模拟填报 / AI 定制方案）+ 一行辅助入口，并按“步骤”组织。
 * 2. 「上一页刚填的分数，根本没用上」：
 *    表单以前存在本页局部 ref（score/subjects/province…），一刷新或一跳转就丢；
 *    位次还是 `(720 - score) * 240` 这种拍脑袋公式。
 *    现在全部写入 examProfile（localStorage 持久化 + 全站共享），位次走统一的一分一段模型，
 *    并允许用成绩单上的真实位次手动修正。
 */

const router = useRouter();
const route = useRoute();

/* ===== 两阶段：① 填考生信息 → ② 45 个志愿位填报器 ===== */
const stage = ref("form");
const sheetRef = ref(null);
const scoreError = ref("");
const savedCount = ref(0);

const sheetProfile = computed(() => ({
  province: profile.province,
  subjects: [profile.firstSubject, ...profile.secondSubjects],
  score: score.value,
  rank: rank.value,
  batch: profile.batch,
  degreeType: profile.degreeType,
  entrantType: profile.entrantType
}));

onMounted(() => {
  syncFromAuth();
  savedCount.value = currentSheetCount();
  // 院校详情页 / 推荐页的「加入志愿填报」可直达填报器
  if (String(route.query.autostart || "") === "1" || route.query.school) {
    if (isReady.value) stage.value = "sheet";
  }
});

function goSheet() {
  if (!isReady.value) {
    scoreError.value = "请先填写 100–750 之间的高考分数，否则无法测算位次与录取概率";
    return;
  }
  scoreError.value = "";
  confirmProfile();
  stage.value = "sheet";
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function backToForm() {
  stage.value = "form";
  savedCount.value = currentSheetCount();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function onScoreInput(value) {
  setScore(value);
  if (score.value != null) scoreError.value = "";
}

function onRankInput(event) {
  const raw = event.target.value;
  profile.manualRank = raw === "" ? null : Number(raw);
}

/* AI 定制方案：把表单里的信息真的带给 AI（包括位次与百分位） */
function customizePlan() {
  if (!isReady.value) {
    scoreError.value = "请先填写高考分数，AI 需要分数和位次才能生成方案";
    return;
  }
  confirmProfile();
  const q = `我是${profile.province}${profile.entrantType === "art" ? "艺术类" : "普通类"}考生，${profile.degreeType}${profile.batch}，选科${subjectsText.value}，${score.value}分（全省位次约${rank.value}名，超过${percent.value}%考生），请生成 45 个志愿位的冲稳保涨度方案`;
  router.push({ path: "/agent", query: { q } });
}

async function goDiagnose() {
  if (!isReady.value) {
    scoreError.value = "请先填写高考分数，系统才能根据志愿表和录取概率完成防掉档诊断";
    return;
  }
  confirmProfile();
  stage.value = "sheet";
  await nextTick();
  sheetRef.value?.openDiagnosis();
  window.scrollTo({ top: 0, behavior: "smooth" });
}

function goPlans() {
  router.push({ path: "/plans" });
}

</script>

<template>
  <div class="gk-page">
    <GkHeader active="志愿填报" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <template v-if="stage === 'form'">
            <!-- 步骤条：让用户一眼看出这个页面到底要做什么 -->
            <ol class="mnz-steps">
              <li class="is-active"><b>1</b>填考生信息</li>
              <li><b>2</b>选院校专业（45 个志愿位）</li>
              <li><b>3</b>保存为志愿方案</li>
            </ol>

            <div class="mnz-form">
              <p class="mnz-form__title">① 请填写您的高考信息<em>信息会被全站复用（查大学、智能选大学、院校详情的概率）</em></p>

              <div class="mnz-form__row mnz-form__row--entrant">
                <button
                  v-for="t in ENTRANT_TYPES"
                  :key="t.key"
                  type="button"
                  class="mnz-entrant"
                  :class="{ 'is-active': profile.entrantType === t.key }"
                  @click="profile.entrantType = t.key"
                >
                  <i :class="`mnz-entrant__icon mnz-entrant__icon--${t.key}`">{{ t.key === "general" ? "学" : "艺" }}</i>
                  <span>{{ t.label }}</span>
                </button>
              </div>

              <div class="mnz-form__grid">
                <label class="mnz-field">
                  <span class="mnz-field__label">考试地区</span>
                  <el-select v-model="profile.province" size="large">
                    <el-option v-for="p in PROVINCES" :key="p" :label="p" :value="p" />
                  </el-select>
                </label>
                <label class="mnz-field">
                  <span class="mnz-field__label">所属年级</span>
                  <el-select v-model="profile.grade" size="large">
                    <el-option v-for="g in GRADES" :key="g" :label="g" :value="g" />
                  </el-select>
                </label>
                <div class="mnz-field">
                  <span class="mnz-field__label">成绩类型</span>
                  <div class="mnz-field__opts">
                    <button
                      v-for="d in ['本科', '专科']"
                      :key="d"
                      type="button"
                      class="mnz-radio"
                      :class="{ 'is-active': profile.degreeType === d }"
                      @click="profile.degreeType = d"
                    >
                      {{ d }}
                    </button>
                  </div>
                </div>
                <div class="mnz-field">
                  <span class="mnz-field__label">填报批次</span>
                  <el-select v-model="profile.batch" size="large">
                    <el-option v-for="b in BATCHES" :key="b" :label="b" :value="b" />
                  </el-select>
                </div>
              </div>

              <div class="mnz-form__row">
                <span class="mnz-form__label">首选科目</span>
                <div class="mnz-form__subjects">
                  <button
                    v-for="s in FIRST_SUBJECTS"
                    :key="s"
                    type="button"
                    class="mnz-subject"
                    :class="{ 'is-active': profile.firstSubject === s }"
                    @click="setFirstSubject(s)"
                  >
                    {{ s }}
                  </button>
                  <em class="mnz-form__hint">首选决定科类（物理类 / 历史类），一分一段表与录取数据按科类分开统计</em>
                </div>
              </div>

              <div class="mnz-form__row">
                <span class="mnz-form__label">再选科目</span>
                <div class="mnz-form__subjects">
                  <button
                    v-for="s in SECOND_SUBJECTS"
                    :key="s"
                    type="button"
                    class="mnz-subject"
                    :class="{ 'is-active': profile.secondSubjects.includes(s) }"
                    @click="toggleSecondSubject(s)"
                  >
                    {{ s }}
                  </button>
                  <em class="mnz-form__hint">已选 {{ profile.secondSubjects.length }}/2</em>
                </div>
              </div>

              <div class="mnz-form__grid mnz-form__grid--scores">
                <label class="mnz-field">
                  <span class="mnz-field__label">高考分数</span>
                  <el-input
                    :model-value="profile.score ?? ''"
                    size="large"
                    type="number"
                    min="0"
                    max="750"
                    placeholder="必填"
                    @update:model-value="onScoreInput"
                  >
                    <template #suffix>分</template>
                  </el-input>
                </label>
                <div class="mnz-field">
                  <span class="mnz-field__label">对应位次</span>
                  <div class="mnz-rank">
                    <template v-if="isReady">{{ rank.toLocaleString("en-US") }}<i>名</i></template>
                    <template v-else>—<i>先填分数</i></template>
                  </div>
                </div>
                <label class="mnz-field">
                  <span class="mnz-field__label">位次修正<em>（选填）</em></span>
                  <el-input
                    :model-value="profile.manualRank ?? ''"
                    size="large"
                    type="number"
                    placeholder="成绩单上的真实位次"
                    @input="onRankInput"
                  />
                </label>
              </div>

              <p v-if="isReady" class="mnz-form__summary">
                {{ profile.province }} · {{ profile.firstSubject }}类 · {{ subjectsText }} · {{ score }} 分
                · 位次约 <b>{{ rank.toLocaleString() }}</b> · 超过本省 <b>{{ percent }}%</b> 考生
              </p>
              <p v-if="scoreError" class="mnz-form__error">{{ scoreError }}</p>

              <!-- 只保留两个主动作，且写清楚各自干什么 -->
              <div class="mnz-form__actions">
                <button type="button" class="mnz-cta mnz-cta--solid" @click="goSheet">
                  <span>开始模拟填报</span>
                  <span class="mnz-cta__sub">自己选院校专业，填满 45 个志愿位</span>
                </button>
                <button type="button" class="mnz-cta mnz-cta--ghost" @click="customizePlan">
                  <span>AI 定制方案</span>
                  <span class="mnz-cta__sub">把上面的信息交给 AI，直接生成涨度方案</span>
                </button>
              </div>

              <div class="mnz-form__links">
                <button type="button" @click="goPlans">
                  我的志愿方案<em v-if="savedCount">（进行中 {{ savedCount }} 个志愿）</em> &gt;
                </button>
                <button type="button" @click="goDiagnose">防掉档诊断 &gt;</button>
              </div>
            </div>

          </template>

          <div v-else class="mnz-vfill">
            <div class="mnz-vfill__top">
              <button type="button" class="mnz-vfill__back" @click="backToForm">‹ 修改考生信息</button>
              <div class="mnz-vfill__heading">
                <h3>② 选院校专业·填 45 个志愿位</h3>
                <span>
                  {{ profile.province }} · {{ profile.firstSubject }}类 · {{ score }} 分 · 位次约 {{ rank.toLocaleString() }}
                  · {{ profile.batch }}
                </span>
              </div>
              <div class="mnz-vfill__ops">
                <button type="button" class="mnz-vfill__op" @click="sheetRef && sheetRef.smartFill()">一键智能填充</button>
                <button type="button" class="mnz-vfill__op mnz-vfill__op--ghost" @click="goDiagnose">防掉档诊断</button>
              </div>
            </div>
            <VolunteerSheet
              ref="sheetRef"
              :profile="sheetProfile"
              :initial-tab="String(route.query.tab || 'pick')"
              :initial-view="String(route.query.view || 'detail')"
            />
          </div>
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
