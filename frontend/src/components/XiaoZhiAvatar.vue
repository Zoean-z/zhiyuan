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
        <linearGradient id="xzBlueGradient" x1="26" y1="4" x2="70" y2="88" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#39B9EF" />
          <stop offset="1" stop-color="#0F8AD0" />
        </linearGradient>
        <linearGradient id="xzGoldGradient" x1="43" y1="48" x2="53" y2="58" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#FFE17D" />
          <stop offset="1" stop-color="#F5A623" />
        </linearGradient>
        <linearGradient id="xzStrokeGradient" x1="26" y1="4" x2="70" y2="88" gradientUnits="userSpaceOnUse">
          <stop offset="0" stop-color="#1D77B8" />
          <stop offset="1" stop-color="#0B6BA8" />
        </linearGradient>
      </defs>

      <!-- 红尾巴 -->
      <circle cx="69.5" cy="78.5" r="3.6" fill="#E02020" stroke="#B01515" stroke-width="1" />

      <!-- 身体 -->
      <ellipse cx="48" cy="62" rx="19.5" ry="16.5" fill="url(#xzBlueGradient)" stroke="url(#xzStrokeGradient)" stroke-width="1.4" />
      <!-- 大白肚皮 -->
      <ellipse cx="48" cy="64" rx="16" ry="14" fill="#FFFFFF" />

      <!-- 左手臂高举打招呼 -->
      <ellipse cx="23.5" cy="41.5" rx="5.4" ry="9.6" transform="rotate(-40 23.5 41.5)" fill="url(#xzBlueGradient)" stroke="url(#xzStrokeGradient)" stroke-width="1.4" />
      <circle cx="16.5" cy="32.5" r="5" fill="#FFFFFF" stroke="#173A5E" stroke-width="1.3" />
      <!-- 右手臂垂放 -->
      <ellipse cx="69.5" cy="57" rx="5.4" ry="8.4" transform="rotate(-30 69.5 57)" fill="url(#xzBlueGradient)" stroke="url(#xzStrokeGradient)" stroke-width="1.4" />
      <circle cx="73" cy="63.5" r="4.6" fill="#FFFFFF" stroke="#173A5E" stroke-width="1.3" />

      <!-- 脚 -->
      <ellipse cx="35.5" cy="84.5" rx="10" ry="5.4" fill="#FFFFFF" stroke="#173A5E" stroke-width="1.3" />
      <ellipse cx="60.5" cy="84.5" rx="10" ry="5.4" fill="#FFFFFF" stroke="#173A5E" stroke-width="1.3" />

      <!-- 大头 -->
      <circle cx="48" cy="25" r="22" fill="url(#xzBlueGradient)" stroke="url(#xzStrokeGradient)" stroke-width="1.4" />
      <!-- 白脸 -->
      <ellipse cx="48" cy="28.5" rx="17.5" ry="15.5" fill="#FFFFFF" />

      <!-- 左眼睁开（可眨眼） -->
      <g class="xz-avatar__eyes">
        <ellipse cx="41" cy="19.5" rx="5.2" ry="6.6" fill="#FFFFFF" stroke="#1F2329" stroke-width="1.3" />
        <circle cx="41.9" cy="21.6" r="2" fill="#1F2329" />
        <circle cx="42.6" cy="20.6" r="0.65" fill="#FFFFFF" />
      </g>
      <!-- 右眼 wink -->
      <path d="M51 21Q55.6 16.4 60.2 21" stroke="#1F2329" stroke-width="1.8" stroke-linecap="round" />

      <!-- 鼻子与鼻唇线 -->
      <circle cx="48" cy="28.8" r="3.7" fill="#E02020" stroke="#B01515" stroke-width="0.8" />
      <circle cx="46.8" cy="27.4" r="1" fill="#FFFFFF" opacity="0.85" />
      <path d="M48 32.5V35.2" stroke="#1F2329" stroke-width="1.3" stroke-linecap="round" />
      <!-- 贯穿脸颊的大笑弧线 -->
      <path d="M32 34.5Q48 45.5 64 34.5" stroke="#1F2329" stroke-width="1.8" stroke-linecap="round" />

      <!-- 胡须（与嘴线两端交叉） -->
      <g stroke="#1F2329" stroke-width="1.35" stroke-linecap="round">
        <path d="M33.5 29.5L23.5 27.5" />
        <path d="M33 33.5L22 33.5" />
        <path d="M33.5 37.5L23.5 39.5" />
        <path d="M62.5 29.5L72.5 27.5" />
        <path d="M63 33.5L74 33.5" />
        <path d="M62.5 37.5L72.5 39.5" />
      </g>

      <!-- 红项圈 -->
      <path d="M29 44.5h38a3.25 3.25 0 0 1 3.25 3.25v.5A3.25 3.25 0 0 1 67 51.5H29a3.25 3.25 0 0 1-3.25-3.25v-.5A3.25 3.25 0 0 1 29 44.5z" fill="#E02020" />
      <!-- 铃铛 -->
      <path d="M48 50.8V48.9" stroke="#8A5A00" stroke-width="1" stroke-linecap="round" />
      <circle cx="48" cy="53" r="5.3" fill="url(#xzGoldGradient)" stroke="#C77E00" stroke-width="1" />
      <path d="M43.7 51.8h8.6" stroke="#8A5A00" stroke-width="1.3" stroke-linecap="round" />
      <path d="M43.7 54.4h8.6" stroke="#8A5A00" stroke-width="1.1" stroke-linecap="round" />
      <circle cx="48" cy="56.4" r="1.25" fill="#8A5A00" />
      <circle cx="46.5" cy="50.9" r="0.7" fill="#FFFFFF" opacity="0.9" />
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
