/**
 * Chat SSE Client
 * 手动实现的流式对话客户端，用于补充 OpenAPI 生成器不支持 SSE 的问题
 */

import { clearSession as apiClearSession } from './chatController'

export interface ChatMessage {
  role: 'user' | 'assistant'
  content: string
  status: 'pending' | 'streaming' | 'done' | 'error'
  id: string
}

/**
 * 启动流式对话
 * @param chatId 对话ID (Session ID)
 * @param userQuery 用户问题
 * @param userId 用户ID
 * @param onMessage 收到消息回调 (增量文本)
 * @param onDone 完成回调
 * @param _onError 错误回调
 * @returns EventSource 实例 (用于关闭连接)
 */
export function startChatStream(
  chatId: string,
  userQuery: string,
  userId: string,
  onMessage: (data: any, type: string) => void,
  onDone: () => void,
  _onError: (error: any) => void,
): EventSource {
  // 更新为新的 API 路径: /assistant/chat
  // 参数: chatId, userQuery, userId
  const url = `/api/assistant/chat?chatId=${chatId}&userQuery=${encodeURIComponent(userQuery)}&userId=${userId}`
  const eventSource = new EventSource(url)

  // 监听默认的消息事件 (Text)
  eventSource.onmessage = (event) => {
    if (event.data) {
      onMessage(event.data, 'text')
    }
  }

  // 监听思考过程事件 (Thinking)
  eventSource.addEventListener('thinking', (event) => {
    if (event.data) {
      onMessage(event.data, 'thinking')
    }
  })

  // 监听工具调用事件
  eventSource.addEventListener('tool_use', (event) => {
    if (event.data) {
      onMessage(event.data, 'tool_use')
    }
  })

  // 监听工具结果事件
  eventSource.addEventListener('tool_result', (event) => {
    if (event.data) {
      onMessage(event.data, 'tool_result')
    }
  })

  // 仍然保留 done 事件监听
  eventSource.addEventListener('done', () => {
    eventSource.close()
    onDone()
  })

  // 处理连接关闭/错误
  // 由于后端直接关闭流会导致浏览器触发 error，这里我们将其视为一次会话结束
  eventSource.onerror = (_error) => {
    // console.error('SSE Stream closed or error')
    eventSource.close()
    onDone()
  }

  return eventSource
}

/**
 * 获取历史消息
 */
export async function getChatHistory(chatId: string) {
  // 使用 fetch 直接请求，避免 request 封装带来的类型问题（除非确认后端返回 Result）
  const response = await fetch(`/api/assistant/history?chatId=${chatId}`)
  if (!response.ok) {
    throw new Error('Failed to fetch history')
  }
  return await response.json()
}

/**
 * 清除会话上下文 (使用生成的 API)
 */
export function clearSession(paperId: number | string) {
  return apiClearSession({ paperId: Number(paperId) })
}
/**
 * 停止生成
 */
export function stopChat(chatId: string) {
  return fetch(`/api/assistant/stop?chatId=${chatId}`, {
    method: 'POST',
  })
}
