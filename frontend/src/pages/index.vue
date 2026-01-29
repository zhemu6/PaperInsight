<script setup lang="ts">
import { onMounted } from 'vue'
import { useRouter } from 'vue-router'

const router = useRouter()

onMounted(() => {
  // 重定向到论文广场
  router.replace('/PaperSquare')
})
</script>

<template>
  <div class="h-full flex items-center justify-center">
    <div class="text-center">
      <div class="i-ep-loading mb-4 animate-spin text-4xl text-gray-400" />
      <p class="text-gray-500">
        正在跳转...
      </p>
    </div>
  </div>
</template>
