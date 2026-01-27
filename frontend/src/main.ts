import { createPinia } from 'pinia'
import piniaPluginPersistedstate from 'pinia-plugin-persistedstate'
import { createApp } from 'vue'
import { createRouter, createWebHistory } from 'vue-router'
import { handleHotUpdate, routes } from 'vue-router/auto-routes'
import App from './App.vue'

// 使用 tailwind-compat.css 而不是 tailwind.css，避免按钮背景色被重置为 transparent
// tailwind-compat.css 移除了按钮背景色的重置规则，兼容 Element Plus 等 UI 框架
import '@unocss/reset/tailwind-compat.css'
// 移除全量引入，恢复按需引入
// 显式引入 API 组件的样式
import 'element-plus/theme-chalk/src/message.scss'
import 'element-plus/theme-chalk/src/message-box.scss'
import 'element-plus/theme-chalk/src/notification.scss'
import './styles/index.scss'
import 'uno.css'
import 'element-plus/theme-chalk/dark/css-vars.css'

const app = createApp(App)

// 1. 配置 Pinia
const pinia = createPinia()
pinia.use(piniaPluginPersistedstate)
app.use(pinia)

// 2. 配置 Router
const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
})

if (import.meta.hot) {
  handleHotUpdate(router)
}

app.use(router)
app.mount('#app')
