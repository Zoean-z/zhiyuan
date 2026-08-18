<script setup>
import { ref, watch } from "vue";

const props = defineProps({ school: { type: Object, required: true }, size: { type: String, default: "md" } });
const failed = ref(false);

watch(() => [props.school.id, props.school.logoId], () => { failed.value = false; });
</script>

<template>
  <span class="gk-school-logo" :class="`gk-school-logo--${size}`">
    <img v-if="!failed" :src="`/logos/${school.logoId || school.id}.jpg`" :alt="school.name" loading="lazy" @error="failed = true" />
    <strong v-else>{{ school.name.slice(0, 1) }}</strong>
  </span>
</template>

<style scoped>
.gk-school-logo { width: 62px; height: 62px; flex: 0 0 auto; display: grid; place-items: center; border: 1px solid #edf0f4; border-radius: 50%; overflow: hidden; background: #fff7f0; color: #ff7a1a; }
.gk-school-logo img { width: 100%; height: 100%; object-fit: contain; padding: 4px; }
.gk-school-logo--sm { width: 42px; height: 42px; }
.gk-school-logo--lg { width: 76px; height: 76px; }
</style>
