<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'
// @ts-ignore
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

import BasicLayout from '~/layouts/BasicLayout.vue'
import BlankLayout from '~/layouts/BlankLayout.vue'

const route = useRoute()

// Map string keys to components
const layouts: Record<string, any> = {
  basic: BasicLayout,
  blank: BlankLayout
}

// Determine layout based on route
const layout = computed(() => {
  const layoutName = (route.meta.layout as string) || 'basic'
  return layouts[layoutName] || BasicLayout
})
</script>

<template>
  <el-config-provider namespace="ep" :locale="zhCn">
    <component :is="layout">
      <RouterView />
    </component>
  </el-config-provider>
</template>

<style>
/* Global Styles */
html, body, #app {
  height: 100%;
  margin: 0;
  padding: 0;
  font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Fira Sans', 'Droid Sans', 'Helvetica Neue', sans-serif;
  color: var(--ep-text-color-primary);
}
</style>
