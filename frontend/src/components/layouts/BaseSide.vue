<script lang="ts" setup>
import {
  Bell,
  Document,
  Location,
  Search,
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
      <template #title>
        {{ $t('nav.paperSquare') }}
      </template>
    </el-menu-item>

    <el-menu-item index="/file">
      <el-icon><Document /></el-icon>
      <template #title>
        {{ $t('nav.myPapers') }}
      </template>
    </el-menu-item>

    <el-menu-item index="/tools/journal">
      <el-icon><Search /></el-icon>
      <template #title>
        {{ $t('nav.journalQuery') }}
      </template>
    </el-menu-item>

    <el-menu-item index="/notifications">
      <el-icon><Bell /></el-icon>
      <template #title>
        {{ $t('nav.notifications') }}
      </template>
    </el-menu-item>

    <el-menu-item index="/user/profile">
      <el-icon><Setting /></el-icon>
      <template #title>
        {{ $t('nav.profile') }}
      </template>
    </el-menu-item>
  </el-menu>
</template>
