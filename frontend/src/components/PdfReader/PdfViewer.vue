<script setup lang="ts">
import { ref } from 'vue'
import VuePdfApp from 'vue3-pdf-app'
import 'vue3-pdf-app/dist/icons/main.css'

const props = defineProps<{
  url: string
}>()

const emit = defineEmits<{
  (e: 'text-select', data: { text: string, x: number, y: number, rawEvent: MouseEvent }): void
  (e: 'clear-select'): void
}>()

// PDF App 配置
// 可以在这里禁用不需要的功能，例如 secondaryToolbar
const config = ref({
  sidebar: {
    viewThumbnail: true,
    viewOutline: true,
    viewAttachments: true,
  },
  secondaryToolbar: {
    secondaryPresentationMode: true,
    secondaryOpenFile: false,
    secondaryPrint: true,
    secondaryDownload: true,
    secondaryViewBookmark: false,
    firstPage: true,
    lastPage: true,
    pageRotateCw: true,
    pageRotateCcw: true,
    cursorSelectTool: true,
    cursorHandTool: true,
    scrollVertical: true,
    scrollHorizontal: true,
    scrollWrapped: true,
    spreadNone: true,
    spreadOdd: true,
    spreadEven: true,
    documentProperties: true,
  },
  toolbar: {
    // 保持默认工具栏配置
    toolbarViewerLeft: {
      findbar: true,
      previous: true,
      next: true,
      pageNumber: true,
    },
    toolbarViewerRight: {
      presentationMode: true,
      openFile: false,
      print: true,
      download: true,
      viewBookmark: false,
    },
    toolbarViewerMiddle: {
      zoomOut: true,
      zoomIn: true,
      scaleSelectContainer: true,
    },
  },
})

function handlePagesRendered() {
  // 页面渲染完成后的钩子
  console.log('PDF pages rendered')
}

// 划词选择逻辑 (保留之前的实现)
let isMouseDown = false
function handleMouseDown() {
  isMouseDown = true
  emit('clear-select')
}

function handleMouseUp(event: MouseEvent) {
  isMouseDown = false

  // 给一小段延迟，确保系统选择生效
  setTimeout(() => {
    const selection = window.getSelection()
    // 确保选择是在组件范围内
    if (!selection || selection.rangeCount === 0)
      return

    const selectedText = selection.toString().trim()

    if (selectedText && selectedText.length > 0) {
      emit('text-select', {
        text: selectedText,
        x: event.clientX,
        y: event.clientY,
        rawEvent: event,
      })
    }
  }, 100)
}
</script>

<template>
  <div class="pdf-viewer-wrapper" @mouseup="handleMouseUp" @mousedown="handleMouseDown">
    <VuePdfApp
      v-if="url"
      :pdf="url"
      theme="light"
      :config="config"
      @pages-rendered="handlePagesRendered"
    />
    <div v-else class="loading-state">
      <el-skeleton :rows="10" animated />
    </div>
  </div>
</template>

<style scoped>
.pdf-viewer-wrapper {
  width: 100%;
  height: 100%;
  position: relative;
  background-color: #525659;
}

.loading-state {
  padding: 40px;
  background: white;
  height: 100%;
}

/* 样式穿透调整 vue3-pdf-app 的部分样式以适应布局 */
:deep(.pdf-app) {
  height: 100% !important;
}

:deep(.toolbar) {
  z-index: 10 !important;
}
</style>
