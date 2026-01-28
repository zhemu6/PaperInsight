import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getChatHistory } from '~/api/chat-sse'

// 定义内容项类型
export type ChatItem = 
  | { type: 'text'; text: string }
  | { type: 'thinking'; thinking: string }
  | { type: 'tool_use'; name: string; input: string; output?: string }
  | { type: 'tool_result'; name: string; output: string }

export interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string // 用于兼容纯文本显示或用户消息
  items?: ChatItem[] // 用于助手的富文本流式显示
  status: 'pending' | 'streaming' | 'done' | 'error'
  timestamp: number
  // 下面这些旧字段作为 fallback 或兼容以前的逻辑
  thinking?: string 
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
   * 处理流式更新 (支持工具调用)
   */
  /**
   * 处理流式更新 (支持工具调用)
   */
  function handleStreamingUpdate(data: any, type: string) {
    const lastMsg = messages.value[messages.value.length - 1]
    if (!lastMsg || lastMsg.status !== 'streaming') return

    // 初始化 items 数组
    if (!lastMsg.items) {
      lastMsg.items = []
      // 如果已有文本内容，先作为第一项
      if (lastMsg.content) {
        lastMsg.items.push({ type: 'text', text: lastMsg.content })
      }
      // 如果已有思考内容，也添加
      if (lastMsg.thinking) {
        lastMsg.items.push({ type: 'thinking', thinking: lastMsg.thinking })
      }
    }

    if (type === 'text') {
      // 查找最后一个文本块进行追加
      const lastItem = lastMsg.items[lastMsg.items.length - 1]
      if (lastItem && lastItem.type === 'text') {
        lastItem.text += data
      } else {
        lastMsg.items.push({ type: 'text', text: data })
      }
      // 同步更新 content (兼容性)
      lastMsg.content += data
    } else if (type === 'thinking') {
      const lastItem = lastMsg.items[lastMsg.items.length - 1]
      if (lastItem && lastItem.type === 'thinking') {
        lastItem.thinking += data
      } else {
        lastMsg.items.push({ type: 'thinking', thinking: data })
      }
      // 同步更新 thinking (兼容性)
      if (!lastMsg.thinking) lastMsg.thinking = ''
      lastMsg.thinking += data
    } else if (type === 'tool_use') {
      // 工具调用是一个完整的 JSON 对象
      try {
        const toolData = typeof data === 'string' ? JSON.parse(data) : data
        // 去重：检查最后几项是否有相同的 tool_use
        let existingIndex = -1
        // 倒序查找最近的 tool_use
        for (let i = lastMsg.items.length - 1; i >= 0; i--) {
          const item = lastMsg.items[i]
          if (item.type === 'tool_use' && item.name === toolData.name) {
            existingIndex = i
            break
          }
        }

        if (existingIndex !== -1) {
          // 更新现有项
          lastMsg.items[existingIndex] = {
            type: 'tool_use',
            name: toolData.name,
            input: toolData.input,
          }
        } else {
          lastMsg.items.push({
            type: 'tool_use',
            name: toolData.name,
            input: toolData.input,
          })
        }
      } catch (e) {
        console.error('Failed to parse tool_use data', e)
      }
    } else if (type === 'tool_result') {
      // 工具结果是一个完整的 JSON 对象
      try {
        const toolData = typeof data === 'string' ? JSON.parse(data) : data

        // 去重：检查是否已经存在该结果
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
        } else {
          lastMsg.items.push({
            type: 'tool_result',
            name: toolData.name,
            output: toolData.output,
          })
        }
      } catch (e) {
        console.error('Failed to parse tool_result data', e)
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
      // console.log('Raw history from backend:', history)
      if (history && history.length > 0) {
        const mergedMessages: ChatMessage[] = []
        
        history.forEach((item: any, index: number) => {
          const rawContent = item.content
          const items: ChatItem[] = []
          let textContent = ''

          // 解析后端返回的结构化内容
          if (Array.isArray(rawContent)) {
             rawContent.forEach((c: any) => {
               if (c.type === 'text') {
                 items.push({ type: 'text', text: c.text })
                 textContent += c.text
               } else if (c.type === 'thinking') {
                 items.push({ type: 'thinking', thinking: c.thinking })
               } else if (c.type === 'tool_use') {
                 // 查找后续是否紧跟 tool_result (在同一个消息里)
                 // 但这里我们需要保持顺序，所以按顺序添加即可
                 // 后端返回的是 type: 'tool_use', name, input
                 items.push({ type: 'tool_use', name: c.name, input: c.input })
               } else if (c.type === 'tool_result') {
                 // 结果单独作为一个块
                 items.push({ type: 'tool_result', name: c.name, output: c.output })
               }
             })
          } else if (typeof rawContent === 'string') {
             // 兼容旧数据
             textContent = rawContent
             items.push({ type: 'text', text: rawContent })
          }

          // 将 model 和 tool 都视为 assistant，以便在UI上合并成一个连续的对话流
          const role = (item.role === 'model' || item.role === 'tool') ? 'assistant' : item.role
          
          // console.log(`Processing msg ${index} [${role}]:`, items)

          // 如果当前是 assistant 且上一条也是 assistant，则合并
          if (role === 'assistant' && mergedMessages.length > 0 && mergedMessages[mergedMessages.length - 1].role === 'assistant') {
            const lastMsg = mergedMessages[mergedMessages.length - 1]
            // console.log('Merging into lastMsg:', lastMsg.id)
            
            // 合并 items 列表，保持由上到下的时序
            if (!lastMsg.items) {
               lastMsg.items = []
            }
            // console.log('processed items:', items)
            // 去重逻辑：如果新的一批 item 里的第一个是 text，且和 lastMsg 最后一个 item (也是 text) 内容一样，则跳过
            // 这可以防止 history 接口有时候返回重复内容块导致的显示问题
            items.forEach(newItem => {
               if (newItem.type === 'text') {
                  // 向前查找最后一个 text 类型的 item
                  let lastTextItem = null
                  for (let i = lastMsg.items!.length - 1; i >= 0; i--) {
                    if (lastMsg.items![i].type === 'text') {
                      lastTextItem = lastMsg.items![i] as { type: 'text', text: string }
                      break
                    }
                  }
                  
                  if (lastTextItem && lastTextItem.text.trim() === newItem.text.trim()) {
                     return // 跳过重复文本
                  }
               }
               lastMsg.items!.push(newItem)
            })

            // 合并正文内容 (fallback)
            if (textContent) {
              if (!lastMsg.content.endsWith(textContent)) {
                 lastMsg.content += textContent
              }
            }
          } else {
            // 否则作为新消息添加
            const newMessage: ChatMessage = {
              id: item.id || Date.now().toString(),
              role: role as 'user' | 'assistant',
              content: textContent,
              items: items,
              status: 'done',
              timestamp: Date.now()
            }
            mergedMessages.push(newMessage)
          }
        })
        messages.value = mergedMessages
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
    handleStreamingUpdate,
    loadHistory,
    clearMessages
  }
})
