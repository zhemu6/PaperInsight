<script lang="ts" setup>
import {
  Document,
  Location,
  Setting,
} from '@element-plus/icons-vue'
</script>

<template>
  <el-menu
    router
    :default-active="$route.path"
    class="el-menu-vertical-demo h-full border-r-0"
  >
    <el-menu-item index="/PaperSquare">
      <el-icon><Location /></el-icon>
      <template #title>{{ $t('nav.paperSquare') }}</template>
    </el-menu-item>

    <el-menu-item index="/file">
      <el-icon><Document /></el-icon>
      <template #title>{{ $t('nav.myPapers') }}</template>
    </el-menu-item>

    <el-menu-item index="/user/profile">
      <el-icon><Setting /></el-icon>
      <template #title>{{ $t('nav.profile') }}</template>
    </el-menu-item>
  </el-menu>
</template>
