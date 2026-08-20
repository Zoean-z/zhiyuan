<script setup>
import { ref, watch } from "vue";

const props = defineProps({
  school: { type: Object, required: true },
  size: { type: String, default: "" }
});

const failed = ref(false);

watch(
  () => props.school?.id,
  () => {
    failed.value = false;
  }
);
</script>

<template>
  <span class="gk-school__logo" :class="size ? `gk-school__logo--${size}` : ''">
    <img v-if="!failed" :src="`/logos/${props.school.id}.jpg`" :alt="props.school.name" loading="lazy" @error="failed = true" />
    <template v-else>{{ props.school.name.slice(0, 1) }}</template>
  </span>
</template>
