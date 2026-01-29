<script setup lang="ts">
import { ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps<{
  visible: boolean
  x: number
  y: number
  text: string
}>()

const emit = defineEmits<{
  (e: 'close'): void
  (e: 'action', action: string, text: string): void
}>()

function handleAction(action: string) {
  emit('action', action, props.text)
  emit('close')
}

defineExpose({
  // 保留旧方法占位以防报错，逻辑已清空
  setLoading: () => {},
  setResult: () => {},
  appendResult: () => {},
})
</script>

<template>
  <div
    v-if="visible"
    class="ai-floating-menu"
    :style="{ top: `${y}px`, left: `${x}px` }"
    @mousedown.stop
  >
    <div class="menu-actions">
      <el-button size="small" type="primary" :icon="ChatDotRound" @click="handleAction('chat')">
        {{ $t('paperDetail.tabs.chat') }}
      </el-button>
    </div>
  </div>
</template>

<style scoped>
.ai-floating-menu {
  position: fixed;
  z-index: 3000;
  background: white;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  border: 1px solid #ebeef5;
  padding: 4px;
  transform: translate(-50%, -100%);
  margin-top: -10px;
}

.menu-actions {
  display: flex;
  gap: 4px;
}
</style>
