import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getChatHistory } from '~/api/chat-sse'
import type { ChatEvent } from '~/api/chat-sse'

// 定义内容项类型
export type ChatItem
  = | { type: 'text', text: string }
    | { type: 'thinking', thinking: string }
    | { type: 'tool_use', id?: string, name: string, input: string }
    | { type: 'tool_result', id?: string, name: string, output: string }

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  items?: ChatItem[]
  status: 'pending' | 'streaming' | 'done' | 'error'
  timestamp: number
  thinking?: string
  reference?: string // 划词引用上下文
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
      timestamp: Date.now(),
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
   * 处理流式事件 (统一 ChatEvent JSON)
   */
  function handleStreamingEvent(event: ChatEvent) {
    const lastMsg = messages.value[messages.value.length - 1]
    if (!lastMsg || lastMsg.status !== 'streaming')
      return

    if (!lastMsg.items) {
      lastMsg.items = []
      if (lastMsg.content) {
        lastMsg.items.push({ type: 'text', text: lastMsg.content })
      }
      if (lastMsg.thinking) {
        lastMsg.items.push({ type: 'thinking', thinking: lastMsg.thinking })
      }
    }

    if (event.type === 'TEXT') {
      const data = event.content || ''
      const lastItem = lastMsg.items[lastMsg.items.length - 1]
      if (lastItem && lastItem.type === 'text') {
        lastItem.text += data
      }
      else {
        lastMsg.items.push({ type: 'text', text: data })
      }
      lastMsg.content += data
    }
    else if (event.type === 'THINKING') {
      const data = event.content || ''
      const lastItem = lastMsg.items[lastMsg.items.length - 1]
      if (lastItem && lastItem.type === 'thinking') {
        lastItem.thinking += data
      }
      else {
        lastMsg.items.push({ type: 'thinking', thinking: data })
      }
      if (!lastMsg.thinking)
        lastMsg.thinking = ''
      lastMsg.thinking += data
    }
    else if (event.type === 'TOOL_USE') {
      const id = event.toolId
      const name = event.toolName || 'tool'
      const input = event.toolInput ? JSON.stringify(event.toolInput, null, 2) : ''

      const existingIndex = lastMsg.items.findIndex(item => item.type === 'tool_use' && item.id && id && item.id === id)
      if (existingIndex !== -1) {
        lastMsg.items[existingIndex] = { type: 'tool_use', id, name, input }
      }
      else {
        lastMsg.items.push({ type: 'tool_use', id, name, input })
      }
    }
    else if (event.type === 'TOOL_RESULT') {
      const id = event.toolId
      const name = event.toolName || 'tool'
      const output = event.toolResult || ''

      const existingIndex = lastMsg.items.findIndex(item => item.type === 'tool_result' && item.id && id && item.id === id)
      if (existingIndex !== -1) {
        lastMsg.items[existingIndex] = { type: 'tool_result', id, name, output }
      }
      else {
        lastMsg.items.push({ type: 'tool_result', id, name, output })
      }
    }
    else if (event.type === 'ERROR') {
      lastMsg.status = 'error'
      lastMsg.content += `\n\n(${event.error || '发生错误'})`
    }
  }

  async function loadHistory(chatId: string) {
    currentChatId.value = chatId
    try {
      isLoading.value = true
      const history = await getChatHistory(chatId)
      if (history && history.length > 0) {
        const mergedMessages: ChatMessage[] = []
        history.forEach((item: any) => {
          const items: ChatItem[] = []
          let textContent = ''

          const events = Array.isArray(item.events) ? item.events : []
          events.forEach((e: any) => {
            if (e.type === 'TEXT') {
              const t = e.content || ''
              items.push({ type: 'text', text: t })
              textContent += t
            }
            else if (e.type === 'THINKING') {
              const t = e.content || ''
              items.push({ type: 'thinking', thinking: t })
            }
            else if (e.type === 'TOOL_USE') {
              items.push({
                type: 'tool_use',
                id: e.toolId,
                name: e.toolName || 'tool',
                input: e.toolInput ? JSON.stringify(e.toolInput, null, 2) : '',
              })
            }
            else if (e.type === 'TOOL_RESULT') {
              items.push({
                type: 'tool_result',
                id: e.toolId,
                name: e.toolName || 'tool',
                output: e.toolResult || '',
              })
            }
          })

          const role = item.role === 'user' ? 'user' : 'assistant'

          // 解析引用内容
          let reference: string | undefined
          if (role === 'user' && textContent) {
            const trimmed = textContent.trim()
            if (trimmed.startsWith('【引用内容】')) {
              const splitKey = '【我的问题】'
              const splitIdx = trimmed.indexOf(splitKey)
              if (splitIdx !== -1) {
                reference = trimmed.substring(6, splitIdx).trim()
                textContent = trimmed.substring(splitIdx + splitKey.length).trim()
              }
            }
          }

          if (role === 'assistant' && mergedMessages.length > 0 && mergedMessages[mergedMessages.length - 1].role === 'assistant') {
            const lastMsg = mergedMessages[mergedMessages.length - 1]
            if (!lastMsg.items)
              lastMsg.items = []
            items.forEach((newItem) => {
              if (newItem.type === 'text') {
                let lastTextItem = null
                for (let i = lastMsg.items!.length - 1; i >= 0; i--) {
                  if (lastMsg.items![i].type === 'text') {
                    lastTextItem = lastMsg.items![i] as { type: 'text', text: string }
                    break
                  }
                }
                if (lastTextItem && lastTextItem.text.trim() === newItem.text.trim())
                  return
              }
              lastMsg.items!.push(newItem)
            })
            if (textContent && !lastMsg.content.endsWith(textContent)) {
              lastMsg.content += textContent
            }
          }
          else {
            mergedMessages.push({
              id: item.id || Date.now().toString(),
              role: role as 'user' | 'assistant',
              content: textContent,
              items,
              status: 'done',
              timestamp: Date.now(),
              reference,
            })
          }
        })
        messages.value = mergedMessages
      }
      else {
        messages.value = []
      }
    }
    catch (error) {
      messages.value = []
      throw error
    }
    finally {
      isLoading.value = false
    }
  }

  function clearMessages() {
    messages.value = []
  }

  return {
    messages,
    isLoading,
    currentChatId,
    addMessage,
    addStreamingMessage,
    updateStreamingContent,
    handleStreamingEvent,
    loadHistory,
    clearMessages,
  }
})
