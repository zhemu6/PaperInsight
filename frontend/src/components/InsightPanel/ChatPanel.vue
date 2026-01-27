<script setup lang="ts">
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { useI18n } from 'vue-i18n'
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { clearSession, startChatStream, stopChat } from '~/api/chat-sse'
import { useChatStore } from '~/stores/chat'
import { useUserStore } from '~/stores/user'
import MarkdownViewer from '~/components/InsightPanel/MarkdownViewer.vue'

const { t } = useI18n()

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
 * 停止生成
 */
async function handleStop() {
  if (currentEventSource) {
    currentEventSource.close()
    currentEventSource = null
  }
  
  // 更新 UI 状态
  isLoading.value = false
  const lastMsg = messages.value[messages.value.length - 1]
  if (lastMsg && lastMsg.status === 'streaming') {
    lastMsg.status = 'done'
    lastMsg.content += ' [已停止]'
  }

  // 调用后端接口
  try {
     if (currentChatId.value) {
       await stopChat(currentChatId.value)
     }
  } catch (e) {
    console.error('Stop chat failed', e)
  }
}

/**
 * 发送消息
 */
async function handleSend() {
  // 1. 检查登录状态
  if (!userStore.loginUser || !userStore.loginUser.id) {
    ElMessage.warning(t('error.unauthorized'))
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
  isLoading.value = true // 开始加载

  try {
    const userId = String(userStore.loginUser.id)
    currentEventSource = startChatStream(
      currentChatId.value,
      question,
      userId,
      (data, type) => {
        // 增量更新内容 (支持工具调用)
        chatStore.handleStreamingUpdate(data, type)
      },
      () => {
        // 完成
        assistantMsg.status = 'done'
        currentEventSource = null
        isLoading.value = false // 结束加载
      },
      (error) => {
        // 错误
        console.error('SSE Error', error)
        assistantMsg.status = 'error'
        assistantMsg.content += '\n\n(连接中断，请重试)'
        currentEventSource = null
        isLoading.value = false // 结束加载
      },
    )
  }
  catch (e) {
    ElMessage.error('无法连接到聊天服务')
    messages.value.pop() // 移除失败的消息
    isLoading.value = false
  }
}

/**
 * 清除会话
 */
async function handleClear() {
  try {
    // 如果正在生成，先停止
    if (isLoading.value) {
      await handleStop()
    }
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
    if (!isLoading.value) {
       handleSend()
    }
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

          <!-- 顺序渲染内容项 -->
          <div v-if="msg.items && msg.items.length > 0">
             <div v-for="(item, idx) in msg.items" :key="idx">
                
                <!-- 思考过程 -->
                <div v-if="item.type === 'thinking'" class="mb-2">
                  <div class="rounded-lg bg-purple-50/50 dark:bg-purple-900/10 border border-purple-200 dark:border-purple-800 overflow-hidden">
                    <details class="group">
                      <summary class="flex items-center gap-2 px-3 py-2 text-xs font-medium text-purple-600 dark:text-purple-400 cursor-pointer hover:bg-purple-100/50 dark:hover:bg-purple-900/20 select-none transition-colors">
                        <div class="i-ri-brain-line text-sm" />
                        <span>{{ $t('chat.thinking') }}</span>
                        <div class="i-ri-arrow-down-s-line ml-auto transform transition-transform group-open:rotate-180 opacity-50" />
                      </summary>
                      <div class="p-3 text-sm text-gray-600 dark:text-gray-300 border-t border-purple-200 dark:border-purple-800">
                        <MarkdownViewer :content="item.thinking" />
                      </div>
                    </details>
                  </div>
                </div>

                <!-- 工具调用 -->
                <div v-if="item.type === 'tool_use'" class="mb-2">
                   <div class="rounded-lg border border-blue-100 dark:border-blue-900 bg-blue-50/30 dark:bg-blue-900/10 overflow-hidden">
                      <div class="flex items-center gap-2 px-3 py-2 text-xs font-medium text-blue-600 dark:text-blue-400">
                        <div class="i-ri-tools-line text-sm" />
                        <span>{{ $t('chat.toolCall') }}: {{ item.name }} </span>
                      </div>
                      <div class="px-3 pb-2 text-xs">
                        <pre class="bg-blue-50 dark:bg-gray-900/50 p-2 rounded text-gray-600 dark:text-gray-400 overflow-x-auto border border-blue-100 dark:border-blue-900/30">{{ $t('chat.toolParams') }}：{{ item.input }}</pre>
                      </div>
                   </div>
                </div>

                <!-- 工具结果 -->
                <div v-if="item.type === 'tool_result'" class="mb-2">
                   <div class="rounded-lg border border-green-100 dark:border-green-900 bg-green-50/30 dark:bg-green-900/10 overflow-hidden">
                      <div class="flex items-center gap-2 px-3 py-2 text-xs font-medium text-green-600 dark:text-green-400">
                        <div class="i-ri-check-line text-sm" />
                        <span>{{ $t('chat.toolResult') }}: {{ item.name }}</span>
                      </div>
                      <div class="px-3 pb-2 text-xs">
                         <div class="bg-green-50 dark:bg-gray-900/50 p-2 rounded text-gray-600 dark:text-gray-400 overflow-x-auto border border-green-100 dark:border-green-900/30 max-h-40 overflow-y-auto">
                            {{ item.output }}
                         </div>
                      </div>
                   </div>
                </div>

                <!-- 文本回复 -->
                <div v-if="item.type === 'text'" class="text-sm">
                   <MarkdownViewer :content="item.text" />
                </div>

             </div>
          </div>

          <!-- 兼容旧数据的显示逻辑 (如果没有items) -->
          <div v-else>
             <!-- 思考过程 (旧) -->
             <div v-if="msg.thinking" class="mb-3">
              <div class="rounded-lg bg-purple-50/50 dark:bg-purple-900/10 border border-purple-200 dark:border-purple-800 overflow-hidden">
                <details class="group">
                  <summary class="flex items-center gap-2 px-3 py-2 text-xs font-medium text-purple-600 dark:text-purple-400 cursor-pointer hover:bg-purple-100/50 dark:hover:bg-purple-900/20 select-none transition-colors">
                    <div class="i-ri-brain-line text-sm" />
                    <span>{{ $t('chat.thinking') }}</span>
                    <div class="i-ri-arrow-down-s-line ml-auto transform transition-transform group-open:rotate-180 opacity-50" />
                  </summary>
                  <div class="p-3 text-sm text-gray-600 dark:text-gray-300 border-t border-purple-200 dark:border-purple-800">
                    <MarkdownViewer :content="msg.thinking" />
                  </div>
                </details>
              </div>
            </div>
          
            <!-- 正文内容 (旧) -->
            <div class="text-sm">
              <MarkdownViewer :content="msg.content" />
            </div>

            <!-- 工具调用展示 (旧逻辑不需要保留了，因为新数据会走上面的items，旧数据没有tools字段) -->
          </div>

            <!-- Loading 动画 -->
            <div v-if="!msg.content && msg.status === 'streaming' && !msg.thinking" class="flex gap-1 py-1">
              <span class="w-2 h-2 rounded-full bg-gray-400 animate-bounce" />
              <span class="w-2 h-2 rounded-full bg-gray-400 animate-bounce delay-100" />
              <span class="w-2 h-2 rounded-full bg-gray-400 animate-bounce delay-200" />
            </div>
            

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
          :placeholder="$t('chat.inputPlaceholder')"
          :disabled="isLoading"
          @keydown.enter="onEnter"
        />
        
        <div class="absolute bottom-3 right-3 flex gap-2">
          <button
            v-if="messages.length > 1"
            class="p-1.5 text-gray-400 hover:text-red-500 transition-colors"
            :title="$t('chat.clear')"
            :disabled="isLoading" 
            @click="handleClear"
          >
            <div class="i-ri-delete-bin-line text-lg" />
          </button>
          
          <!-- Stop Button -->
          <button
            v-if="isLoading"
            class="p-1.5 rounded-lg transition-colors bg-red-500 text-white hover:bg-red-600 shadow-md"
            :title="$t('common.loading')"
            @click="handleStop"
          >
             <div class="i-ri-stop-circle-fill text-lg animate-pulse" />
          </button>

          <!-- Send Button -->
          <button
            v-else
            class="p-1.5 rounded-lg transition-colors"
            :class="[
              inputMessage.trim()
                ? 'bg-blue-600 text-white hover:bg-blue-700 shadow-md'
                : 'bg-gray-200 text-gray-400 cursor-not-allowed dark:bg-gray-700',
            ]"
            :disabled="!inputMessage.trim()"
            @click="handleSend"
          >
            <div class="i-ri-send-plane-fill text-lg" />
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
