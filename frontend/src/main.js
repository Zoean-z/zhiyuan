import { createApp } from "vue";
import ElementPlus from "element-plus";
import "element-plus/dist/index.css";
import App from "./App.vue";
import router from "./router";
import { isMockMode, setupMockInterceptor } from "./utils/mock";
import "./styles.css";

// 如果启用 Mock 模式，设置拦截器
if (isMockMode()) {
  setupMockInterceptor();
  console.log("[Mock Mode] 已启用演示模式，使用模拟数据");
}

createApp(App).use(ElementPlus).use(router).mount("#app");
