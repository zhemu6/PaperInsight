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
 * @param onError 错误回调
 * @returns EventSource 实例 (用于关闭连接)
 */
export function startChatStream(
  chatId: string,
  userQuery: string,
  userId: string,
  onMessage: (text: string) => void,
  onDone: () => void,
  onError: (error: any) => void,
): EventSource {
  // 更新为新的 API 路径: /assistant/chat
  // 参数: chatId, userQuery, userId
  const url = `/api/assistant/chat?chatId=${chatId}&userQuery=${encodeURIComponent(userQuery)}&userId=${userId}`
  const eventSource = new EventSource(url)

  eventSource.onmessage = (event) => {
    // 官方 SupervisorAgentController 直接发送内容，没有 [DONE] 标记，而是通过流关闭来结束
    // 但为了保险，我们还是检查一下数据
    if (event.data) {
       onMessage(event.data)
    }
  }

  eventSource.addEventListener('done', () => {
    eventSource.close()
    onDone()
  })

  eventSource.onerror = (error) => {
    console.error('SSE Error:', error)
    eventSource.close()
    onError(error)
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
