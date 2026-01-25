<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { getVisitStats } from '~/api/sysUserController'

const todayCount = ref(0)
const totalCount = ref(0)

const fetchVisitStats = async () => {
  try {
    const res = await getVisitStats()
    if (res.code === 0 && res.data) {
      todayCount.value = Number(res.data.todayVisitCount) || 0
      totalCount.value = Number(res.data.totalVisitCount) || 0
    }
  } catch (error) {
    console.error('Failed to fetch visit stats:', error)
  }
}

onMounted(() => {
  fetchVisitStats()
})
</script>

<template>
  <div class="h-full flex flex-col items-center justify-center text-gray-400 text-xs tracking-wider gap-1">
    <div>© 2026 PaperInsight · 赋能科研新范式</div>
    <div class="flex gap-4 opacity-80 scale-90">
      <span>今日访客: {{ todayCount }}</span>
      <span>总访问量: {{ totalCount }}</span>
    </div>
  </div>
</template>

<style scoped>
</style>
