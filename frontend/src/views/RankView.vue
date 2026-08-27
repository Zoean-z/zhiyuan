<script setup>
import GkHeader from "../components/GkHeader.vue";
import GkSidePanel from "../components/GkSidePanel.vue";
</script>

<template>
  <div class="gk-page">
    <GkHeader active="院校排行" />

    <main class="gk-home__container gk-page__main">
      <div class="gk-page__body">
        <section class="gk-page__content">
          <el-empty description="暂无可追溯的权威院校排行数据，本页不展示本地生成榜单" />
        </section>

        <GkSidePanel />
      </div>
    </main>
  </div>
</template>
