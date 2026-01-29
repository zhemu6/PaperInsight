<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<route lang="yaml">
meta:
  layout: blank
</route>

<template>
  <div class="min-h-screen flex flex-col items-center justify-center bg-gray-50 text-gray-800">
    <h1 class="mb-4 text-6xl text-blue-600 font-bold">
      403
    </h1>
    <p class="mb-8 text-xl text-gray-600">
      抱歉，您没有权限访问该页面。
    </p>
    <el-button type="primary" size="large" @click="router.push('/')">
      返回首页
    </el-button>
  </div>
</template>
