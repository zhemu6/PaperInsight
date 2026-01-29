<script setup lang="ts">
import VuePdfApp from 'vue3-pdf-app'
import 'vue3-pdf-app/dist/icons/main.css'

const props = defineProps<{
  url: string
}>()

// 自定义样式可以根据需要覆盖
</script>

<template>
  <div class="h-full w-full">
    <VuePdfApp
      :pdf="url"
      theme="light"
      :config="{
        sidebar: false,
        toolbar: {
          toolbarViewerLeft: {
            findbar: true,
            previous: true,
            next: true,
            pageNumber: true,
          },
          toolbarViewerMiddle: {
            zoomOut: true,
            zoomIn: true,
            scaleSelect: true,
          },
          toolbarViewerRight: {
            presentationMode: true,
            openFile: false,
            print: false,
            download: true,
            viewBookmark: false,
          },
        },
      }"
    />
  </div>
</template>

<style>
/* 针对 vue3-pdf-app 的样式微调 (如果需要) */
</style>
