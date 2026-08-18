<script setup>
import { computed, ref, watch } from "vue";
import { ELECTIVE_SUBJECT_OPTIONS, SUBJECT_OPTIONS } from "../utils/recommendation.js";

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  loading: { type: Boolean, default: false },
  school: { type: Object, default: null },
  majors: { type: Array, default: () => [] },
  profile: { type: Object, default: null }
});
const emit = defineEmits(["update:modelValue", "add-selected"]);
const selectedMajorKeys = ref([]);
const activeGroupCode = ref("");

const visible = computed({ get: () => props.modelValue, set: (value) => emit("update:modelValue", value) });
const schoolTags = computed(() => Array.isArray(props.school?.schoolTags) ? props.school.schoolTags.filter(Boolean) : []);
const subjectLabels = new Map([...SUBJECT_OPTIONS, ...ELECTIVE_SUBJECT_OPTIONS].map((item) => [item.value, item.label]));
function splitElectiveSubjects(value) {
  return String(value || "").split(",").map((item) => item.trim()).filter(Boolean);
}
function formatSubjectRequirements(primarySubject, electiveSubjects) {
  const primary = subjectLabels.get(primarySubject) || primarySubject;
  const elective = electiveSubjects.map((item) => subjectLabels.get(item) || item).join("或");
  return elective ? `首选${primary}，再选${elective}` : `首选${primary}`;
}
function isGroupEligible(primarySubject, electiveSubjects) {
  if (!props.profile || props.profile.subjectType !== primarySubject) return false;
  const selected = new Set(props.profile.electiveSubjects || []);
  return !electiveSubjects.length || electiveSubjects.some((subject) => selected.has(subject));
}
const professionalGroups = computed(() => {
  const grouped = new Map();
  (props.majors || []).forEach((major) => {
    const code = String(major?.professionalGroupCode || "").trim();
    if (!code) return;
    if (!grouped.has(code)) {
      const electiveSubjects = splitElectiveSubjects(major.electiveSubjects);
      grouped.set(code, {
        code,
        name: major.professionalGroupName || "院校专业组",
        primarySubject: major.primarySubject,
        electiveSubjects,
        subjectRequirements: formatSubjectRequirements(major.primarySubject, electiveSubjects),
        eligible: isGroupEligible(major.primarySubject, electiveSubjects),
        majors: []
      });
    }
    const group = grouped.get(code);
    group.majors.push({
      ...major,
      professionalGroupCode: group.code,
      professionalGroupName: group.name,
      subjectRequirements: group.subjectRequirements
    });
  });
  return [...grouped.values()];
});
const activeGroup = computed(() => professionalGroups.value.find((group) => group.code === activeGroupCode.value) || professionalGroups.value[0] || null);
const displayedMajors = computed(() => activeGroup.value?.majors || props.majors || []);
const allSelectableMajors = computed(() => professionalGroups.value.length
  ? professionalGroups.value.filter((group) => group.eligible).flatMap((group) => group.majors)
  : props.majors || []);
const selectedMajors = computed(() => {
  const keys = new Set(selectedMajorKeys.value);
  return allSelectableMajors.value.filter((item) => keys.has(buildMajorKey(item)));
});

function buildMajorKey(item) {
  return `${item?.professionalGroupCode || "flat"}::${String(item?.majorName || "").trim().toLowerCase()}`;
}
function isMajorSelected(item) { return selectedMajorKeys.value.includes(buildMajorKey(item)); }
function toggleMajor(item) {
  if ((activeGroup.value && !activeGroup.value.eligible) || !item?.majorName) return;
  const key = buildMajorKey(item);
  selectedMajorKeys.value = selectedMajorKeys.value.includes(key)
    ? selectedMajorKeys.value.filter((value) => value !== key)
    : [...selectedMajorKeys.value, key];
}
function resetDrawerState() {
  selectedMajorKeys.value = [];
  activeGroupCode.value = professionalGroups.value[0]?.code || "";
}
function handleAddSelected() { emit("add-selected", selectedMajors.value); }

watch(() => [props.school?.universityId, props.profile?.subjectType, ...(props.profile?.electiveSubjects || [])], resetDrawerState);
watch(professionalGroups, (groups) => {
  if (groups.length && !groups.some((group) => group.code === activeGroupCode.value)) activeGroupCode.value = groups[0].code;
}, { immediate: true });
watch(() => props.modelValue, (value) => { if (!value) resetDrawerState(); });
</script>

<template>
  <el-drawer v-model="visible" title="院校专业组" size="min(900px, 92vw)" destroy-on-close class="school-detail-drawer">
    <div v-if="school" v-loading="loading" class="school-detail-drawer__body">
      <div class="school-detail-drawer__hero">
        <div class="school-detail-drawer__title-row">
          <div>
            <h3 class="school-detail-drawer__name">{{ school.universityName || "-" }}</h3>
            <div class="school-detail-drawer__province">{{ school.universityProvince || "-" }} · {{ school.universityTier || "普通本科" }}</div>
          </div>
          <div v-if="schoolTags.length" class="school-detail-drawer__tags">
            <el-tag v-for="tag in schoolTags" :key="tag" size="small" effect="plain">{{ tag }}</el-tag>
          </div>
        </div>
        <div v-if="school.introduction" class="school-detail-drawer__intro">{{ school.introduction }}</div>
      </div>

      <template v-if="professionalGroups.length">
        <div class="professional-group-switch" role="tablist" aria-label="院校专业组">
          <button v-for="group in professionalGroups" :key="group.code" type="button" role="tab"
            :aria-selected="activeGroup?.code === group.code"
            :class="['professional-group-tab', { 'is-active': activeGroup?.code === group.code, 'is-ineligible': !group.eligible }]"
            @click="activeGroupCode = group.code">
            <strong>[{{ group.code }}]</strong>
            <span>{{ group.name }}</span>
            <small>{{ group.eligible ? "选科符合" : "选科不符合" }}</small>
          </button>
        </div>
      </template>

      <div class="school-detail-drawer__section">
        <div class="school-detail-drawer__section-head">
          <div>
            <strong>{{ activeGroup ? `[${activeGroup.code}] ${activeGroup.name}` : "专业列表" }}</strong>
            <p v-if="activeGroup">{{ activeGroup.subjectRequirements }}</p>
          </div>
          <span class="school-detail-drawer__count">已选 {{ selectedMajorKeys.length }} 个专业</span>
        </div>
        <div v-if="activeGroup && !activeGroup.eligible" class="professional-group-warning">你的选科不符合该专业组要求，可以查看，但不能加入志愿单。</div>
        <div v-if="displayedMajors.length" class="school-detail-drawer__major-list">
          <button v-for="major in displayedMajors" :key="buildMajorKey(major)" type="button"
            class="school-detail-drawer__major-item"
            :class="{ 'is-selected': isMajorSelected(major), 'is-disabled': activeGroup && !activeGroup.eligible }"
            :disabled="activeGroup && !activeGroup.eligible" @click="toggleMajor(major)">
            <div class="school-detail-drawer__major-main">
              <div class="school-detail-drawer__major-name">{{ major.majorName || "-" }}</div>
              <div class="school-detail-drawer__major-meta">
                <span>最低分 {{ major.cutoffScore ?? "-" }}</span><span>最低位次 {{ major.minRank ?? "-" }}</span>
              </div>
            </div>
            <el-checkbox :model-value="isMajorSelected(major)" :disabled="activeGroup && !activeGroup.eligible" @click.stop @change="toggleMajor(major)" />
          </button>
        </div>
        <el-empty v-else :image-size="90" description="暂无专业录取数据" />
      </div>
    </div>
    <template #footer>
      <div class="school-detail-drawer__footer">
        <span class="school-detail-drawer__footer-text">已选 {{ selectedMajorKeys.length }} 个专业</span>
        <div><el-button @click="visible = false">取消</el-button><el-button type="primary" :disabled="!selectedMajorKeys.length" @click="handleAddSelected">加入志愿单</el-button></div>
      </div>
    </template>
  </el-drawer>
</template>
