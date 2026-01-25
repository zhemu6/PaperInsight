<script lang="ts" setup>
import {
  Document,
  Menu as IconMenu,
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
      <template #title>论文广场</template>
    </el-menu-item>

    <el-menu-item index="/file">
      <el-icon><Document /></el-icon>
      <template #title>我的论文</template>
    </el-menu-item>

    <el-menu-item index="/user/profile">
      <el-icon><Setting /></el-icon>
      <template #title>个人中心</template>
    </el-menu-item>
  </el-menu>
</template>
