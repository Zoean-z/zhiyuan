<script setup>
import BrandLockup from "./BrandLockup.vue";

defineProps({
  title: { type: String, required: true },
  standalone: { type: Boolean, default: false }
});
</script>

<template>
  <header class="app-header" :class="{ 'app-header--standalone': standalone }">
    <div v-if="standalone" class="app-header__brand-column">
      <BrandLockup />
    </div>
    <div class="app-header__content">
      <h1>{{ title }}</h1>
      <slot />
    </div>
  </header>
</template>
