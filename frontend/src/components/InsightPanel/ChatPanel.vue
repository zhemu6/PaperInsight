<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { clearSession, startChatStream } from '~/api/chat-sse'
import { useUserStore } from '~/stores/user'
import { useChatStore } from '~/stores/chat'
import MarkdownViewer from '~/components/InsightPanel/MarkdownViewer.vue'

const props = defineProps<{
  paperId: string
}>()

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const { messages, isLoading, currentChatId } = storeToRefs(chatStore)

const inputMessage = ref('')
const chatContainer = ref<HTMLElement | null>(null)
let currentEventSource: EventSource | null = null

// 自动滚动到底部
function scrollToBottom() {
  nextTick(() => {
    if (chatContainer.value) {
      chatContainer.value.scrollTop = chatContainer.value.scrollHeight
    }
  })
}

// 监听消息变化自动滚动
watch(() => messages.value.length, scrollToBottom)
watch(() => messages.value[messages.value.length - 1]?.content, scrollToBottom)

// 加载历史记录
async function initChat() {
  if (!userStore.loginUser?.id) return
  
  const userId = String(userStore.loginUser.id)
  const chatId = `chat_${props.paperId}_${userId}`
  await chatStore.loadHistory(chatId)
}

// 监听 paperId 变化重新加载
watch(() => props.paperId, initChat, { immediate: true })

/**
 * 发送消息
 */
async function handleSend() {
  // 1. 检查登录状态
  if (!userStore.loginUser || !userStore.loginUser.id) {
    ElMessage.warning('请先登录')
    router.push('/user/login')
    return
  }

  const question = inputMessage.value.trim()
  if (!question || isLoading.value)
    return

  // 2. 添加用户消息到 store
  chatStore.addMessage({
    role: 'user',
    content: question,
  })

  inputMessage.value = ''
  
  // 3. 添加助手流式消息占位
  const assistantMsg = chatStore.addStreamingMessage()

  try {
    const userId = String(userStore.loginUser.id)
    currentEventSource = startChatStream(
      currentChatId.value,
      question,
      userId,
      (text) => {
        // 增量更新内容
        assistantMsg.content += text
      },
      () => {
        // 完成
        assistantMsg.status = 'done'
        currentEventSource = null
      },
      (error) => {
        // 错误
        console.error('SSE Error', error)
        assistantMsg.status = 'error'
        assistantMsg.content += '\n\n(连接中断，请重试)'
        currentEventSource = null
      },
    )
  }
  catch (e) {
    ElMessage.error('无法连接到聊天服务')
    messages.value.pop() // 移除失败的消息
  }
}

/**
 * 清除会话
 */
async function handleClear() {
  try {
    await clearSession(props.paperId)
    chatStore.clearMessages()
    ElMessage.success('会话已重置')
  }
  catch (e) {
    ElMessage.error('重置会话失败')
  }
}

// 组件卸载时关闭连接
onUnmounted(() => {
  if (currentEventSource) {
    currentEventSource.close()
  }
})

// 监听 Enter 键发送
function onEnter(e: KeyboardEvent) {
  if (e.ctrlKey || e.metaKey) {
    inputMessage.value += '\n'
  }
  else {
    e.preventDefault()
    handleSend()
  }
}
</script>

<template>
  <div class="flex flex-col h-full bg-white dark:bg-gray-800">
    <!-- Messages Area -->
    <div ref="chatContainer" class="flex-1 overflow-y-auto p-4 space-y-6 custom-scrollbar">
      <div v-if="isLoading && messages.length === 0" class="flex justify-center items-center h-full">
        <div class="i-ri-loader-4-line animate-spin text-2xl text-gray-400" />
      </div>
      
      <div
        v-for="msg in messages"
        :key="msg.id"
        class="flex gap-3"
        :class="msg.role === 'user' ? 'flex-row-reverse' : ''"
      >
        <!-- Avatar -->
        <div
          class="flex-shrink-0 w-8 h-8 rounded-full flex items-center justify-center text-sm"
          :class="msg.role === 'user' ? 'bg-blue-100 text-blue-600' : 'bg-purple-100 text-purple-600'"
        >
          <div v-if="msg.role === 'user'" class="i-ri-user-smile-line" />
          <div v-else class="i-ri-robot-2-line" />
        </div>

        <!-- Content -->
        <div
          class="max-w-[85%] rounded-lg p-3 text-sm leading-relaxed shadow-sm transition-all"
          :class="[
            msg.role === 'user'
              ? 'bg-blue-600 text-white'
              : 'bg-gray-50 dark:bg-gray-700 text-gray-800 dark:text-gray-100 border border-gray-100 dark:border-gray-600',
            msg.status === 'streaming' ? 'streaming-cursor' : '',
          ]"
        >
          <div v-if="msg.role === 'user'">
            {{ msg.content }}
          </div>
          <div v-else>
            <div v-if="!msg.content && msg.status === 'streaming'" class="flex gap-1 py-1">
              <span class="w-2 h-2 rounded-full bg-gray-400 animate-bounce" />
              <span class="w-2 h-2 rounded-full bg-gray-400 animate-bounce delay-100" />
              <span class="w-2 h-2 rounded-full bg-gray-400 animate-bounce delay-200" />
            </div>
            <MarkdownViewer
              v-else
              :content="msg.content"
              class="prose-sm max-w-none"
              :class="msg.role === 'user' ? 'prose-invert' : ''"
            />
          </div>
        </div>
      </div>
    </div>

    <!-- Input Area -->
    <div class="border-t border-gray-100 dark:border-gray-700 p-4 bg-white dark:bg-gray-800">
      <div class="relative">
        <textarea
          v-model="inputMessage"
          rows="3"
          class="w-full resize-none rounded-xl border border-gray-200 bg-gray-50 p-3 pr-12 text-sm focus:border-blue-500 focus:bg-white focus:outline-none dark:border-gray-600 dark:bg-gray-900 dark:text-gray-100 dark:focus:border-blue-400"
          placeholder="输入问题，Enter 发送..."
          :disabled="isLoading"
          @keydown.enter="onEnter"
        />
        
        <div class="absolute bottom-3 right-3 flex gap-2">
          <button
            v-if="messages.length > 1"
            class="p-1.5 text-gray-400 hover:text-red-500 transition-colors"
            title="清空会话"
            @click="handleClear"
          >
            <div class="i-ri-delete-bin-line text-lg" />
          </button>
          
          <button
            class="p-1.5 rounded-lg transition-colors"
            :class="[
              inputMessage.trim() && !isLoading
                ? 'bg-blue-600 text-white hover:bg-blue-700 shadow-md'
                : 'bg-gray-200 text-gray-400 cursor-not-allowed dark:bg-gray-700',
            ]"
            :disabled="!inputMessage.trim() || isLoading"
            @click="handleSend"
          >
            <div v-if="isLoading" class="i-ri-loader-4-line animate-spin text-lg" />
            <div v-else class="i-ri-send-plane-fill text-lg" />
          </button>
        </div>
      </div>
      <div class="mt-2 text-center text-xs text-gray-400">
        Generated by AgentScope • RAG Enhanced
      </div>
    </div>
  </div>
</template>

<style scoped>
.custom-scrollbar::-webkit-scrollbar {
  width: 6px;
}
.custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #ddd;
  border-radius: 3px;
}
.dark .custom-scrollbar::-webkit-scrollbar-thumb {
  background-color: #444;
}

/* 光标闪烁动画 */
.streaming-cursor :deep(p:last-child)::after {
  content: '▋';
  display: inline-block;
  animation: blink 1s infinite;
  color: #3b82f6;
  margin-left: 2px;
  vertical-align: baseline;
}

@keyframes blink {
  0%, 100% { opacity: 1; }
  50% { opacity: 0; }
}
</style>
