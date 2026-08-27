<script setup>
import { computed, ref, watch } from "vue";
import { resolveSchoolLogoId, schoolNameOf } from "../utils/schoolLogoMap";

const props = defineProps({
  school: { type: Object, required: true },
  size: { type: String, default: "" }
});

const failed = ref(false);
const schoolName = computed(() => schoolNameOf(props.school));
const logoId = computed(() => resolveSchoolLogoId(props.school));

watch(
  logoId,
  () => {
    failed.value = false;
  }
);
</script>

<template>
  <span class="gk-school__logo" :class="size ? `gk-school__logo--${size}` : ''">
    <img v-if="logoId && !failed" :src="`/logos/${logoId}.jpg`" :alt="schoolName" loading="lazy" @error="failed = true" />
    <template v-else>{{ schoolName.slice(0, 1) || "校" }}</template>
  </span>
</template>
