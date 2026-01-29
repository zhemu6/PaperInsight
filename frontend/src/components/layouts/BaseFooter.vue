<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { getVisitStats } from '~/api/sysUserController'

const todayCount = ref(0)
const totalCount = ref(0)

async function fetchVisitStats() {
  try {
    const res = await getVisitStats()
    if (res.code === 0 && res.data) {
      todayCount.value = Number(res.data.todayVisitCount) || 0
      totalCount.value = Number(res.data.totalVisitCount) || 0
    }
  }
  catch (error) {
    console.error('Failed to fetch visit stats:', error)
  }
}

onMounted(() => {
  fetchVisitStats()
})
</script>

<template>
  <div class="h-full flex flex-col items-center justify-center gap-1 text-xs text-gray-400 tracking-wider">
    <div>© 2026 PaperInsight · {{ $t('home.copyright') }}</div>
    <div class="flex scale-90 gap-4 opacity-80">
      <span>{{ $t('home.todayVisitors') }}: {{ todayCount }}</span>
      <span>{{ $t('home.totalVisitors') }}: {{ totalCount }}</span>
    </div>
  </div>
</template>

<style scoped>
</style>
