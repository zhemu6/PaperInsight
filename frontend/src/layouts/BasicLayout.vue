<script setup lang="ts">
// Components are auto-imported via unplugin-vue-components
</script>

<template>
  <el-container class="h-screen w-full">
    <!-- 顶部导航 -->
    <el-header height="60px" class="border-b border-gray-100 dark:border-gray-800 p-0">
      <BaseHeader />
    </el-header>

    <el-container class="overflow-hidden">
      <!-- 左侧边栏 -->
      <el-aside width="220px" class="border-r border-gray-100 dark:border-gray-800 bg-gray-50/50 dark:bg-gray-900/50 transition-all duration-300">
        <BaseSide />
      </el-aside>

      <!-- 主内容区 -->
      <el-container class="flex flex-col min-w-0 bg-[var(--el-bg-color-page)]">
        <el-main class="flex-1 p-6 overflow-y-auto relative scroll-smooth">
          <RouterView />
        </el-main>
        
        <!-- 底部版权 -->
        <el-footer height="40px" class="border-t border-gray-100 dark:border-gray-800">
          <BaseFooter />
        </el-footer>
      </el-container>
    </el-container>
  </el-container>
</template>

<style scoped>
/* 针对 el-main 的滚动条美化 (可选) */
:deep(.el-main)::-webkit-scrollbar {
  width: 6px;
  height: 6px;
}
:deep(.el-main)::-webkit-scrollbar-thumb {
  background: #e5e7eb;
  border-radius: 3px;
}
:deep(.el-main)::-webkit-scrollbar-track {
  background: transparent;
}
</style>
