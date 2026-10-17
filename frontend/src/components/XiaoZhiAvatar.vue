<script setup>
defineProps({
  size: { type: String, default: "md" },
  float: { type: Boolean, default: false }
});
</script>

<template>
  <span class="xz-avatar" :class="[`xz-avatar--${size}`, { 'xz-avatar--float': float }]" role="img" aria-label="问小智">
    <svg viewBox="0 0 96 96" fill="none" xmlns="http://www.w3.org/2000/svg">
      <defs>
        <linearGradient id="xzBodyGradient" x1="24" y1="8" x2="72" y2="88" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#4FB8E8" />
          <stop offset="1" stop-color="#1A7BB8" />
        </linearGradient>
        <linearGradient id="xzBellyGradient" x1="34" y1="42" x2="62" y2="78" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#E8F6FF" />
          <stop offset="1" stop-color="#C4E8FC" />
        </linearGradient>
        <linearGradient id="xzHelmetGradient" x1="30" y1="6" x2="66" y2="40" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#5BC5F5" />
          <stop offset="1" stop-color="#1A7BB8" />
        </linearGradient>
        <linearGradient id="xzVisorGradient" x1="34" y1="16" x2="62" y2="36" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#1B2A4A" />
          <stop offset="1" stop-color="#2E5077" />
        </linearGradient>
      </defs>

      <!-- 天线 -->
      <line x1="48" y1="4" x2="48" y2="12" stroke="#F5A623" stroke-width="2.5" stroke-linecap="round" />
      <circle cx="48" cy="4" r="3" fill="#F5A623" stroke="#D4880F" stroke-width="1">
        <animate attributeName="opacity" values="1;0.5;1" dur="2.4s" repeatCount="indefinite" />
      </circle>

      <!-- 头盔（圆头） -->
      <circle cx="48" cy="27" r="21" fill="url(#xzHelmetGradient)" stroke="#1565A0" stroke-width="1.5" />
      <!-- 头盔高光 -->
      <ellipse cx="40" cy="17" rx="7" ry="5" fill="#FFFFFF" opacity="0.35" transform="rotate(-20 40 17)" />

      <!-- 面罩（深色弧形视窗） -->
      <path d="M30 25a18 18 0 0 1 36 0c0 3-1 6-2.8 8.2H32.8A17.6 17.6 0 0 1 30 25z" fill="url(#xzVisorGradient)" />
      <!-- 眼睛（视窗内的发光条） -->
      <g class="xz-avatar__eyes">
        <rect x="38" y="20" width="6.5" height="8" rx="3" fill="#7FDBFF" />
        <rect x="51" y="20" width="6.5" height="8" rx="3" fill="#7FDBFF" />
        <circle cx="41" cy="23" r="1.2" fill="#FFFFFF" />
        <circle cx="54" cy="23" r="1.2" fill="#FFFFFF" />
      </g>
      <!-- 微笑（视窗下缘） -->
      <path d="M40 30.5Q48 34 56 30.5" stroke="#7FDBFF" stroke-width="1.6" stroke-linecap="round" />

      <!-- 脖颈 -->
      <rect x="42" y="46" width="12" height="5" rx="2.5" fill="#1565A0" />

      <!-- 身体（胶囊形） -->
      <rect x="28" y="50" width="40" height="32" rx="12" fill="url(#xzBodyGradient)" stroke="#1565A0" stroke-width="1.5" />
      <!-- 胸口显示屏 -->
      <rect x="37" y="56" width="22" height="14" rx="4" fill="url(#xzBellyGradient)" stroke="#1565A0" stroke-width="1" />
      <!-- 屏幕上的对话气泡图标 -->
      <path d="M43 60h10a2 2 0 0 1 2 2v4a2 2 0 0 1-2 2h-6l-3 3v-3h-1a2 2 0 0 1-2-2v-4a2 2 0 0 1 2-2z" fill="#1565A0" opacity="0.4" />
      <!-- 屏幕高光 -->
      <line x1="39.5" y1="58" x2="39.5" y2="68" stroke="#FFFFFF" stroke-width="1.2" opacity="0.35" stroke-linecap="round" />

      <!-- 左手臂（挥手） -->
      <rect x="18" y="52" width="9" height="18" rx="4.5" fill="url(#xzBodyGradient)" stroke="#1565A0" stroke-width="1.2" transform="rotate(-35 22.5 52)" />
      <circle cx="18" cy="42" r="4.5" fill="#7FDBFF" stroke="#1565A0" stroke-width="1.2" />
      <!-- 右手臂 -->
      <rect x="69" y="52" width="9" height="18" rx="4.5" fill="url(#xzBodyGradient)" stroke="#1565A0" stroke-width="1.2" transform="rotate(35 73.5 52)" />
      <circle cx="78" cy="68" r="4.5" fill="#7FDBFF" stroke="#1565A0" stroke-width="1.2" />

      <!-- 底盘脚（磁悬浮底座） -->
      <ellipse cx="48" cy="85" rx="12" ry="4" fill="#1565A0" opacity="0.25" />
      <rect x="38" y="80" width="20" height="5" rx="2.5" fill="#1565A0" />
    </svg>
  </span>
</template>

<style scoped>
.xz-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  line-height: 0;
}

.xz-avatar svg {
  width: 100%;
  height: 100%;
  display: block;
}

.xz-avatar--sm {
  width: 28px;
  height: 28px;
}

.xz-avatar--md {
  width: 44px;
  height: 44px;
}

.xz-avatar--lg {
  width: 64px;
  height: 64px;
}

.xz-avatar--xl {
  width: 84px;
  height: 84px;
}

.xz-avatar--hero {
  width: 170px;
  height: 170px;
}

.xz-avatar--float svg {
  animation: xz-float 3.3s ease-in-out infinite alternate;
}

@keyframes xz-float {
  from {
    transform: translateY(-5px);
  }

  to {
    transform: translateY(5px);
  }
}

.xz-avatar__eyes {
  transform-origin: 41px 20px;
  animation: xz-blink 4.6s infinite;
}

@keyframes xz-blink {
  0%,
  92%,
  100% {
    transform: scaleY(1);
  }
  95%,
  97% {
    transform: scaleY(0.12);
  }
}
</style>
