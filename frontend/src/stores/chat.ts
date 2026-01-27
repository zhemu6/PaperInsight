import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getChatHistory } from '~/api/chat-sse'

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  status: 'pending' | 'streaming' | 'done' | 'error'
  timestamp: number
}

export const useChatStore = defineStore('chat', () => {
  const messages = ref<ChatMessage[]>([])
  const isLoading = ref(false)
  const currentChatId = ref('')

  /**
   * 添加消息
   */
  function addMessage(message: Omit<ChatMessage, 'id' | 'timestamp' | 'status'>) {
    const newMessage: ChatMessage = {
      ...message,
      id: Date.now().toString() + Math.random().toString(36).substring(2, 9),
      timestamp: Date.now(),
      status: 'done',
    }
    messages.value.push(newMessage)
    return newMessage
  }

  /**
   * 添加流式占位消息
   */
  function addStreamingMessage() {
    const id = `ai-${Date.now()}`
    const newMessage: ChatMessage = {
      id,
      role: 'assistant',
      content: '',
      status: 'streaming',
      timestamp: Date.now()
    }
    messages.value.push(newMessage)
    return messages.value[messages.value.length - 1]
  }

  /**
   * 更新最后一条流式消息内容
   */
  function updateStreamingContent(content: string, isDone = false) {
    const lastMsg = messages.value[messages.value.length - 1]
    if (lastMsg && lastMsg.status === 'streaming') {
      lastMsg.content = content
      if (isDone) {
        lastMsg.status = 'done'
      }
    }
  }

  /**
   * 加载历史记录
   */
  async function loadHistory(chatId: string) {
    currentChatId.value = chatId
    try {
      isLoading.value = true
      const history = await getChatHistory(chatId)
      if (history && history.length > 0) {
        messages.value = history.map((item: any) => ({
          id: item.id || Math.random().toString(36),
          role: item.role === 'model' ? 'assistant' : item.role,
          content: item.content,
          status: 'done',
          timestamp: Date.now() // 后端暂未返回时间戳，取当前
        }))
      } else {
        setWelcomeMessage()
      }
    } catch (error) {
      console.error('Failed to load history:', error)
      setWelcomeMessage()
    } finally {
      isLoading.value = false
    }
  }

  function setWelcomeMessage() {
    messages.value = [{
      id: 'welcome',
      role: 'assistant',
      content: '你好！我是你的论文助手。关于这篇论文，你想了解什么？',
      status: 'done',
      timestamp: Date.now(),
    }]
  }

  function clearMessages() {
    messages.value = []
    setWelcomeMessage()
  }

  return {
    messages,
    isLoading,
    currentChatId,
    addMessage,
    addStreamingMessage,
    updateStreamingContent,
    loadHistory,
    clearMessages
  }
})
