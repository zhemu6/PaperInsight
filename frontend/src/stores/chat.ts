import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getChatHistory } from '~/api/chat-sse'

// 定义内容项类型
export type ChatItem
  = | { type: 'text', text: string }
    | { type: 'thinking', thinking: string }
    | { type: 'tool_use', name: string, input: string, output?: string }
    | { type: 'tool_result', name: string, output: string }

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
   * 处理流式更新 (支持工具调用)
   */
  function handleStreamingUpdate(data: any, type: string) {
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

    if (type === 'text') {
      const lastItem = lastMsg.items[lastMsg.items.length - 1]
      if (lastItem && lastItem.type === 'text') {
        lastItem.text += data
      }
      else {
        lastMsg.items.push({ type: 'text', text: data })
      }
      lastMsg.content += data
    }
    else if (type === 'thinking') {
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
    else if (type === 'tool_use') {
      try {
        const toolData = typeof data === 'string' ? JSON.parse(data) : data
        let existingIndex = -1
        for (let i = lastMsg.items.length - 1; i >= 0; i--) {
          const item = lastMsg.items[i]
          if (item.type === 'tool_use' && item.name === toolData.name) {
            existingIndex = i
            break
          }
        }
        if (existingIndex !== -1) {
          lastMsg.items[existingIndex] = {
            type: 'tool_use',
            name: toolData.name,
            input: toolData.input,
          }
        }
        else {
          lastMsg.items.push({
            type: 'tool_use',
            name: toolData.name,
            input: toolData.input,
          })
        }
      }
      catch (e) {
        console.error('Failed to parse tool_use data', e)
      }
    }
    else if (type === 'tool_result') {
      try {
        const toolData = typeof data === 'string' ? JSON.parse(data) : data
        let existingIndex = -1
        for (let i = lastMsg.items.length - 1; i >= 0; i--) {
          const item = lastMsg.items[i]
          if (item.type === 'tool_result' && item.name === toolData.name) {
            existingIndex = i
            break
          }
        }
        if (existingIndex !== -1) {
          lastMsg.items[existingIndex] = {
            type: 'tool_result',
            name: toolData.name,
            output: toolData.output,
          }
        }
        else {
          lastMsg.items.push({
            type: 'tool_result',
            name: toolData.name,
            output: toolData.output,
          })
        }
      }
      catch (e) {
        console.error('Failed to parse tool_result data', e)
      }
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
          const rawContent = item.content
          const items: ChatItem[] = []
          let textContent = ''

          if (Array.isArray(rawContent)) {
            rawContent.forEach((c: any) => {
              if (c.type === 'text') {
                items.push({ type: 'text', text: c.text })
                textContent += c.text
              }
              else if (c.type === 'thinking') {
                items.push({ type: 'thinking', thinking: c.thinking })
              }
              else if (c.type === 'tool_use') {
                items.push({ type: 'tool_use', name: c.name, input: c.input })
              }
              else if (c.type === 'tool_result') {
                items.push({ type: 'tool_result', name: c.name, output: c.output })
              }
            })
          }
          else if (typeof rawContent === 'string') {
            textContent = rawContent
            items.push({ type: 'text', text: rawContent })
          }

          const role = (item.role === 'model' || item.role === 'tool') ? 'assistant' : item.role

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
        setWelcomeMessage()
      }
    }
    catch (error) {
      setWelcomeMessage()
    }
    finally {
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
    handleStreamingUpdate,
    loadHistory,
    clearMessages,
  }
})
