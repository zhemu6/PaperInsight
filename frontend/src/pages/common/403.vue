<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<route lang="yaml">
meta:
  layout: blank
</route>

<template>
  <div class="flex flex-col items-center justify-center min-h-screen bg-gray-50 text-gray-800">
    <h1 class="text-6xl font-bold text-blue-600 mb-4">403</h1>
    <p class="text-xl mb-8 text-gray-600">抱歉，您没有权限访问该页面。</p>
    <el-button type="primary" size="large" @click="router.push('/')">
      返回首页
    </el-button>
  </div>
</template>
