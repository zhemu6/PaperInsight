<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { storeToRefs } from 'pinia'
import { nextTick, onUnmounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { clearSession, startChatStream, stopChat } from '~/api/chat-sse'
import MarkdownViewer from '~/components/InsightPanel/MarkdownViewer.vue'
import { useChatStore } from '~/stores/chat'
import { useUserStore } from '~/stores/user'

const props = defineProps<{
  paperId: string
  context?: string
}>()

const emit = defineEmits<{
  (e: 'update:context', val: string): void
}>()

const { t } = useI18n()

const router = useRouter()
const userStore = useUserStore()
const chatStore = useChatStore()
const { messages, isLoading, currentChatId } = storeToRefs(chatStore)

const inputMessage = ref('')
const chatContainer = ref<HTMLElement | null>(null)
let currentEventSource: EventSource | null = null

// 监听外部 context 变化，自动填充（可选，或者只显示预览）
watch(() => props.context, (val) => {
  if (val && !inputMessage.value) {
    // 如果输入框为空且有新引用，可以考虑自动聚焦或提示
  }
})

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
  if (!userStore.loginUser?.id)
    return

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
  }
  catch (e) {
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

  // 如果有引用内容，拼接 Prompt
  let fullQuestion = question
  if (props.context) {
    fullQuestion = `【引用内容】\n${props.context}\n\n【我的问题】\n${question}`
  }

  // 2. 添加用户消息到 store (UI 显示简洁版)
  chatStore.addMessage({
    role: 'user',
    content: question,
    reference: props.context, // 可以在 Store 中扩展支持显示引用
  })

  inputMessage.value = ''
  // 发送后清除引用
  emit('update:context', '')

  // 3. 添加助手流式消息占位
  const assistantMsg = chatStore.addStreamingMessage()
  isLoading.value = true // 开始加载

  try {
    const userId = String(userStore.loginUser.id)
    currentEventSource = startChatStream(
      currentChatId.value,
      fullQuestion,
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
    console.error(e)
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
    console.error(e)
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
  <div class="h-full flex flex-col bg-white dark:bg-gray-800">
    <!-- Messages Area -->
    <div ref="chatContainer" class="custom-scrollbar flex-1 overflow-y-auto p-4 space-y-6">
      <div v-if="isLoading && messages.length === 0" class="h-full flex items-center justify-center">
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
          class="h-8 w-8 flex flex-shrink-0 items-center justify-center rounded-full text-sm"
          :class="msg.role === 'user' ? 'bg-blue-100 text-blue-600' : 'bg-purple-100 text-purple-600'"
        >
          <div v-if="msg.role === 'user'" class="i-ri-user-smile-line" />
          <div v-else class="i-ri-robot-2-line" />
        </div>

        <!-- Content -->
        <div
          class="max-w-[80%] overflow-hidden rounded-2xl px-4 py-2 text-sm shadow-sm"
          :class="[
            msg.role === 'user'
              ? 'bg-blue-600 text-white rounded-tr-none whitespace-pre-wrap'
              : 'bg-white dark:bg-gray-900 border border-gray-100 dark:border-gray-800 text-gray-800 dark:text-gray-200 rounded-tl-none',
            msg.status === 'streaming' ? 'streaming-cursor' : '',
          ]"
        >
          <div v-if="msg.role === 'user'">
            <!-- Show reference if exists -->
            <div v-if="msg.reference" class="mb-2 border-l-4 border-white/50 rounded bg-blue-700/30 p-2 text-xs italic opacity-90">
              "{{ msg.reference }}"
            </div>
            {{ msg.content }}
          </div>
          <div v-else>
            <!-- 顺序渲染内容项 -->
            <div v-if="msg.items && msg.items.length > 0">
              <div v-for="(item, idx) in msg.items" :key="idx">
                <!-- 思考过程 -->
                <div v-if="item.type === 'thinking'" class="mb-2">
                  <div class="overflow-hidden border border-purple-200 rounded-lg bg-purple-50/50 dark:border-purple-800 dark:bg-purple-900/10">
                    <details class="group">
                      <summary class="flex cursor-pointer select-none items-center gap-2 px-3 py-2 text-xs text-purple-600 font-medium transition-colors hover:bg-purple-100/50 dark:text-purple-400 dark:hover:bg-purple-900/20">
                        <div class="i-ri-brain-line text-sm" />
                        <span>{{ $t('chat.thinking') }}</span>
                        <div class="i-ri-arrow-down-s-line ml-auto transform opacity-50 transition-transform group-open:rotate-180" />
                      </summary>
                      <div class="border-t border-purple-200 p-3 text-sm text-gray-600 dark:border-purple-800 dark:text-gray-300">
                        <MarkdownViewer :content="item.thinking" />
                      </div>
                    </details>
                  </div>
                </div>

                <!-- 工具调用 -->
                <div v-if="item.type === 'tool_use'" class="mb-2">
                  <div class="overflow-hidden border border-blue-100 rounded-lg bg-blue-50/30 dark:border-blue-900 dark:bg-blue-900/10">
                    <div class="flex items-center gap-2 px-3 py-2 text-xs text-blue-600 font-medium dark:text-blue-400">
                      <div class="i-ri-tools-line text-sm" />
                      <span>{{ $t('chat.toolCall') }}: {{ item.name }} </span>
                    </div>
                    <div class="px-3 pb-2 text-xs">
                      <pre class="overflow-x-auto border border-blue-100 rounded bg-blue-50 p-2 text-gray-600 dark:border-blue-900/30 dark:bg-gray-900/50 dark:text-gray-400">{{ $t('chat.toolParams') }}：{{ item.input }}</pre>
                    </div>
                  </div>
                </div>

                <!-- 工具结果 -->
                <div v-if="item.type === 'tool_result'" class="mb-2">
                  <div class="overflow-hidden border border-green-100 rounded-lg bg-green-50/30 dark:border-green-900 dark:bg-green-900/10">
                    <div class="flex items-center gap-2 px-3 py-2 text-xs text-green-600 font-medium dark:text-green-400">
                      <div class="i-ri-check-line text-sm" />
                      <span>{{ $t('chat.toolResult') }}: {{ item.name }}</span>
                    </div>
                    <div class="px-3 pb-2 text-xs">
                      <div class="max-h-40 overflow-x-auto overflow-y-auto border border-green-100 rounded bg-green-50 p-2 text-gray-600 dark:border-green-900/30 dark:bg-gray-900/50 dark:text-gray-400">
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

            <!-- Loading 动画 -->
            <div v-if="!msg.content && msg.status === 'streaming' && !msg.thinking" class="flex gap-1 py-1">
              <span class="h-2 w-2 animate-bounce rounded-full bg-gray-400" />
              <span class="h-2 w-2 animate-bounce rounded-full bg-gray-400 delay-100" />
              <span class="h-2 w-2 animate-bounce rounded-full bg-gray-400 delay-200" />
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- Input Area -->
    <div class="border-t border-gray-100 bg-white p-4 dark:border-gray-700 dark:bg-gray-800">
      <!-- Context Reference Preview -->
      <div v-if="context" class="group relative mb-2 border border-blue-100 rounded-lg bg-blue-50 p-2 dark:border-blue-800 dark:bg-blue-900/20">
        <div class="mb-1 flex items-center gap-2">
          <div class="i-ri-quote-line text-blue-500" />
          <span class="text-xs text-blue-600 font-medium dark:text-blue-400">{{ $t('common.reference') }}</span>
          <button
            class="ml-auto p-0.5 text-gray-400 opacity-0 transition-opacity hover:text-red-500 group-hover:opacity-100"
            @click="emit('update:context', '')"
          >
            <div class="i-ri-close-line" />
          </button>
        </div>
        <div class="line-clamp-2 text-xs text-gray-600 italic dark:text-gray-300">
          "{{ context }}"
        </div>
      </div>

      <div class="relative">
        <textarea
          v-model="inputMessage"
          rows="3"
          class="w-full resize-none border border-gray-200 rounded-xl bg-gray-50 p-3 pr-12 text-sm dark:border-gray-600 focus:border-blue-500 dark:bg-gray-900 focus:bg-white dark:text-gray-100 focus:outline-none dark:focus:border-blue-400"
          :placeholder="$t('chat.inputPlaceholder')"
          :disabled="isLoading"
          @keydown.enter="onEnter"
        />

        <div class="absolute bottom-3 right-3 flex gap-2">
          <button
            v-if="messages.length > 1"
            class="p-1.5 text-gray-400 transition-colors hover:text-red-500"
            :title="$t('chat.clear')"
            :disabled="isLoading"
            @click="handleClear"
          >
            <div class="i-ri-delete-bin-line text-lg" />
          </button>

          <!-- Stop Button -->
          <button
            v-if="isLoading"
            class="rounded-lg bg-red-500 p-1.5 text-white shadow-md transition-colors hover:bg-red-600"
            :title="$t('common.loading')"
            @click="handleStop"
          >
            <div class="i-ri-stop-circle-fill animate-pulse text-lg" />
          </button>

          <!-- Send Button -->
          <button
            v-else
            class="rounded-lg p-1.5 transition-colors"
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
  0%,
  100% {
    opacity: 1;
  }
  50% {
    opacity: 0;
  }
}
</style>
